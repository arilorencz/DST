package dst.ass3.elastic.tests;

import dst.ass3.elastic.impl.WorkerAdjustment;
import dst.ass3.grading.GitHubClassroomGrading;
import dst.ass3.grading.LocalGradingClassRule;
import dst.ass3.grading.LocalGradingRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static dst.ass3.elastic.impl.WorkerAdjustment.calculateWorkerAdjustment;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WorkerAdjustmentTest {

    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();

    @Test(timeout = 60000)
    @GitHubClassroomGrading(maxScore = 5)
    public void testCalculateWorkerAdjustment_basic() {
        // Given
        long requestCount = 10;
        double avgProcessingTime = 2.0;
        long maxTime = 5;
        double scaleOut = 0.1;
        double scaleDown = 0.2;

        // When
        WorkerAdjustment adj = calculateWorkerAdjustment(
            requestCount, avgProcessingTime, maxTime, scaleOut, scaleDown);

        assertNotNull(adj);

        // Then
        assertEquals(4.0, adj.targetWorkers, 0.0001); // 2*10/5 = 4.0
        assertEquals(3.6, adj.thresholdUp, 0.0001);   // 4.0 * (1-0.1) = 3.6
        assertEquals(4.8, adj.thresholdDown, 0.0001); // 4.0 * (1+0.2) = 4.8
    }

    @Test(timeout = 60000)
    @GitHubClassroomGrading(maxScore = 5)
    public void testCalculateWorkerAdjustment_zeroRequests() {
        // Given
        long requestCount = 0;
        double avgProcessingTime = 2.0;
        long maxTime = 5;
        double scaleOut = 0.1;
        double scaleDown = 0.2;

        // When
        WorkerAdjustment adj = calculateWorkerAdjustment(
            requestCount, avgProcessingTime, maxTime, scaleOut, scaleDown);

        assertNotNull(adj);

        // Then
        assertEquals(0.0, adj.targetWorkers, 0.0001);
        assertEquals(0.0, adj.thresholdUp, 0.0001);
        assertEquals(0.0, adj.thresholdDown, 0.0001);
    }


}
