package dst.ass3.event.impl;

import dst.ass3.event.IEventProcessingEnvironment;
import dst.ass3.event.IEventSourceFunction;
import dst.ass3.event.model.domain.ITripEventInfo;
import dst.ass3.event.model.domain.Region;
import dst.ass3.event.model.domain.TripState;
import dst.ass3.event.model.events.*;
import org.apache.flink.api.common.eventtime.Watermark;
import org.apache.flink.api.common.eventtime.WatermarkGenerator;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.PatternTimeoutFunction;
import org.apache.flink.cep.functions.PatternProcessFunction;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.GlobalWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.triggers.CountTrigger;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.System.out;

public class EventProcessingEnvironment implements IEventProcessingEnvironment {
    private SinkFunction<LifecycleEvent> lifecycleSink;
    private SinkFunction<MatchingDuration> matchingDurationSink;
    private SinkFunction<AverageMatchingDuration> averageMatchingSink;
    private SinkFunction<MatchingTimeoutWarning> timeoutWarningSink;
    private SinkFunction<TripFailedWarning> failedWarningSink;
    private SinkFunction<Alert> alertSink;
    private Time matchingTimeout;

    @Override
    public void initialize(StreamExecutionEnvironment env) {
        EventSourceFunction source = new EventSourceFunction();

        WatermarkStrategy<LifecycleEvent> watermarkStrategy = WatermarkStrategy
                .<LifecycleEvent>forBoundedOutOfOrderness(Duration.ofSeconds(1))
                .withTimestampAssigner((event, timestamp) -> event.getTimestamp());

        DataStream<LifecycleEvent> stream = env
                .addSource(source)
                // Filter all trips without a region
                .filter(info -> info.getRegion() != null)
                .map(LifecycleEvent::new)
                .assignTimestampsAndWatermarks(watermarkStrategy);


        stream.addSink(lifecycleSink);

        //pattern from created -> matched
        Pattern<LifecycleEvent, ?> pattern = Pattern.<LifecycleEvent>begin("created")
                .where(new SimpleCondition<LifecycleEvent>() {
                    @Override
                    public boolean filter(LifecycleEvent event) {
                        return event.getState() == TripState.CREATED;
                    }
                })
                .followedByAny("queued")
                .where(new SimpleCondition<LifecycleEvent>() {
                    @Override
                    public boolean filter(LifecycleEvent event) {
                        return event.getState() == TripState.QUEUED;
                    }
                })
                .oneOrMore()
                .optional()
                .next("matched")
                .where(new SimpleCondition<LifecycleEvent>() {
                    @Override
                    public boolean filter(LifecycleEvent event) {
                        return event.getState() == TripState.MATCHED;
                    }
                })
                .within(matchingTimeout)
                ;


        PatternStream<LifecycleEvent> patternStream = CEP.pattern(
                stream.keyBy(LifecycleEvent::getTripId),
                pattern
        );

        // MATCHED timestamp result
        patternStream.process(new PatternProcessFunction<LifecycleEvent, MatchingDuration>() {
            private transient Set<Long> emittedTripIds;

            @Override
            public void open(Configuration parameters) {
                emittedTripIds = new HashSet<>();
            }
            @Override
            public void processMatch(Map<String, List<LifecycleEvent>> pattern, Context context, Collector<MatchingDuration> out) throws Exception {
                LifecycleEvent created = pattern.get("created").get(0);
                LifecycleEvent matched = pattern.get("matched").get(0);
                long tripId = created.getTripId();

                if (!emittedTripIds.contains(tripId)) {
                    out.collect(new MatchingDuration(
                            tripId,
                            created.getRegion(),
                            matched.getTimestamp() - created.getTimestamp()
                    ));
                    emittedTripIds.add(tripId);
                }
            }
        }).addSink(matchingDurationSink);

        //handle timeouts
        OutputTag<MatchingTimeoutWarning> timeoutTag = new OutputTag<MatchingTimeoutWarning>("timeout-warning") {};

        SingleOutputStreamOperator<MatchingDuration> resultStream = patternStream.select(
                timeoutTag,
                new PatternTimeoutFunction<LifecycleEvent, MatchingTimeoutWarning>() {
                    @Override
                    public MatchingTimeoutWarning timeout(Map<String, List<LifecycleEvent>> pattern, long timeoutTimestamp) throws Exception {
                        LifecycleEvent created = pattern.get("created").get(0);
                        System.out.println(">>> Timeout triggered for trip " + pattern.get("created").get(0).getTripId());
                        return new MatchingTimeoutWarning(created.getTripId(), created.getRegion());
                    }
                },
                new PatternSelectFunction<LifecycleEvent, MatchingDuration>() {
                    @Override
                    public MatchingDuration select(Map<String, List<LifecycleEvent>> pattern) throws Exception {
                        LifecycleEvent created = pattern.get("created").get(0);
                        LifecycleEvent matched = pattern.get("matched").get(0);
                        return new MatchingDuration(
                                created.getTripId(),
                                created.getRegion(),
                                matched.getTimestamp() - created.getTimestamp()
                        );
                    }
                }
        );

        DataStream<MatchingTimeoutWarning> timeoutWarnings = resultStream.getSideOutput(timeoutTag);
        timeoutWarnings.addSink(timeoutWarningSink);

        //anomalous trip requests
        Pattern<LifecycleEvent, ?> anomalousPattern = Pattern.<LifecycleEvent>begin("created")
                .where(new SimpleCondition<LifecycleEvent>() {
                    @Override
                    public boolean filter(LifecycleEvent event) {
                        return event.getState() == TripState.CREATED;
                    }
                })
                .followedBy("mq1")
                .where(new SimpleCondition<LifecycleEvent>() {
                    @Override
                    public boolean filter(LifecycleEvent event) {
                        return event.getState() == TripState.MATCHED;
                    }
                })
                .followedBy("qq1")
                .where(new SimpleCondition<LifecycleEvent>() {
                    @Override
                    public boolean filter(LifecycleEvent event) {
                        return event.getState() == TripState.QUEUED;
                    }
                })
                .followedBy("mq2")
                .where(new SimpleCondition<LifecycleEvent>() {
                    @Override
                    public boolean filter(LifecycleEvent event) {
                        return event.getState() == TripState.MATCHED;
                    }
                })
                .followedBy("qq2")
                .where(new SimpleCondition<LifecycleEvent>() {
                    @Override
                    public boolean filter(LifecycleEvent event) {
                        return event.getState() == TripState.QUEUED;
                    }
                })
                .followedBy("mq3")
                .where(new SimpleCondition<LifecycleEvent>() {
                    @Override
                    public boolean filter(LifecycleEvent event) {
                        return event.getState() == TripState.MATCHED;
                    }
                })
                .followedBy("qq3")
                .where(new SimpleCondition<LifecycleEvent>() {
                    @Override
                    public boolean filter(LifecycleEvent event) {
                        return event.getState() == TripState.QUEUED;
                    }
                })
                .within(matchingTimeout); // what length of time should I choose here?

        PatternStream<LifecycleEvent> anomalousPatternStream = CEP.pattern(
                stream.keyBy(LifecycleEvent::getTripId),
                anomalousPattern
        );

        DataStream<TripFailedWarning> failedWarningStream = anomalousPatternStream.process(new PatternProcessFunction<LifecycleEvent, TripFailedWarning>() {
            Set<Long> emittedTripIds;

            @Override
            public void open(Configuration parameters) throws Exception {
                emittedTripIds = new HashSet<>();
            }

            @Override
            public void processMatch(Map<String, List<LifecycleEvent>> anomalousPattern, Context ctx, Collector<TripFailedWarning> out) throws Exception {
                LifecycleEvent created = anomalousPattern.get("created").get(0);
                long tripId = created.getTripId();

                if (!emittedTripIds.contains(tripId)) {
                    out.collect(new TripFailedWarning(tripId, created.getRegion()));
                    emittedTripIds.add(tripId);
                }
            }
        });
        failedWarningStream.addSink(failedWarningSink);

        //alerts
        // Merge the two streams that emit Warnings
        DataStream<Warning> warnings = timeoutWarnings
                .map((MapFunction<MatchingTimeoutWarning, Warning>) w -> (Warning) w, TypeInformation.of(Warning.class))
                .union(
                        failedWarningStream.map((MapFunction<TripFailedWarning, Warning>) w -> (Warning) w, TypeInformation.of(Warning.class))
                );

        // Key by region name (as String)
        warnings
                .keyBy(warning -> warning.getRegion().name())
                .window(GlobalWindows.create())
                .trigger(CountTrigger.of(3))
                .process(new ProcessWindowFunction<Warning, Alert, String, GlobalWindow>() {
                    @Override
                    public void process(String regionName, Context context, Iterable<Warning> elements, Collector<Alert> out) {
                        Region region = Region.valueOf(regionName);
                        List<Warning> collectedWarnings = new java.util.ArrayList<>();
                        elements.forEach(collectedWarnings::add);
                        out.collect(new Alert(region, collectedWarnings));
                    }
                })
                .addSink(alertSink);


        try {
            String plan = env.getExecutionPlan();
            java.nio.file.Files.write(
                    java.nio.file.Paths.get("executionPlan.json"),
                    plan.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            e.printStackTrace(); // or use a logger
        }
    }

    @Override
    public void setMatchingDurationTimeout(Time time) {
        this.matchingTimeout = time;
    }

    @Override
    public void setLifecycleEventStreamSink(SinkFunction<LifecycleEvent> sink) {
        this.lifecycleSink = sink;
    }

    @Override
    public void setMatchingDurationStreamSink(SinkFunction<MatchingDuration> sink) {
        this.matchingDurationSink = sink;
    }

    @Override
    public void setAverageMatchingDurationStreamSink(SinkFunction<AverageMatchingDuration> sink) {
        this.averageMatchingSink = sink;
    }

    @Override
    public void setMatchingTimeoutWarningStreamSink(SinkFunction<MatchingTimeoutWarning> sink) {
        this.timeoutWarningSink = sink;
    }

    @Override
    public void setTripFailedWarningStreamSink(SinkFunction<TripFailedWarning> sink) {
        this.failedWarningSink = sink;
    }

    @Override
    public void setAlertStreamSink(SinkFunction<Alert> sink) {
        this.alertSink = sink;
    }
}
