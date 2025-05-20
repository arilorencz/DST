package dst.ass3.worker.tests;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import dst.ass3.grading.GitHubClassroomGrading;
import dst.ass3.grading.LocalGradingClassRule;
import dst.ass3.grading.LocalGradingRule;
import dst.ass3.messaging.RabbitResource;
import org.junit.*;
import org.junit.rules.Timeout;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WorkerTest {

    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();

    private static final Logger LOG = LoggerFactory.getLogger(WorkerTest.class);

    private static final String WORKER_IMAGE = "dst/ass3-worker:latest";
    private static final String DEFAULT_HOST = "tcp://127.0.0.1:2375";

    @Rule
    public RabbitResource rabbit = new RabbitResource();

    @Rule
    public Timeout timeout = new Timeout(120, TimeUnit.SECONDS);

    private DockerClient client;
    private Jedis jedis;

    @Before
    public void setUp() throws Exception {
        DefaultDockerClientConfig config = DefaultDockerClientConfig
            .createDefaultConfigBuilder()
            .withDockerHost(DEFAULT_HOST)
            .build();
        client = DockerClientBuilder.getInstance(config).build();
        jedis = new Jedis("127.0.0.1", 6379);
    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }

    @Test(timeout = 60000)
    @GitHubClassroomGrading(maxScore = 10)
    public void t00_dockerBuild_imageAvailable() throws Exception {
        File dockerfile = new File("./");
        String imageId = client.buildImageCmd(dockerfile)
            .withTags(new HashSet<>(List.of(WORKER_IMAGE)))
            .exec(new BuildImageResultCallback()).awaitImageId();
        InspectImageResponse image = client.inspectImageCmd(imageId).exec();
        assertThat(image, notNullValue());
        assertThat(image.getRepoTags().size(), is(1));
        assertThat(image.getRepoTags().get(0), is(WORKER_IMAGE));
    }

    @Test(timeout = 60000)
    @GitHubClassroomGrading(maxScore = 8)
    public void t01_startViennaWorker_findDriver_andRemoveFromRedis() {
        String requestId = "testViennaId";
        String regex = ".*(\"|\\s)(" + requestId + ")(\"|\\s).*(\"|\\s)([345][0-9]{3})(\"|\\s|}).*";
        testNonEmptyRedisForRegion("at_vienna", requestId, regex);
    }

    @Test(timeout = 60000)
    @GitHubClassroomGrading(maxScore = 8)
    public void t02_startViennaWorker_onEmptyRedis() {
        String requestId = "testViennaId";
        String regex = ".*(\"|\\s)(" + requestId + ")(\"|\\s).*(\"|\\s)(0)(\"|\\s|}).*";
        testEmptyRedisForRegion("at_vienna", requestId, regex);
    }

    @Test(timeout = 60000)
    @GitHubClassroomGrading(maxScore = 8)
    public void t03_startLinzWorker_findDriver_andRemoveFromRedis() {
        String requestId = "testLinzId";
        String regex = ".*(\"|\\s)(" + requestId + ")(\"|\\s).*(\"|\\s)(1[0-9]{3}|2[0-9]{3})(\"|\\s|}).*";
        testNonEmptyRedisForRegion("at_linz", requestId, regex);
    }

    @Test(timeout = 60000)
    @GitHubClassroomGrading(maxScore = 8)
    public void t04_startLinzWorker_onEmptyRedis() {
        String requestId = "testLinzId";
        String regex = ".*(\"|\\s)(" + requestId + ")(\"|\\s).*(\"|\\s)(0)(\"|\\s|}).*";
        testEmptyRedisForRegion("at_linz", requestId, regex);
    }

    @Test(timeout = 60000)
    @GitHubClassroomGrading(maxScore = 8)
    public void t05_startBerlinWorker_findDriver_andRemoveFromRedis() {
        String requestId = "testBerlinId";
        String regex = ".*(\"|\\s)(" + requestId + ")(\"|\\s).*(\"|\\s)(8[0-9]{3}|9[0-8][0-9]{2}|99[0-8][0-9]|999[0-9]|10[0-9]{3}|11[0-9]{3})(\"|\\s|}).*";
        testNonEmptyRedisForRegion("de_berlin", requestId, regex);
    }

    @Test(timeout = 60000)
    @GitHubClassroomGrading(maxScore = 10)
    public void t06_startBerlinWorker_onEmptyRedis() {
        String requestId = "testBerlinId";
        String regex = ".*(\"|\\s)(" + requestId + ")(\"|\\s).*(\"|\\s)(0)(\"|\\s|}).*";
        testEmptyRedisForRegion("de_berlin", requestId, regex);
    }


    private void testEmptyRedisForRegion(String region, String requestId, String regex) {
        String latitude = "48.198122";
        String longitude = "16.371334";
        jedis.del("drivers:" + region);

        String driverRegex = getEmptyDriverRegex();
        sendRequestAndGetDriver(region, requestId, longitude, latitude, regex, driverRegex);
    }

    private void testNonEmptyRedisForRegion(String region, String requestId, String regex) {
        String latitude = "48.198122";
        String longitude = "16.371334";
        String driverId = "9999999";
        jedis.del("drivers:" + region);
        insertDrivers("drivers:" + region);

        String driverRegex = getDriverRegex(driverId);
        sendRequestAndGetDriver(region, requestId, longitude, latitude, regex, driverRegex);
        Map<String, String> updatedDrivers = jedis.hgetAll("drivers:" + region);
        assertThat(updatedDrivers.containsKey(driverId), is(false));
    }

    private void insertDrivers(String key) {
        Map<String, String> drivers = new HashMap<>();
        drivers.put("9999999", "48.19819 16.37127");
        drivers.put("5678", "50.19819 20.37127");

        jedis.hmset(key, drivers);
    }

    private String getDriverRegex(String driverId) {
        return ".*driverId.*:.*" + driverId + ".*";
    }

    private String getEmptyDriverRegex() {
        return ".*driverId.*:.*\"\".*";
    }


    private void sendRequestAndGetDriver(String region, String id, String longitude, String latitude, String regex, String driverRegex) {
        String containerId = null;
        try {
            Exchange exchange = new TopicExchange("dst.workers");
            rabbit.getAdmin().declareExchange(exchange);

            Queue toWorkerQueue = new Queue("dst." + region);
            rabbit.getAdmin().declareQueue(toWorkerQueue);

            Queue fromWorkerQueue = new Queue("requests." + region);
            rabbit.getAdmin().declareQueue(fromWorkerQueue);

            Binding binding = BindingBuilder.bind(fromWorkerQueue).to(exchange).with("requests." + region).noargs();
            rabbit.getAdmin().declareBinding(binding);
            binding = BindingBuilder.bind(toWorkerQueue).to(exchange).with("test.to.worker").noargs();
            rabbit.getAdmin().declareBinding(binding);

            // send an message to consume
            rabbit.getClient().convertAndSend("dst.workers", "test.to.worker",
                "{\"id\":\"" + id + "\",\"region\":\"" + region.toUpperCase() + "\",\"pickup\":{\"longitude\":" + longitude + ",\"latitude\":" + latitude + "}}");
            containerId = client
                .createContainerCmd(WORKER_IMAGE)
                .withHostConfig(HostConfig.newHostConfig().withNetworkMode("dst"))
                .withCmd(region)
                .exec()
                .getId();
            client.startContainerCmd(containerId).exec();

            // wait for the response of the container
            Message messageFromWorker = rabbit.getClient().receive("requests." + region, 20000);
            assertThat(messageFromWorker, notNullValue());
            assertThat(messageFromWorker.getBody(), notNullValue());
            String message = new String(messageFromWorker.getBody());
            LOG.info("driver regex: " + driverRegex);
            LOG.info("Received message: " + message);
            assertThat("The result message of the worker is not correct!",
                message.matches(regex), is(true));

            assertThat("The driverId is not correct!", message.matches(driverRegex), is(true));

        } finally {
            if (containerId != null) {
                client.killContainerCmd(containerId).exec();
                client.removeContainerCmd(containerId).exec();
            }
            rabbit.getAdmin().deleteQueue("dst." + region);
            rabbit.getAdmin().deleteQueue("requests." + region);
            rabbit.getAdmin().deleteExchange("dst.workers");
        }
    }


}
