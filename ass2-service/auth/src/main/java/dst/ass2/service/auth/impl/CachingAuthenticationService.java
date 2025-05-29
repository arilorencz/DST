package dst.ass2.service.auth.impl;

import dst.ass1.jpa.dao.IDAOFactory;
import dst.ass1.jpa.dao.IRiderDAO;
import dst.ass1.jpa.model.IRider;
import dst.ass2.service.api.auth.AuthenticationException;
import dst.ass2.service.api.auth.NoSuchUserException;
import dst.ass2.service.auth.ICachingAuthenticationService;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Singleton
@ManagedBean
public class CachingAuthenticationService implements ICachingAuthenticationService {

    @PersistenceContext
    private EntityManager em;
    @Inject
    IDAOFactory daoFactory;
    ConcurrentHashMap<String, byte[]> userCredentials = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, String> tokenCache = new ConcurrentHashMap<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();


    @Override
    public void changePassword(String email, String newPassword) throws NoSuchUserException {
        lock.writeLock().lock();
        try {
            IRiderDAO riderDAO = daoFactory.createRiderDAO();
            IRider rider = riderDAO.findByEmail(email);

            if (rider == null) {
                throw new NoSuchUserException("CachingAuthenticationService - changePassword: User not found.");
            }

            byte[] newHash = SHAsum(newPassword.getBytes());
            userCredentials.put(email, newHash);
            rider.setPassword(newHash);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String getUser(String token) {
        if (token == null) return null;
        return tokenCache.get(token);
    }

    @Override
    public boolean isValid(String token) {
        if (token == null) return false;
        return tokenCache.containsKey(token);
    }

    @Override
    public boolean invalidate(String token) {
        if (token == null) return false;
        return tokenCache.remove(token) != null;
    }

    @Override
    @Transactional
    public String authenticate(String email, String password) throws NoSuchUserException, AuthenticationException {
        lock.readLock().lock();

        try {
            //look up user/pw in cache or db
            byte[] storedHash = userCredentials.get(email);
            if (storedHash == null) {
                IRiderDAO riderDAO = daoFactory.createRiderDAO();
                IRider rider = riderDAO.findByEmail(email);
                if (rider == null) {
                    throw new NoSuchUserException("CachingAuthenticationService - authenticate: User not found: " + email);
                }
                storedHash = rider.getPassword();
                if (storedHash == null) {
                    throw new AuthenticationException("CachingAuthenticationService - authenticate: No password hash stored for user.");
                }
                userCredentials.put(email, storedHash);
            }

            //check pw hash
            byte[] inputHash = SHAsum(password.getBytes());

            if (!MessageDigest.isEqual(storedHash, inputHash)) {
                throw new AuthenticationException("CachingAuthenticationService - authenticate: Invalid credentials.");
            }

            //new token
            String token = java.util.UUID.randomUUID().toString();
            tokenCache.put(token, email);
            return token;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @PostConstruct
    public void loadData() {
        IRiderDAO riderDAO = daoFactory.createRiderDAO();
        for (IRider rider : riderDAO.findAll()) {
            byte[] passwordHash = rider.getPassword();
            if (rider.getEmail() != null && passwordHash != null) {
                userCredentials.put(rider.getEmail(), passwordHash);
            }
        }
    }

    @Override
    public void clearCache() {
        lock.writeLock().lock();
        try {
            userCredentials.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public EntityManager getEntityManager() {
        return em;
    }

    @Override
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    public void setUserToPassLock(ReadWriteLock readWriteLock) {
        this.lock = readWriteLock;
    }

    private static byte[] SHAsum(byte[] convertme){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return md.digest(convertme);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
