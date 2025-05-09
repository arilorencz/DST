package dst.ass3.messaging;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.http.client.Client;

import java.io.Closeable;
import java.io.IOException;

public interface IMessagingFactory extends Closeable {

    IQueueManager createQueueManager();

    IRequestGateway createRequestGateway();

    IWorkloadMonitor createWorkloadMonitor();

    IWorkloadMonitor createWorkloadMonitorWithClientAndConnectionFactory(Client client, ConnectionFactory factory);

    IRequestGateway createRequestGatewayWithConnectionFactory(ConnectionFactory factory);


    /**
     * Closes any resource the factory may create.
     *
     * @throws IOException propagated exceptions
     */
    @Override
    void close() throws IOException;
}
