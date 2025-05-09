package dst.ass3.messaging;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

public interface IWorkloadMonitor extends Closeable {

    /**
     * Returns for each region the amount of waiting requests.
     *
     * @return a map
     */
    Map<Region, Long> getRequestCount();

    /**
     * Returns the amount of workers for each region. This can be deduced from the amount of consumers to each
     * queue.
     *
     * @return a map
     */
    Map<Region, Long> getWorkerCount();

    /**
     * Returns for each region the average processing time of the last 10 recorded requests. The data comes from
     * subscriptions to the respective topics.
     *
     * @return a map
     */
    Map<Region, Double> getAverageProcessingTime();

    /**
     * Subscribes to the message queues for all regions to monitor and process worker responses.
     * This method dynamically declares and binds temporary queues to a topic exchange for each region.
     * It sets up consumers to retrieve and process messages corresponding to the regions.
     * The messages include processing times which are maintained in a region-specific, limited-size list.
     * <p>
     * The subscription involves:
     * - Declaring a temporary queue for each region.
     * - Binding the queue to the topic exchange with a routing key specific to the region.
     * - Creating a consumer that processes the worker's response messages and stores their processing times.
     * <p>
     * If an error occurs during the subscription setup process for any region, it logs the error but
     * proceeds with other regions.
     */
    void subscribe();

    @Override
    void close() throws IOException;
}
