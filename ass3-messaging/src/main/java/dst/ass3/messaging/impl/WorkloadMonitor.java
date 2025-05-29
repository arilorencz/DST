package dst.ass3.messaging.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.domain.QueueInfo;
import dst.ass3.messaging.Constants;
import dst.ass3.messaging.IWorkloadMonitor;
import dst.ass3.messaging.Region;
import dst.ass3.messaging.WorkerResponse;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class WorkloadMonitor implements IWorkloadMonitor {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<Region, LinkedList<Long>> processingTimes = new EnumMap<>(Region.class);

    private Client httpClient;
    private Connection amqpConnection;
    private ConnectionFactory factory;
    private Channel amqpChannel;
    private String tempQueueName;

    public WorkloadMonitor() {
        try {
            // Set up AMQP connection
            factory = new ConnectionFactory();
            factory.setHost(Constants.RMQ_HOST);
            factory.setPort(Integer.parseInt(Constants.RMQ_PORT));
            factory.setUsername(Constants.RMQ_USER);
            factory.setPassword(Constants.RMQ_PASSWORD);

            httpClient = new Client(new URL(Constants.RMQ_API_URL), Constants.RMQ_USER, Constants.RMQ_PASSWORD);

            subscribe();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize RabbitMQ HTTP client", e);
        }
    }

    public WorkloadMonitor(Client client, ConnectionFactory factory) {
        this.httpClient = client;
        this.factory = factory;

        try {
            subscribe();
        } catch (Exception e) {
            throw new RuntimeException("Failed to subscribe in alternate constructor", e);
        }
    }

    @Override
    public Map<Region, Long> getRequestCount() {
        Map<Region, Long> result = new EnumMap<>(Region.class);
        for (Region region : Region.values()) {
            String queueName = "dst." + region.name().toLowerCase();
            try {
                QueueInfo queue = httpClient.getQueue("/", queueName);
                result.put(region, queue.getMessagesReady());
            } catch (Exception e) {
                result.put(region, 0L); // fallback or log if needed
            }
        }
        return result;
    }

    @Override
    public Map<Region, Long> getWorkerCount() {
        Map<Region, Long> result = new EnumMap<>(Region.class);
        for (Region region : Region.values()) {
            String queueName = "dst." + region.name().toLowerCase();
            try {
                QueueInfo queue = httpClient.getQueue("/", queueName);
                result.put(region, queue.getConsumerCount());
            } catch (Exception e) {
                result.put(region, 0L); // fallback or log if needed
            }
        }
        return result;
    }

    @Override
    public Map<Region, Double> getAverageProcessingTime() {
        Map<Region, Double> averages = new EnumMap<>(Region.class);

        for (Map.Entry<Region, LinkedList<Long>> entry : processingTimes.entrySet()) {
            List<Long> times = entry.getValue();
            double avg = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
            averages.put(entry.getKey(), avg);
        }

        return averages;
    }

    @Override
    public void subscribe() {
        try {
            amqpConnection = factory.newConnection();
            amqpChannel = amqpConnection.createChannel();

            // Limit message dispatch to 1 per consumer at a time
            amqpChannel.basicQos(1);

            // Create a temporary, auto-delete queue
            String uniqueQueueName = "monitor.queue." + UUID.randomUUID();
            amqpChannel.queueDeclare(uniqueQueueName, false, false, true, null);
            tempQueueName = uniqueQueueName;

            // Start consuming messages
            Consumer consumer = new DefaultConsumer(amqpChannel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                        throws IOException {
                    String routingKey = envelope.getRoutingKey();
                    String message = new String(body);
                    Region region = Region.valueOf(routingKey.split("\\.")[1].toUpperCase());
                    handleWorkerMessage(region, message);
                }
            };

            amqpChannel.basicConsume(tempQueueName, true, consumer);

            // Bind the temp queue to routing keys for each region
            amqpChannel.queueBind(tempQueueName, Constants.TOPIC_EXCHANGE, Constants.ROUTING_KEY_AT_VIENNA);
            amqpChannel.queueBind(tempQueueName, Constants.TOPIC_EXCHANGE, Constants.ROUTING_KEY_AT_LINZ);
            amqpChannel.queueBind(tempQueueName, Constants.TOPIC_EXCHANGE, Constants.ROUTING_KEY_DE_BERLIN);

        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Failed to subscribe to worker feedback topics", e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            // Delete only our temp queue
            if (amqpChannel != null && tempQueueName != null) {
                amqpChannel.queueDelete(tempQueueName, false, false);
            }

            // Clean up extra monitor queues left over from other tests
            if (httpClient != null) {
                List<QueueInfo> queues = httpClient.getQueues();
                for (QueueInfo queue : queues) {
                    String qName = queue.getName();
                    if (!Arrays.asList(Constants.WORK_QUEUES).contains(qName) &&
                            qName.startsWith("monitor.queue.")) {
                        try {
                            amqpChannel.queueDelete(qName, false, false);
                        } catch (Exception e) {
                            // Might already be deleted or in use by another test
                        }
                    }
                }
            }

            if (amqpChannel != null) {
                try {
                    amqpChannel.close();
                } catch (TimeoutException e) {
                    throw new IOException("Timeout closing AMQP channel", e);
                }
            }

            if (amqpConnection != null) {
                amqpConnection.close();
            }

        } finally {
            httpClient = null;
            amqpChannel = null;
            amqpConnection = null;
        }
    }

    private synchronized void handleWorkerMessage(Region region, String message) {
        try {
            WorkerResponse response = objectMapper.readValue(message, WorkerResponse.class);
            processingTimes.putIfAbsent(region, new LinkedList<>());

            LinkedList<Long> times = processingTimes.get(region);
            times.add(response.getProcessingTime());

            if (times.size() > 10) {
                times.removeFirst();
            }

        } catch (JsonProcessingException e) {
            System.err.println("Failed to parse worker message: " + message);
        }
    }

    private boolean isWorkQueue(String queueName) {
        return Arrays.asList(Constants.WORK_QUEUES).contains(queueName);
    }

    private Region regionFromQueue(String queueName) {
        return Region.valueOf(queueName.substring(4).toUpperCase());
    }
}
