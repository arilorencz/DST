package dst.ass3.event.impl;

import dst.ass3.event.IEventProcessingEnvironment;
import dst.ass3.event.IEventSourceFunction;
import dst.ass3.event.model.domain.ITripEventInfo;
import dst.ass3.event.model.domain.Region;
import dst.ass3.event.model.domain.TripState;
import dst.ass3.event.model.events.*;
import org.apache.flink.api.common.eventtime.Watermark;
import org.apache.flink.api.common.eventtime.WatermarkGenerator;
import org.apache.flink.api.common.eventtime.WatermarkOutput;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.PatternTimeoutFunction;
import org.apache.flink.cep.functions.PatternProcessFunction;
import org.apache.flink.cep.functions.TimedOutPartialMatchHandler;
import org.apache.flink.cep.nfa.aftermatch.AfterMatchSkipStrategy;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
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

import java.util.*;


public class EventProcessingEnvironment implements IEventProcessingEnvironment {
    private SinkFunction<LifecycleEvent> lifecycleSink;
    private SinkFunction<MatchingDuration> matchingDurationSink;
    private SinkFunction<AverageMatchingDuration> averageMatchingSink;
    private SinkFunction<MatchingTimeoutWarning> timeoutWarningSink;
    private SinkFunction<TripFailedWarning> failedWarningSink;
    private SinkFunction<Alert> alertSink;
    private Time matchingTimeout;
    private static final OutputTag<MatchingTimeoutWarning> MATCHING_TIMEOUT_TAG = new OutputTag<MatchingTimeoutWarning>("timeout-warning") {};


    @Override
    public void initialize(StreamExecutionEnvironment env) {
        EventSourceFunction source = new EventSourceFunction();

        WatermarkStrategy<LifecycleEvent> watermarkStrategy = WatermarkStrategy
                .<LifecycleEvent>forGenerator(ctx -> new PunctuatedAssigner())
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
                        return event.getState().equals(TripState.CREATED);
                    }
                })
                .followedBy("matched")
                .where(new SimpleCondition<>() {
                    @Override
                    public boolean filter(LifecycleEvent value) throws Exception {
                        return value.getState().equals(TripState.MATCHED);
                    }
                })
                .within(matchingTimeout)
                ;


        PatternStream<LifecycleEvent> patternStream = CEP.pattern(
                stream.keyBy((KeySelector<LifecycleEvent, Long>) LifecycleEvent::getTripId),
                pattern
        );

        // MATCHED timestamp result
        SingleOutputStreamOperator<MatchingDuration> resultStream = patternStream.process(new MatchingDurationFunction());
        resultStream.addSink(matchingDurationSink);

        DataStream<MatchingTimeoutWarning> timeoutWarnings = resultStream.getSideOutput(MATCHING_TIMEOUT_TAG);
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
        DataStream<Warning> warnings = timeoutWarnings
                .map(w -> (Warning) w)
                .union(
                        failedWarningStream.map(w -> (Warning) w)
                );

        KeyedStream<Warning, String> keyedWarnings = warnings
                .keyBy(warning -> warning.getRegion().name());

        AfterMatchSkipStrategy skipStrategy = AfterMatchSkipStrategy.skipPastLastEvent();


        Pattern<Warning, ?> warningPattern = Pattern.<Warning>begin("first", skipStrategy)
                .where(new SimpleCondition<Warning>() {
                    @Override
                    public boolean filter(Warning warning) {
                        return true; // Accept all warnings
                    }
                })
                .next("second")
                .where(new SimpleCondition<Warning>() {
                    @Override
                    public boolean filter(Warning warning) {
                        return true; // Accept all warnings
                    }
                })
                .next("third")
                .where(new SimpleCondition<Warning>() {
                    @Override
                    public boolean filter(Warning warning) {
                        return true; // Accept all warnings
                    }
                });

        PatternStream<Warning> warningPatternStream = CEP.pattern(keyedWarnings, warningPattern);

        warningPatternStream.process(new PatternProcessFunction<Warning, Alert>() {
            @Override
            public void processMatch(Map<String, List<Warning>> pattern, Context ctx, Collector<Alert> out) {
                List<Warning> first = pattern.get("first");
                List<Warning> second = pattern.get("second");
                List<Warning> third = pattern.get("third");

                List<Warning> warnings = new ArrayList<>();
                warnings.addAll(first);
                warnings.addAll(second);
                warnings.addAll(third);

                Region region = warnings.get(0).getRegion(); // All warnings have the same region

                out.collect(new Alert(region, warnings));
            }
        }).addSink(alertSink);

        // Key by region name (as String)
        /*
        warnings
                .keyBy(warning -> warning.getRegion().name())
                .window(GlobalWindows.create())
                .trigger(PurgingTrigger.of(CountTrigger.of(3)))
                .process(new ProcessWindowFunction<Warning, Alert, String, GlobalWindow>() {
                    private transient ListState<Warning> warningState;

                    @Override
                    public void open(Configuration parameters) {
                        ListStateDescriptor<Warning> desc = new ListStateDescriptor<>(
                                "warnings", TypeInformation.of(Warning.class));
                        warningState = getRuntimeContext().getListState(desc);
                    }

                    @Override
                    public void process(String key, Context context, Iterable<Warning> elements, Collector<Alert> out) {
                        List<Warning> warnings = new ArrayList<>();

                        Region region = Region.valueOf(key);

                        elements.forEach(element -> {
                            if (!warnings.contains(element)) {
                                System.out.println("************************************");
                                System.out.println("Added warning: " + element);
                                warnings.add(element);
                                System.out.println("************************************");
                            }
                        });

                        // Trigger Alert if there are at least 3 warnings
                        if (warnings.size() >= 3) {
                            System.out.println("----------------------------------------");
                            System.out.println("----------------------------------------");
                            System.out.println("Alert triggered for region: ");
                            System.out.println(region);
                            System.out.println("warnings: length = " + warnings.size());
                            System.out.println(warnings);
                            System.out.println("----------------------------------------");
                            System.out.println("----------------------------------------");
                            out.collect(new Alert(region, warnings));
                            warnings.clear();
                        }
                    }
                })
                .addSink(alertSink);

         */

        //average matching duration
        resultStream
                .keyBy(md -> md.getRegion().name())
                .window(GlobalWindows.create())
                .trigger(CountTrigger.of(5))
                .process(new ProcessWindowFunction<MatchingDuration, AverageMatchingDuration, String, GlobalWindow>() {
                    @Override
                    public void process(String regionName, Context context, Iterable<MatchingDuration> elements, Collector<AverageMatchingDuration> out) {
                        int count = 0;
                        long totalDuration = 0;
                        for (MatchingDuration duration : elements) {
                            totalDuration += duration.getDuration();
                            count++;
                        }

                        if (count == 5) {
                            double avg = (double) totalDuration / count;
                            Region region = Region.valueOf(regionName);
                            out.collect(new AverageMatchingDuration(region, avg));
                        }
                    }
                })
                .addSink(averageMatchingSink);

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

    private static class PunctuatedAssigner implements WatermarkGenerator<LifecycleEvent> {
        @Override
        public void onEvent(LifecycleEvent event, long l, WatermarkOutput output) {
            output.emitWatermark(new Watermark(event.getTimestamp()));
        }

        @Override
        public void onPeriodicEmit(WatermarkOutput watermarkOutput) {

        }
    }

    private static class MatchingDurationFunction extends PatternProcessFunction<LifecycleEvent, MatchingDuration>
            implements TimedOutPartialMatchHandler<LifecycleEvent> {

        @Override
        public void processMatch(Map<String, List<LifecycleEvent>> pattern, Context ctx, Collector<MatchingDuration> out) {
            LifecycleEvent created = pattern.get("created").get(0);
            LifecycleEvent matched = pattern.get("matched").get(0);
            long duration = matched.getTimestamp() - created.getTimestamp();
            out.collect(new MatchingDuration(created.getTripId(), created.getRegion(), duration));
        }

        @Override
        public void processTimedOutMatch(Map<String, List<LifecycleEvent>> pattern, Context ctx) {
            LifecycleEvent created = pattern.get("created").get(0);
            ctx.output(MATCHING_TIMEOUT_TAG, new MatchingTimeoutWarning(created.getTripId(), created.getRegion()));
        }
    }

}
