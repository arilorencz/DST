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
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.PatternTimeoutFunction;
import org.apache.flink.cep.functions.PatternProcessFunction;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.util.Collector;

import java.time.Duration;
import java.util.List;
import java.util.Map;

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
        var source = new EventSourceFunction();

        var watermarkStrategy = WatermarkStrategy
                .<LifecycleEvent>forBoundedOutOfOrderness(Duration.ofSeconds(20))
                .withTimestampAssigner((event, timestamp) -> event.getTimestamp());

        var stream = env
                .addSource(source)
                // Filter all trips without a region
                .filter(info -> info.getRegion() != null)
                .map(LifecycleEvent::new)
                .assignTimestampsAndWatermarks(watermarkStrategy);


        stream.addSink(lifecycleSink);



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
