package dst.ass3.messaging.impl;

import com.rabbitmq.client.*;
import dst.ass3.messaging.Constants;
import dst.ass3.messaging.IQueueManager;
import dst.ass3.messaging.Region;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class QueueManager implements IQueueManager {
    private ConnectionFactory factory;
    private Connection connection;

    public QueueManager() {
        factory = new ConnectionFactory();
        factory.setHost(Constants.RMQ_HOST);
        factory.setPort(Integer.parseInt(Constants.RMQ_PORT));
        factory.setUsername(Constants.RMQ_USER);
        factory.setPassword(Constants.RMQ_PASSWORD);
    }

    @Override
    public void setUp() {
        try (Channel channel = getOrCreateConnection().createChannel()) {

            //declare the topic exchange
            channel.exchangeDeclare(Constants.TOPIC_EXCHANGE, BuiltinExchangeType.TOPIC, true);

            //declare queues and bind them to the exchange
            for (var queue : Constants.WORK_QUEUES) {
                String regionSuffix = queue.split("\\.")[1];
                String routingKey = "requests." + regionSuffix;

                channel.queueDeclare(queue, true, false, false, null);
                channel.queueBind(queue, Constants.TOPIC_EXCHANGE, routingKey);
            }
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Failed to set up RabbitMQ queues and exchange", e);
        }
    }

    @Override
    public void tearDown() {
        try (Channel channel = getOrCreateConnection().createChannel()) {

            //delete all queues and exchanges
            for (var queue : Constants.WORK_QUEUES) {
                channel.queueDelete(queue);
            }
            channel.exchangeDelete(Constants.TOPIC_EXCHANGE);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Failed to tear down RabbitMQ queues and exchange", e);
        }
    }

    @Override
    public void close() throws IOException {
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
    }

    private Connection getOrCreateConnection() throws IOException, TimeoutException {
        if (connection == null || !connection.isOpen()) {
            connection = factory.newConnection();
        }
        return connection;
    }
}
