package dst.ass1.kv.impl;

import dst.ass1.kv.ISessionManager;
import dst.ass1.kv.ISessionManagerFactory;

import java.util.Properties;

public class SessionManagerFactory implements ISessionManagerFactory {

    @Override
    public ISessionManager createSessionManager(Properties properties) {
        // read "redis.host" and "redis.port" from the properties
        String host = (String) properties.get("redis.host");
        int port = Integer.parseInt((String)properties.get("redis.port"));

        return new SessionManager(host, port);
    }
}
