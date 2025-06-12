package dst.ass3.elastic.impl;

import dst.ass3.elastic.ContainerException;
import dst.ass3.elastic.IContainerService;
import dst.ass3.elastic.IElasticityController;
import dst.ass3.messaging.IWorkloadMonitor;
import dst.ass3.messaging.Region;

import java.util.Map;

public class ElasticityController implements IElasticityController {
    private final IWorkloadMonitor workloadMonitor;
    private final IContainerService containerService;

    private static final double SCALE_OUT_THRESHOLD_PERCENT = 0.1;
    private static final double SCALE_DOWN_THRESHOLD_PERCENT = 0.05;

    public ElasticityController(IWorkloadMonitor workloadMonitor, IContainerService containerService) {
        this.workloadMonitor = workloadMonitor;
        this.containerService = containerService;
    }
    @Override
    public void adjustWorkers() throws ContainerException {
        Map<Region, Long> workerCounts = workloadMonitor.getWorkerCount();
        Map<Region, Long> requestCounts = workloadMonitor.getRequestCount();
        Map<Region, Double> processingTimes = workloadMonitor.getAverageProcessingTime();

        for (Region region : Region.values()) {
            long workers = workerCounts.getOrDefault(region, 0L);
            long queued = requestCounts.getOrDefault(region, 0L);
            double avgProcTime = processingTimes.getOrDefault(region, 0.0);

            if (workers == 0 || avgProcTime <= 0) continue;

            double expectedWaitTime = (queued * avgProcTime) / workers;
            double maxWaitTime = getMaxWaitTime(region);

            double diffRatio = (expectedWaitTime - maxWaitTime) / maxWaitTime;

            if (diffRatio > SCALE_OUT_THRESHOLD_PERCENT) {
                containerService.startWorker(region);
            } else if (diffRatio < -SCALE_DOWN_THRESHOLD_PERCENT && workers > 1) {
                // Find a container in this region and stop it
                containerService.listContainers().stream()
                        .filter(c -> region.equals(c.getWorkerRegion()))
                        .findFirst()
                        .ifPresent(c -> {
                            try {
                                containerService.stopContainer(c.getContainerId());
                            } catch (ContainerException e) {
                                System.err.println("Failed to stop container: " + e.getMessage());
                            }
                        });
            }
        }
    }

    private int getMaxWaitTime(Region region) {
        if (region.equals(Region.AT_LINZ) || region.equals(Region.AT_VIENNA)) return 30;
        return 120; //DE_BERLIN
    }
}
