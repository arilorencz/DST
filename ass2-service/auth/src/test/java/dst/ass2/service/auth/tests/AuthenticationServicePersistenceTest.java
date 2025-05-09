package dst.ass2.service.auth.tests;

import dst.ass1.grading.GitHubClassroomGrading;
import dst.ass1.jpa.model.IModelFactory;
import dst.ass1.jpa.tests.TestData;
import dst.ass2.service.auth.AuthenticationServiceApplication;
import dst.ass2.service.auth.ICachingAuthenticationService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = AuthenticationServiceApplication.class)
@Transactional
@ActiveProfiles("testdata")
public class AuthenticationServicePersistenceTest implements ApplicationContextAware {
    @PersistenceContext
    private EntityManager em;

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationServicePersistenceTest.class);

    private ICachingAuthenticationService authenticationService;
    private ApplicationContext ctx;
    private IModelFactory modelFactory;

    @Before
    public void setUp() {
        LOG.info("Test resolving beans from application context");
        authenticationService = ctx.getBean(ICachingAuthenticationService.class);
        modelFactory = ctx.getBean(IModelFactory.class);

        // reload the data before each test
        authenticationService.loadData();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 5)
    public void loadDataUsesPostConstruct() throws NoSuchMethodException {
        // Check if the match method has the correct transaction attributes
        Class<?> clazz = AopProxyUtils.ultimateTargetClass(authenticationService);

        Method matchMethod = clazz.getMethod("loadData");

        // Verify method is annotated with @Transactional
        PostConstruct transactionalAnnotation = matchMethod.getAnnotation(PostConstruct.class);
        Assert.assertNotNull("loadData method should be annotated with @PostConstruct", transactionalAnnotation);
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 5)
    public void testAuthenticateUsesOnlyReadLock() {
        // Create the service instance
        ICachingAuthenticationService authService = authenticationService;

        // Create mock locks
        ReadWriteLock mockLock = mock(ReadWriteLock.class);
        Lock mockReadLock = mock(Lock.class);
        Lock mockWriteLock = mock(Lock.class);

        // Setup the mock to return our mock locks
        when(mockLock.readLock()).thenReturn(mockReadLock);
        when(mockLock.writeLock()).thenReturn(mockWriteLock);

        // Set our mock lock in the service
        authService.setUserToPassLock(mockLock);

        // Call authenticate method
        try {
            authService.authenticate(TestData.RIDER_1_EMAIL, TestData.RIDER_1_PW);
        } catch (Exception e) {
            // Ignore exceptions for this test
        }

        // Verify that only read lock was used
        verify(mockLock, atLeastOnce()).readLock();
        verify(mockReadLock, times(1)).lock();
        verify(mockReadLock, times(1)).unlock();

        // Verify write lock was never used
        verify(mockLock, never()).writeLock();
        verify(mockWriteLock, never()).lock();
        verify(mockWriteLock, never()).unlock();
    }


    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 5)
    public void testChangePasswordUsesOnlyWriteLock() {
        // Create the service instance
        ICachingAuthenticationService authService = authenticationService;

        // Create mock locks
        ReadWriteLock mockLock = mock(ReadWriteLock.class);
        Lock mockReadLock = mock(Lock.class);
        Lock mockWriteLock = mock(Lock.class);

        // Setup the mock to return our mock locks
        when(mockLock.readLock()).thenReturn(mockReadLock);
        when(mockLock.writeLock()).thenReturn(mockWriteLock);

        // Set our mock lock in the service
        authService.setUserToPassLock(mockLock);

        // Call changePassword method
        try {
            authService.changePassword(TestData.RIDER_1_EMAIL, "newpassword");
        } catch (Exception e) {
            // Ignore exceptions for this test
        }

        // Verify that only write lock was used
        verify(mockLock, atLeastOnce()).writeLock();
        verify(mockWriteLock, times(1)).lock();
        verify(mockWriteLock, times(1)).unlock();

        // Verify read lock was never used
        verify(mockLock, never()).readLock();
        verify(mockReadLock, never()).lock();
        verify(mockReadLock, never()).unlock();
    }




}
