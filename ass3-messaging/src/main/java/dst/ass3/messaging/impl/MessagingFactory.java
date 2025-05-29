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
        return new QueueManager();
    }

    @Override
    public IRequestGateway createRequestGateway() {
        return new RequestGateway();
    }

    @Override
    public IWorkloadMonitor createWorkloadMonitor() {
        return new WorkloadMonitor();
    }

    @Override
    public IRequestGateway createRequestGatewayWithConnectionFactory(ConnectionFactory factory) {
        return new RequestGateway(factory);
    }

    @Override
    public IWorkloadMonitor createWorkloadMonitorWithClientAndConnectionFactory(Client client, ConnectionFactory factory) {
        return new WorkloadMonitor(client, factory);
    }

    @Override
    public void close() {
        // implement if needed
    }
}
