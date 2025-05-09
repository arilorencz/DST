package dst.ass3.messaging.impl;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.http.client.Client;
import dst.ass3.messaging.IMessagingFactory;
import dst.ass3.messaging.IQueueManager;
import dst.ass3.messaging.IRequestGateway;
import dst.ass3.messaging.IWorkloadMonitor;

public class MessagingFactory implements IMessagingFactory {

    @Override
    public IQueueManager createQueueManager() {
        // TODO
        return null;
    }

    @Override
    public IRequestGateway createRequestGateway() {
        // TODO
        return null;
    }

    @Override
    public IWorkloadMonitor createWorkloadMonitor() {
        // TODO
        return null;
    }

    @Override
    public IRequestGateway createRequestGatewayWithConnectionFactory(ConnectionFactory factory) {
        // TODO
        return null;
    }

    @Override
    public IWorkloadMonitor createWorkloadMonitorWithClientAndConnectionFactory(Client client, ConnectionFactory factory) {
        // TODO
        return null;
    }

    @Override
    public void close() {
        // implement if needed
    }
}
