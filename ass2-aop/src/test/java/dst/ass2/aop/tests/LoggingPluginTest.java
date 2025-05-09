package dst.ass2.aop.tests;

import dst.ass2.aop.IPluginExecutable;
import dst.ass2.aop.event.Event;
import dst.ass2.aop.event.EventBus;
import dst.ass2.aop.event.EventType;
import dst.ass2.aop.grading.GitHubClassroomGrading;
import dst.ass2.aop.logging.Invisible;
import dst.ass2.aop.logging.LoggingAspect;
import dst.ass2.aop.sample.InvisiblePluginExecutable;
import dst.ass2.aop.sample.LoggingPluginExecutable;
import dst.ass2.aop.sample.SystemOutPluginExecutable;
import dst.ass2.aop.tests.plugin.ConsoleLoggerPluginExecutable;
import dst.ass2.aop.tests.plugin.LoggerPluginExecutable;
import dst.ass2.aop.util.PluginUtils;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.weaver.internal.tools.PointcutExpressionImpl;
import org.aspectj.weaver.tools.ShadowMatch;
import org.junit.Test;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.Advised;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static dst.ass2.aop.util.PluginUtils.*;
import static dst.ass2.aop.util.PluginUtils.EXECUTE_METHOD;
import static org.junit.Assert.*;

public class LoggingPluginTest {
    final EventBus eventBus = EventBus.getInstance();

    @org.junit.Before
    @org.junit.After
    public void beforeAndAfter() {
        eventBus.reset();
    }

    /**
     * Verifies that the {@link LoggingAspect} is a valid AspectJ aspect i.e., {@link Aspect @Aspect} as well as
     * {@link Around @Around} or {@link Before @Before} / {@link After @After}.
     */
    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 5)
    public void loggingAspect_isValid() {
        Aspect aspect = AnnotationUtils.findAnnotation(LoggingAspect.class, Aspect.class);
        assertNotNull("LoggingAspect is not annotated with @Aspect", aspect);

        Map<Method, Around> around = PluginUtils.findMethodAnnotation(LoggingAspect.class, Around.class);
        Map<Method, Before> before = PluginUtils.findMethodAnnotation(LoggingAspect.class, Before.class);
        Map<Method, After> after = PluginUtils.findMethodAnnotation(LoggingAspect.class, After.class);

        boolean found = !around.isEmpty() || (!before.isEmpty() && !after.isEmpty());
        assertTrue("LoggingAspect does not contain methods annotated with @Around OR @Before / @After", found);

        IPluginExecutable executable = getExecutable(
            LoggerPluginExecutable.class, LoggingAspect.class);
        assertTrue("Executable must implement the Advised interface",
            executable instanceof Advised);
        Advised advised = (Advised) executable;

        PointcutAdvisor pointcutAdvisor = getPointcutAdvisor(advised);
        assertNotNull(
            "PointcutAdvisor not found because there is no pointcut or the pointcut does not match",
            pointcutAdvisor);

        PointcutExpressionImpl pointcutExpression = getPointcutExpression(advised);
        ShadowMatch shadowMatch = pointcutExpression
            .matchesMethodExecution(EXECUTE_METHOD);
        assertTrue("Pointcut does not match IPluginExecute.execute()",
            shadowMatch.alwaysMatches());

        shadowMatch = pointcutExpression
            .matchesMethodExecution(INTERRUPTED_METHOD);
        assertTrue("Pointcut must not match IPluginExecute.interrupted()",
            shadowMatch.neverMatches());

        shadowMatch = pointcutExpression.matchesMethodExecution(ReflectionUtils
            .findMethod(getClass(), EXECUTE_METHOD.getName()));
        assertTrue("Pointcut must not match LoggingPluginTest.execute()",
            shadowMatch.neverMatches());
    }

    /**
     * Verifies that the pointcut expression of the {@link LoggingAspect} does not match any method except the
     * {@link IPluginExecutable#execute()} method.
     */
    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 10)
    public void pointcutExpression_matchesCorrectlyLogging() {
        IPluginExecutable executable = PluginUtils.getExecutable(LoggingPluginExecutable.class, LoggingAspect.class);
        assertTrue("Executable must implement the Advised interface", executable instanceof Advised);
        Advised advised = (Advised) executable;

        PointcutAdvisor pointcutAdvisor = PluginUtils.getPointcutAdvisor(advised);
        assertNotNull("PointcutAdvisor not found because there is no pointcut or the pointcut does not match", pointcutAdvisor);

        String expression = PluginUtils.getBestExpression(advised);
        assertTrue("Pointcut expression must include '" + IPluginExecutable.class.getName() + "'", expression.contains(IPluginExecutable.class.getName()));
        assertTrue("Pointcut expression must include '" + PluginUtils.EXECUTE_METHOD.getName() + "'", expression.contains(PluginUtils.EXECUTE_METHOD.getName()));

        PointcutExpressionImpl pointcutExpression = PluginUtils.getPointcutExpression(advised);
        ShadowMatch shadowMatch = pointcutExpression.matchesMethodExecution(PluginUtils.EXECUTE_METHOD);
        assertTrue("Pointcut does not match IPluginExecute.execute()", shadowMatch.alwaysMatches());

        shadowMatch = pointcutExpression.matchesMethodExecution(PluginUtils.INTERRUPTED_METHOD);
        assertTrue("Pointcut must not match IPluginExecute.interrupted()", shadowMatch.neverMatches());

        shadowMatch = pointcutExpression.matchesMethodExecution(ReflectionUtils.findMethod(getClass(), PluginUtils.EXECUTE_METHOD.getName()));
        assertTrue("Pointcut must not match LoggingPluginTest.execute()", shadowMatch.neverMatches());
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 10)
    public void test_correctLogImplementation() throws Exception {
        IPluginExecutable executable = getExecutable(
            LoggerPluginExecutable.class, LoggingAspect.class);
        Advised advised = (Advised) executable;

        // Add handler end check that there are no events
        addBusHandlerIfNecessary(advised);
        assertEquals("EventBus must be empty", 0,
            eventBus.count(EventType.INFO));

        // Execute plugin and check that there are 2 events
        executable.execute();
        List<Event> events = eventBus.getEvents(EventType.INFO);
        assertEquals("EventBus must exactly contain 2 INFO events", 2,
            events.size());
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 10)
    public void test_logContainsCorrectClassnames() throws Exception {
        test_correctLogImplementation();

        // Check if the logger contains the correct class name
        List<Event> events = eventBus.getEvents(EventType.INFO);
        for (Event event : events) {
            assertEquals("Event message must contain the name of the "
                    + LoggingAspect.class.getSimpleName(),
                LoggingAspect.class.getName(), event.getMessage());
            assertSame("Event must be logged for "
                    + LoggerPluginExecutable.class.getSimpleName(),
                LoggerPluginExecutable.class, event.getPluginClass());
        }
    }

    /**
     * Verifies that the pointcut expression of the LoggingAspect contains the {@link Invisible @Invisible} annotation.
     */
    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 5)
    public void pointcutExpression_containsInvisibleAnnotation() {
        IPluginExecutable executable = PluginUtils.getExecutable(LoggingPluginExecutable.class, LoggingAspect.class);
        Advised advised = (Advised) executable;

        String expression = PluginUtils.getBestExpression(advised);
        String annotationName = Invisible.class.getName();
        assertTrue("Pointcut expression does not contain " + annotationName, expression.contains(annotationName));
    }

    /**
     * Verifies that the pointcut expression of the {@link LoggingAspect} does not match any method annotated with
     * {@link Invisible @Invisible}.
     */
    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 5)
    public void pointcutExpression_doesNotMatchInvisible() {
        IPluginExecutable executable = PluginUtils.getExecutable(LoggingPluginExecutable.class, LoggingAspect.class);
        Advised advised = (Advised) executable;

        PointcutExpressionImpl pointcutExpression = PluginUtils.getPointcutExpression(advised);

        Method loggingMethod = ReflectionUtils.findMethod(LoggingPluginExecutable.class, PluginUtils.EXECUTE_METHOD.getName());
        ShadowMatch shadowMatch = pointcutExpression.matchesMethodExecution(loggingMethod);
        assertTrue("Pointcut does not match LoggingPluginExecutable.execute()", shadowMatch.alwaysMatches());

        Method invisibleMethod = ReflectionUtils.findMethod(InvisiblePluginExecutable.class, PluginUtils.EXECUTE_METHOD.getName());
        shadowMatch = pointcutExpression.matchesMethodExecution(invisibleMethod);
        assertTrue("Pointcut matches InvisiblePluginExecutable.execute()", shadowMatch.neverMatches());
    }


    @Test(timeout = 5000)
    @GitHubClassroomGrading(maxScore = 10)
    public void test_reuseExistingLogger() {
        loggingAspect_usesLogger();
        loggingAspect_usesSystemOut();
        loggingAspect_usesConsoleLogger();
    }

    /**
     * Tests if the {@link LoggingAspect} uses the {@link java.util.logging.Logger Logger} defined in the plugin.
     */
    public void loggingAspect_usesLogger() {
        IPluginExecutable executable = PluginUtils.getExecutable(LoggingPluginExecutable.class, LoggingAspect.class);
        Advised advised = (Advised) executable;

        // Add handler end check that there are no events
        PluginUtils.addBusHandlerIfNecessary(advised);
        assertEquals("EventBus must be empty", 0, eventBus.count(EventType.INFO));

        // Execute plugin and check that there are 2 events
        executable.execute();
        List<Event> events = eventBus.getEvents(EventType.INFO);
        assertEquals("EventBus must exactly contain 2 INFO events", 2, events.size());

        // Check if the logger contains the correct class name
        events = eventBus.getEvents(EventType.INFO);
        for (Event event : events) {
            assertEquals("Event message must contain the name of the " + LoggingAspect.class.getSimpleName(), LoggingAspect.class.getName(), event.getMessage());
            assertSame("Event must be logged for " + LoggingPluginExecutable.class.getSimpleName(), LoggingPluginExecutable.class, event.getPluginClass());
        }

        eventBus.reset();
    }

    /**
     * Tests if the {@link LoggingAspect} uses {@code System.out} if the plugin does not contain a
     * {@link java.util.logging.Logger Logger} field.
     */
    public void loggingAspect_usesSystemOut() {
        // Redirect System.out
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream out = System.out;
        System.setOut(new PrintStream(byteArrayOutputStream));
        try {
            // Execute plugin
            IPluginExecutable executable = PluginUtils.getExecutable(SystemOutPluginExecutable.class, LoggingAspect.class);
            assertEquals("EventBus must be empty", 0, eventBus.size());
            executable.execute();
            assertEquals("EventBus must exactly contain 2 events", 2, eventBus.size());

            // Verify that the log output contains the class name of the executed plugin
            String output = byteArrayOutputStream.toString();
            assertTrue(String.format("Log output must contain %s\n\tbut was%s", SystemOutPluginExecutable.class.getName(), output),
                output.contains(SystemOutPluginExecutable.class.getName()));
        } finally {
            // Reset System.out
            System.setOut(out);
        }
        eventBus.reset();
    }

    public void loggingAspect_usesConsoleLogger() {
        // Redirect System.out
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream out = System.out;
        System.setOut(new PrintStream(byteArrayOutputStream));
        try {
            // Execute plugin
            IPluginExecutable executable = getExecutable(
                ConsoleLoggerPluginExecutable.class, LoggingAspect.class);
            assertEquals("EventBus must be empty", 0, eventBus.size());
            executable.execute();
            assertEquals("EventBus must exactly contain 2 events", 2,
                eventBus.size());

            // Verify that the log output contains the class name of the
            // executed plugin
            String output = byteArrayOutputStream.toString();
            assertTrue(String.format("Log output must contain %s\n\tbut was%s",
                    ConsoleLoggerPluginExecutable.class.getName(), output),
                output.contains(ConsoleLoggerPluginExecutable.class
                    .getName()));
        } finally {
            // Reset System.out
            System.setOut(out);
        }
        eventBus.reset();
    }


    public void execute() {

    }
}
