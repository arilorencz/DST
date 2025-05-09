package dst.ass2.ioc.tests.di;

import dst.ass2.ioc.di.IObjectContainer;
import dst.ass2.ioc.di.IObjectContainerFactory;
import dst.ass2.ioc.di.annotation.Component;
import dst.ass2.ioc.di.annotation.Scope;
import dst.ass2.ioc.di.impl.ObjectContainerFactory;
import dst.ass2.ioc.tests.grading.GitHubClassroomGrading;
import org.junit.Test;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class ObjectContainerSingletonTest {

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 10)
    public void testSingletonCreationIsThreadSafe() throws Exception {
        Properties properties = new Properties();
        IObjectContainerFactory factory = new ObjectContainerFactory();

        IObjectContainer container = factory.newObjectContainer(properties);


        int threadCount = 30;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicReference<TestSingleton> firstInstance = new AtomicReference<>();
        AtomicReference<Boolean> exceptionThrown = new AtomicReference<>();
        exceptionThrown.set(false);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await(); // Wait for all threads to be ready
                    TestSingleton singleton = container.getObject(TestSingleton.class);
                    firstInstance.compareAndSet(null, singleton);
                } catch (Exception e) {
                    exceptionThrown.set(true);
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(2, TimeUnit.SECONDS);

        assertFalse(exceptionThrown.get());
        TestSingleton expectedInstance = firstInstance.get();
        assertNotNull("Singleton instance should not be null", expectedInstance);

        for (int i = 0; i < threadCount; i++) {
            TestSingleton instance = container.getObject(TestSingleton.class);
            assertSame("All instances should be the same", expectedInstance, instance);
        }
    }

    @Component(scope = Scope.SINGLETON)
    public static class TestSingleton {
        private static int instanceCount = 0;

        public TestSingleton() throws InstantiationException {
            instanceCount++;
            if (instanceCount > 1) {
                throw new InstantiationException("Multiple instances created!");
            }
        }
    }
}
