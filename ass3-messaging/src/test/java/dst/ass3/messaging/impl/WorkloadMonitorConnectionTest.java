package dst.ass3.messaging.impl;

import com.rabbitmq.client.*;
import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.domain.QueueInfo;
import dst.ass3.grading.GitHubClassroomGrading;
import dst.ass3.grading.LocalGradingClassRule;
import dst.ass3.grading.LocalGradingRule;
import dst.ass3.messaging.IMessagingFactory;
import dst.ass3.messaging.IWorkloadMonitor;
import dst.ass3.messaging.Region;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class WorkloadMonitorConnectionTest {

    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();


    private IMessagingFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new MessagingFactory();
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 5)
    public void testHttpClientIsUsedForQueueInfo() throws Exception {
        Client mockHttpClient = mock(Client.class);
        QueueInfo mockQueueInfo = mock(QueueInfo.class);
        when(mockQueueInfo.getMessagesReady()).thenReturn(5L);
        when(mockQueueInfo.getMessagesUnacknowledged()).thenReturn(2L);
        when(mockQueueInfo.getConsumerCount()).thenReturn(3L);

        when(mockHttpClient.getQueue(anyString(), anyString())).thenReturn(mockQueueInfo);

        ConnectionFactory mockFactory = mock(ConnectionFactory.class);
        Connection mockConnection = mock(Connection.class);
        Channel mockChannel = mock(Channel.class);
        when(mockFactory.newConnection()).thenReturn(mockConnection);
        when(mockConnection.createChannel()).thenReturn(mockChannel);

        IWorkloadMonitor monitor = factory.createWorkloadMonitorWithClientAndConnectionFactory(mockHttpClient, mockFactory);

        assertNotNull(monitor);

        monitor.getRequestCount();
        monitor.getWorkerCount();

        for (Region region : Region.values()) {
            verify(mockHttpClient, atLeastOnce())
                .getQueue(eq("/"), eq("dst." + region.toString().toLowerCase()));
        }
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 10)
    public void testSubscribeRegistersConsumers() throws IOException, TimeoutException {
        Client mockHttpClient;
        ConnectionFactory mockFactory;
        Connection mockConnection;
        Channel mockChannel;

        mockHttpClient = mock(Client.class);
        mockFactory = mock(ConnectionFactory.class);
        mockConnection = mock(Connection.class);
        mockChannel = mock(Channel.class);

        when(mockFactory.newConnection()).thenReturn(mockConnection);
        when(mockConnection.createChannel()).thenReturn(mockChannel);
        when(mockChannel.queueDeclare()).thenReturn(mock(AMQP.Queue.DeclareOk.class));
        when(mockChannel.queueDeclare().getQueue()).thenReturn("test-queue");
        AMQP.Queue.BindOk mockBindOk = mock(AMQP.Queue.BindOk.class);
        when(mockChannel.queueBind(anyString(), anyString(), anyString())).thenReturn(mockBindOk);
        when(mockChannel.basicConsume(anyString(), anyBoolean(), any(Consumer.class))).thenReturn("consumer-tag");

        IWorkloadMonitor monitor = factory.createWorkloadMonitorWithClientAndConnectionFactory(mockHttpClient, mockFactory);

        monitor.subscribe();

        for (Region region : Region.values()) {
            verify(mockChannel, atLeastOnce()).queueDeclare();
            verify(mockChannel, atLeastOnce()).queueBind(anyString(), anyString(), eq("requests." + region.toString().toLowerCase()));
            verify(mockChannel, atLeastOnce()).basicConsume(anyString(), eq(true), any(Consumer.class));
        }
    }



}
