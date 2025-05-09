package dst.ass3.messaging.impl;

import com.rabbitmq.http.client.domain.ExchangeInfo;
import com.rabbitmq.http.client.domain.QueueInfo;
import dst.ass3.grading.GitHubClassroomGrading;
import dst.ass3.grading.LocalGradingClassRule;
import dst.ass3.grading.LocalGradingRule;
import dst.ass3.messaging.IMessagingFactory;
import dst.ass3.messaging.IQueueManager;
import dst.ass3.messaging.RabbitResource;
import org.junit.*;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static dst.ass3.messaging.Constants.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class QueueManagerTest {

    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();

    @Rule
    public RabbitResource rabbit = new RabbitResource();

    @Rule
    public Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

    private IMessagingFactory factory = new MessagingFactory();
    private IQueueManager queueManager;

    @Before
    public void setUp() throws Exception {
        factory = new MessagingFactory();
        queueManager = factory.createQueueManager();
    }

    @After
    public void tearDown() throws Exception {
        try {
            queueManager.close();
        } catch (IOException e) {
            // ignore
        }
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 10)
    public void setUp_createsQueues() throws Exception {
        queueManager.setUp();

        try {
            List<QueueInfo> queues = rabbit.getManager().getQueues();
            assertThat(queues.size(), not(0));

            // make sure all work queues exist
            List<String> queueNames = queues.stream().map(QueueInfo::getName).collect(Collectors.toList());
            Arrays.stream(WORK_QUEUES)
                .forEach(wq -> assertThat(queueNames, hasItem(wq)));
        } finally {
            queueManager.tearDown();
        }
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 10)
    public void setUp_createsExchange() throws Exception {
        queueManager.setUp();
        try {
            ExchangeInfo exchange = rabbit.getManager().getExchange(RMQ_VHOST, TOPIC_EXCHANGE);
            assertThat(exchange, notNullValue());
        } finally {
            queueManager.tearDown();
        }
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 3)
    public void tearDown_removesQueues() throws Exception {
        queueManager.setUp();
        queueManager.tearDown();
        List<QueueInfo> queues = rabbit.getManager().getQueues();
        List<String> queueNames = queues.stream().map(QueueInfo::getName).collect(Collectors.toList());
        Arrays.stream(WORK_QUEUES)
            .forEach(wq -> assertThat(queueNames, not(hasItem(wq))));
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 2)
    public void tearDown_removesExchange() throws Exception {
        queueManager.setUp();
        queueManager.tearDown();
        ExchangeInfo exchange = rabbit.getManager().getExchange(RMQ_VHOST, TOPIC_EXCHANGE);
        assertThat(exchange, nullValue());
    }
}
