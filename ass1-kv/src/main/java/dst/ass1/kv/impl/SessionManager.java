package dst.ass1.kv.impl;

import dst.ass1.kv.ISessionManager;
import dst.ass1.kv.SessionCreationFailedException;
import dst.ass1.kv.SessionNotFoundException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Transaction;

import java.util.List;
import java.util.UUID;

public class SessionManager implements ISessionManager {
    private static final String SESSION_INDEX_KEY = "userSession";
    private final JedisPool pool;

    public SessionManager(String host, int port) {
        pool = new JedisPool(new JedisPoolConfig(), host, port);
    }

    @Override
    public String createSession(Long userId, int timeToLive) throws SessionCreationFailedException {
        try (Jedis jedis = pool.getResource()) {
            String token = UUID.randomUUID().toString();

            // start a transaction
            jedis.watch(SESSION_INDEX_KEY, token); // to ensure session doesn't exist
            var tsc = jedis.multi();

            tsc.hset(token, "userId", userId.toString());
            tsc.hset(token, "timeToLive", String.valueOf(timeToLive));
            tsc.expire(token, timeToLive);

            tsc.zremrangeByScore(SESSION_INDEX_KEY, userId, userId); // remove any existing sessions
            tsc.zadd(SESSION_INDEX_KEY, userId, token); //add session

            var results = tsc.exec();

            if (results == null || results.isEmpty()) {
                throw new SessionCreationFailedException("Transaction failed while creating session.");
            }

            return token;

        } catch (Exception e) {
            throw new SessionCreationFailedException("Failed to create session", e);
        }
    }

    @Override
    public void setSessionVariable(String sessionId, String key, String value) throws SessionNotFoundException {
        try (Jedis jedis = pool.getResource()) {
            if (!jedis.exists(sessionId)) {
                throw new SessionNotFoundException("Session ID not found: " + sessionId);
            }
            jedis.hset(sessionId, key, value);
        }
    }

    @Override
    public String getSessionVariable(String sessionId, String key) throws SessionNotFoundException {
        try (Jedis jedis = pool.getResource()) {
            if (!jedis.exists(sessionId)) {
                throw new SessionNotFoundException("Session ID not found: " + sessionId);
            }
            return jedis.hget(sessionId, key); // returns null if key doesn't exist
        }
    }

    @Override
    public Long getUserId(String sessionId) throws SessionNotFoundException {
        try (Jedis jedis = pool.getResource()) {
            if (!jedis.exists(sessionId)) {
                throw new SessionNotFoundException("Session ID not found: " + sessionId);
            }
            String userId = jedis.hget(sessionId, "userId");
            return userId != null ? Long.valueOf(userId) : null;
        }
    }

    @Override
    public int getTimeToLive(String sessionId) throws SessionNotFoundException {
        try (Jedis jedis = pool.getResource()) {
            if (!jedis.exists(sessionId)) {
                throw new SessionNotFoundException("Session ID not found: " + sessionId);
            }
            return Integer.parseInt(getSessionVariable(sessionId, "timeToLive"));
        }
    }

    @Override
    public String requireSession(Long userId, int timeToLive) throws SessionCreationFailedException {
        try (Jedis jedis = pool.getResource()) {
            jedis.watch(SESSION_INDEX_KEY); // for race conditions

            var sessionIds = jedis.zrangeByScore(SESSION_INDEX_KEY, userId, userId);
            if (!sessionIds.isEmpty()) {
                String potentialSessionId = sessionIds.get(0);
                if (jedis.exists(potentialSessionId)) {
                    jedis.unwatch();
                    return potentialSessionId;
                }
            }

        } catch (Exception e) {
            throw new SessionCreationFailedException("requireSession failed", e);
        }

        return createSession(userId, timeToLive);
    }


    @Override
    public void close() {
        pool.close();
    }
}
