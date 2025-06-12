package dst.ass3.elastic.impl;

public class WorkerAdjustment {
    public final double targetWorkers;
    public final double thresholdUp;
    public final double thresholdDown;

    public WorkerAdjustment(double targetWorkers, double thresholdUp, double thresholdDown) {
        this.targetWorkers = targetWorkers;
        this.thresholdUp = thresholdUp;
        this.thresholdDown = thresholdDown;
    }

    /**
     * Calculates the required worker adjustment based on the given workload and thresholds.
     *
     * @param requestCount the total number of requests to process
     * @param averageProcessingTime the average time it takes to process a single request, in milliseconds
     * @param maxTime the maximum allowable time for processing all requests, in milliseconds
     * @param scaleOutThresholdPercent the percentage threshold for scaling out (adding more workers)
     * @param scaleDownThresholdPercent the percentage threshold for scaling down (reducing workers)
     * @return a {@code WorkerAdjustment} object containing the calculated target number of workers,
     *         scale-out threshold, and scale-down threshold
     */
    public static WorkerAdjustment calculateWorkerAdjustment(
        long requestCount,
        double averageProcessingTime,
        long maxTime,
        double scaleOutThresholdPercent,
        double scaleDownThresholdPercent)
    {
        if (requestCount == 0 || averageProcessingTime <= 0 || maxTime <= 0) {
            return new WorkerAdjustment(0, 0, 0);
        }

        double targetWorkers = (requestCount * averageProcessingTime) / maxTime;
        double scaleUpThreshold = targetWorkers * (1 + scaleOutThresholdPercent);
        double scaleDownThreshold = targetWorkers * (1 - scaleDownThresholdPercent);

        return new WorkerAdjustment(targetWorkers, scaleUpThreshold, scaleDownThreshold);
    }

}


