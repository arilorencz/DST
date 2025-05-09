package dst.ass1.kv.tests;

import dst.ass1.kv.EmbeddedRedis;
import dst.ass1.kv.ISessionManager;
import dst.ass1.kv.SessionCreationFailedException;
import dst.ass1.kv.grading.GitHubClassroomGrading;
import dst.ass1.kv.grading.LocalGradingClassRule;
import dst.ass1.kv.grading.LocalGradingRule;
import dst.ass1.kv.impl.SessionManagerFactory;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

public class Ass1_5_2_ConcurrencyTest {
    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();

    @Rule
    public EmbeddedRedis redisRule = new EmbeddedRedis();

    private CountDownLatch latch;

    @Test
    @GitHubClassroomGrading(maxScore = 20)
    public void requireSession_concurrentAccess_behavesCorrectly() throws Exception {
        latch = new CountDownLatch(1);

        SessionManagerFactory fact = new SessionManagerFactory();
        ISessionManager m1 = fact.createSessionManager(redisRule.getProperties());
        ISessionManager m2 = fact.createSessionManager(redisRule.getProperties());
        ISessionManager m3 = fact.createSessionManager(redisRule.getProperties());
        ISessionManager m4 = fact.createSessionManager(redisRule.getProperties());

        RequireSessionRunner r1 = new RequireSessionRunner(m1);
        RequireSessionRunner r2 = new RequireSessionRunner(m2);
        RequireSessionRunner r3 = new RequireSessionRunner(m3);
        RequireSessionRunner r4 = new RequireSessionRunner(m4);

        Thread t1 = new Thread(r1, "thread-r1");
        Thread t2 = new Thread(r2, "thread-r2");
        Thread t3 = new Thread(r3, "thread-r3");
        Thread t4 = new Thread(r4, "thread-r4");

        // chaos ensues
        t1.start();
        t2.start();
        t3.start();
        t4.start();

        // wait for thread spin up
        Thread.sleep(1000);
        // execute threads
        latch.countDown();
        // wait for them to finish.
        t1.join();
        t2.join();
        t3.join();
        t4.join();

        // print and assert
        System.out.printf("%s: result = %s, exception = %s%n", t1.getName(), r1.result, r1.exception);
        System.out.printf("%s: result = %s, exception = %s%n", t2.getName(), r2.result, r2.exception);
        System.out.printf("%s: result = %s, exception = %s%n", t3.getName(), r3.result, r3.exception);
        System.out.printf("%s: result = %s, exception = %s%n", t4.getName(), r4.result, r4.exception);

        List<String> results = new ArrayList<>(Arrays.asList(r1.result, r2.result, r3.result, r4.result));
        results.removeIf(Objects::isNull);

        assertThat("requireSession should return at least one result", results.size(), greaterThan(0));
        String sessionId = results.get(0);
        if (results.size() > 1) {
            for (int i = 1; i < results.size(); i++) {
                assertThat("requireSession should return consistent results", results.get(i), is(sessionId));
            }
        }

        System.out.println("Running requireSession again");
        Thread.sleep(1000); // run require session again, this time no exception should be caught
        RequireSessionRunner r5 = new RequireSessionRunner(fact.createSessionManager(redisRule.getProperties()));
        r5.run();
        assertThat(r5.result, is(sessionId));
        assertThat(r5.exception, nullValue());
    }

    public class RequireSessionRunner implements Runnable {

        private ISessionManager manager;
        String result;
        SessionCreationFailedException exception;

        public RequireSessionRunner(ISessionManager manager) {
            this.manager = manager;
        }

        @Override
        public void run() {
            try {
                String msg = "Executing " + Thread.currentThread().getName();
                latch.await();
                System.out.println(msg);
                result = manager.requireSession(42L, 1000);
            } catch (SessionCreationFailedException e) {
                this.exception = e;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public Exception getException() {
            return exception;
        }

        public String getResult() {
            return result;
        }
    }
}
