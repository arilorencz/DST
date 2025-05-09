package dst.ass3.worker.tests;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import dst.ass3.grading.GitHubClassroomGrading;
import dst.ass3.grading.LocalGradingClassRule;
import dst.ass3.grading.LocalGradingRule;
import dst.ass3.messaging.RabbitResource;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

public class WorkerStartupLogTest {
    private static final String DEFAULT_HOST = "tcp://127.0.0.1:2375";
    DefaultDockerClientConfig config = DefaultDockerClientConfig
            .createDefaultConfigBuilder()
            .withDockerHost(DEFAULT_HOST)
            .build();
    @Rule
    public RabbitResource rabbit = new RabbitResource();

    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();

    private static final String IMAGE_NAME = "dst/ass3-worker:latest";

    @Test(timeout = 20000)
    @GitHubClassroomGrading(maxScore = 5)
    public void testRegionLog() throws Exception {
        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

        String region = "at_linz";
        assertTrue(test_region_log(dockerClient, region));

        region = "at_vienna";
        assertTrue(test_region_log(dockerClient, region));
    }

    private boolean test_region_log(DockerClient dockerClient, String region) throws InterruptedException {
        try {
            initRabbitForRegion(region);
            CreateContainerResponse container = dockerClient.createContainerCmd(IMAGE_NAME)
                    .withHostConfig(HostConfig.newHostConfig().withNetworkMode("dst"))
                    .withCmd(region) // pass region argument if needed
                    .withTty(true)
                    .exec();

            String containerId = container.getId();
            dockerClient.startContainerCmd(containerId).exec();

            // 2. Capture logs and look for the startup message
            AtomicBoolean foundStartupMessage = new AtomicBoolean(false);
            ResultCallback.Adapter<Frame> logCallback = new ResultCallback.Adapter<>() {
                @Override
                public void onNext(Frame frame) {
                    String logLine = new String(frame.getPayload());
                    System.out.println("Log line:");
                    System.out.print(logLine); // For debugging
                    if (logLine.contains(region)) {
                        foundStartupMessage.set(true);
                    }
                }
            };

            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(logCallback);

            // 3. Wait a few seconds for the worker to start and print the message
            Thread.sleep(2000);

            // 4. Stop and clean up the container
            try {
                dockerClient.stopContainerCmd(containerId).exec();
            } catch (NotModifiedException | NotFoundException ignored) {
                //container already shut down
            }
            logCallback.awaitCompletion(5, TimeUnit.SECONDS);
            dockerClient.removeContainerCmd(containerId).exec();

            return foundStartupMessage.get();
        } finally {
            cleanupRabbitForRegion(region);
        }

    }

    void initRabbitForRegion(String region) {
        Exchange exchange = new TopicExchange("dst.workers");
        rabbit.getAdmin().declareExchange(exchange);

        Queue toWorkerQueue = new Queue("dst." + region);
        rabbit.getAdmin().declareQueue(toWorkerQueue);

        Queue fromWorkerQueue = new Queue("requests." + region);
        rabbit.getAdmin().declareQueue(fromWorkerQueue);
    }

    void cleanupRabbitForRegion(String region) {
        rabbit.getAdmin().deleteQueue("dst." + region);
        rabbit.getAdmin().deleteQueue("requests." + region);
        rabbit.getAdmin().deleteExchange("dst.workers");
    }
}
