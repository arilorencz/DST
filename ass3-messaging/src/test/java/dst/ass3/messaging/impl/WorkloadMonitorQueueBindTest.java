package dst.ass3.messaging.impl;

import com.rabbitmq.client.*;
import com.rabbitmq.http.client.Client;
import dst.ass3.grading.GitHubClassroomGrading;
import dst.ass3.grading.LocalGradingClassRule;
import dst.ass3.grading.LocalGradingRule;
import dst.ass3.messaging.Constants;
import dst.ass3.messaging.IMessagingFactory;
import dst.ass3.messaging.IWorkloadMonitor;
import dst.ass3.messaging.Region;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class WorkloadMonitorQueueBindTest {

    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();


    private Client mockHttpClient;
    private ConnectionFactory mockFactory;
    private Connection mockConnection;
    private Channel mockChannel;
    private IMessagingFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new MessagingFactory();

        mockHttpClient = mock(Client.class);
        mockFactory = mock(ConnectionFactory.class);
        mockConnection = mock(Connection.class);
        mockChannel = mock(Channel.class);

        when(mockFactory.newConnection()).thenReturn(mockConnection);
        when(mockConnection.createChannel()).thenReturn(mockChannel);

        // Mock queueDeclare to return a mock with a unique queue name for each call
        AMQP.Queue.DeclareOk declareOk = mock(AMQP.Queue.DeclareOk.class);
        when(mockChannel.queueDeclare()).thenReturn(declareOk);
        when(declareOk.getQueue()).thenReturn("test-queue");
        when(mockChannel.queueBind(anyString(), anyString(), anyString()))
            .thenReturn(mock(AMQP.Queue.BindOk.class));
        when(mockChannel.basicConsume(anyString(), anyBoolean(), any(Consumer.class)))
            .thenReturn("consumer-tag");
    }

    @Test(timeout = 60000)
    @GitHubClassroomGrading(maxScore = 10)
    public void testQueuesAreBoundToTopicExchangeWithCorrectRoutingKeys() throws IOException {
        IWorkloadMonitor monitor = factory.createWorkloadMonitorWithClientAndConnectionFactory(mockHttpClient, mockFactory);
        assertNotNull(monitor);

        monitor.subscribe();

        for (Region region : Region.values()) {
            String expectedRoutingKey = "requests." + region.toString().toLowerCase();
            // Verify queueBind is called with the topic exchange and correct routing key
            verify(mockChannel, atLeastOnce()).queueBind(
                anyString(), // queueName
                eq(Constants.TOPIC_EXCHANGE),
                eq(expectedRoutingKey)
            );
        }
    }
}
