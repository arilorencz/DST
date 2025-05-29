package dst.ass2.ioc.lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LockManager {
    private static final LockManager instance = new LockManager();
    private final ConcurrentHashMap<String, ReentrantLock> locks;

    private LockManager() {
        locks = new ConcurrentHashMap<>();
    }

    public static LockManager getInstance() {
        return instance;
    }

    public ReentrantLock getLock(String name) {
        return locks.computeIfAbsent(name, k -> new ReentrantLock());
    }
}
