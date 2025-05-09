package dst.ass1.kv;

import org.junit.rules.ExternalResource;
import redis.clients.jedis.Jedis;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class EmbeddedRedis extends ExternalResource {
    private Properties properties;
    private RedisServer redisServer;
    private Jedis jedis;

    @Override
    protected void before() throws IOException {
        properties = loadProperties();

        int port = Integer.parseInt(properties.getProperty("redis.port"));
        String host = properties.getProperty("redis.host");

        this.redisServer = new RedisServer(port);
        redisServer.start();

        jedis = new Jedis(host, port);

        jedis.flushAll();
    }

    @Override
    protected void after() {
        try {
            jedis.close();
            redisServer.stop();
        } catch (IOException e) {
            throw new RuntimeException("Error when shutting down the testing redis server", e);
        }
    }

    private Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("redis.properties")) {
            properties.load(in);
        }
        return properties;
    }

    public Properties getProperties() {
        return properties;
    }

    public Jedis getJedis() {
        return jedis;
    }
}
