package dst.ass3.elastic.impl;

import dst.ass3.elastic.ContainerException;
import dst.ass3.elastic.ContainerInfo;
import dst.ass3.elastic.IContainerService;
import dst.ass3.elastic.IElasticityController;
import dst.ass3.messaging.IWorkloadMonitor;
import dst.ass3.messaging.Region;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

            long maxWaitTime = getMaxWaitTime(region);

            WorkerAdjustment adjustment = WorkerAdjustment.calculateWorkerAdjustment(
                    queued,
                    avgProcTime,
                    maxWaitTime,
                    SCALE_OUT_THRESHOLD_PERCENT,
                    SCALE_DOWN_THRESHOLD_PERCENT
            );

            //comparing current workers to adjustment thresholds
            if (workers < adjustment.thresholdDown) {
                //scale down
                if (workers > 1) {
                    scaleDown(workers, adjustment.targetWorkers, region);
                }
            } else if (workers > adjustment.thresholdUp) {
                //scale up
                while (workers < adjustment.targetWorkers) {
                    containerService.startWorker(region);
                    workers++;
                }
            }
        }
    }

    private void scaleDown(long workers, double desiredAmount, Region region) throws ContainerException {
        List<ContainerInfo> containerInfos = containerService.listContainers().stream().filter(c -> c.getWorkerRegion().equals(region)).collect(Collectors.toList());
        for (ContainerInfo container : containerInfos) {
            if (container.isRunning() && workers > desiredAmount) {
                containerService.stopContainer(container.getContainerId());
                workers--;
            }
        }
    }

    private int getMaxWaitTime(Region region) {
        if (region.equals(Region.AT_LINZ) || region.equals(Region.AT_VIENNA)) return 30;
        return 120; //DE_BERLIN
    }
}
