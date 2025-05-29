package dst.ass3.messaging.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import dst.ass3.messaging.Constants;
import dst.ass3.messaging.IRequestGateway;
import dst.ass3.messaging.Region;
import dst.ass3.messaging.TripRequest;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static dst.ass3.messaging.Region.*;

public class RequestGateway implements IRequestGateway {

    private Connection connection;
    private Channel channel;
    private ObjectMapper objectMapper;

    public RequestGateway() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(Constants.RMQ_HOST);
            factory.setPort(Integer.parseInt(Constants.RMQ_PORT));
            factory.setUsername(Constants.RMQ_USER);
            factory.setPassword(Constants.RMQ_PASSWORD);
            factory.setVirtualHost(Constants.RMQ_VHOST);
            this.connection = factory.newConnection();
            this.channel = connection.createChannel();

            // Declare a topic exchange for worker routing
            channel.exchangeDeclare(Constants.TOPIC_EXCHANGE, "topic", true);

            this.objectMapper = new ObjectMapper();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Failed to initialize RabbitMQ connection", e);
        }
    }

    @Override
    public void submitRequest(TripRequest request) {
        try {
            String routingKey = getRoutingKeyForRegion(request.getRegion());
            String jsonMessage = objectMapper.writeValueAsString(request);
            channel.basicPublish(Constants.TOPIC_EXCHANGE, routingKey, null, jsonMessage.getBytes());
            System.out.println("Published TripRequest to exchange with routing key: " + routingKey);
        } catch (IOException e) {
            throw new RuntimeException("Failed to publish TripRequest", e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (channel != null && channel.isOpen()) channel.close();
            if (connection != null && connection.isOpen()) connection.close();
        } catch (TimeoutException e) {
            throw new IOException("Failed to close RabbitMQ connection cleanly", e);
        }
    }

    private String getRoutingKeyForRegion(Region region) {
        return switch (region) {
            case AT_VIENNA -> Constants.ROUTING_KEY_AT_VIENNA;
            case AT_LINZ -> Constants.ROUTING_KEY_AT_LINZ;
            case DE_BERLIN -> Constants.ROUTING_KEY_DE_BERLIN;
        };
    }
}
