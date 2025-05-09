package dst.ass3.messaging.impl;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import dst.ass3.grading.GitHubClassroomGrading;
import dst.ass3.grading.LocalGradingClassRule;
import dst.ass3.grading.LocalGradingRule;
import dst.ass3.messaging.IMessagingFactory;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class RequestGatewayConnectionTest {

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
    public void testConnectsToRabbitMQ() throws Exception {
        // Arrange
        ConnectionFactory mockFactory = mock(ConnectionFactory.class);
        Connection mockConnection = mock(Connection.class);
        Channel mockChannel = mock(Channel.class);

        when(mockFactory.newConnection()).thenReturn(mockConnection);
        when(mockConnection.createChannel()).thenReturn(mockChannel);

        // Act
        factory.createRequestGatewayWithConnectionFactory(mockFactory);

        // Assert
        verify(mockFactory, times(1)).newConnection();
        verify(mockConnection, times(1)).createChannel();
    }

}
