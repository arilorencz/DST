package dst.ass3.event.impl;

import dst.ass3.event.Constants;
import dst.ass3.event.EventPublisher;
import dst.ass3.event.EventSubscriber;
import dst.ass3.event.IEventSourceFunction;
import dst.ass3.event.model.domain.ITripEventInfo;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.flink.streaming.api.watermark.Watermark;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class EventSourceFunction extends RichSourceFunction<ITripEventInfo> implements IEventSourceFunction {
    private EventSubscriber subscriber;
    private EventPublisher eventPublisher;
    private SocketAddress socketAddress;
    private volatile boolean isRunning;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        //using the port defined in Constants
        this.socketAddress = new InetSocketAddress("localhost", Constants.EVENT_PUBLISHER_PORT);
        this.subscriber = EventSubscriber.subscribe(socketAddress);
        eventPublisher = new EventPublisher(Constants.EVENT_PUBLISHER_PORT);
    }

    @Override
    public void close() throws Exception {
        if (subscriber != null) {
            subscriber.close();
        }

        if (eventPublisher != null) {
            eventPublisher.close();
        }
    }
    @Override
    public void run(SourceContext<ITripEventInfo> ctx) throws Exception {
        isRunning = true;
        while (isRunning) {
            ITripEventInfo event = subscriber.receive();
            if (event == null) {
                isRunning = false;
                System.out.println("EventSourceFunction: received null — shutting down");
                break;
            }

            synchronized (ctx.getCheckpointLock()) {
                ctx.collectWithTimestamp(event, event.getTimestamp());
            }
        }

        // 💡 Force all timeouts to fire
        synchronized (ctx.getCheckpointLock()) {
            ctx.emitWatermark(new Watermark(Long.MAX_VALUE));
        }
    }

    @Override
    public void cancel() {
        isRunning = false;
        if (subscriber != null) {
            subscriber.close();
        }
    }
}
