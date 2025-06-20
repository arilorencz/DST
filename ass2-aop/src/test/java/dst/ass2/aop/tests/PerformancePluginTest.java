package dst.ass2.aop.tests;

import dst.ass2.aop.IPluginExecutable;
import dst.ass2.aop.event.Event;
import dst.ass2.aop.event.EventBus;
import dst.ass2.aop.grading.GitHubClassroomGrading;
import dst.ass2.aop.logging.LoggingAspect;
import dst.ass2.aop.management.ManagementAspect;
import dst.ass2.aop.sample.InterruptedPluginExecutable;
import dst.ass2.aop.tests.plugin.LoggerPluginExecutable;
import dst.ass2.aop.tests.plugin.TimeoutPluginExecutable;
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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static dst.ass2.aop.util.PluginUtils.*;
import static org.junit.Assert.*;

public class PerformancePluginTest {
    final EventBus eventBus = EventBus.getInstance();

    @org.junit.Before
    @org.junit.After
    public void beforeAndAfter() {
        eventBus.reset();
    }

    /**
     * Verifies that the {@link ManagementAspect} is a valid AspectJ aspect i.e., {@link Aspect @Aspect} as well as
     * {@link Around @Around} or {@link Before @Before} / {@link After @After}.
     */
    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 5)
    public void managementAspect_isValid() {
        Aspect aspect = AnnotationUtils.findAnnotation(ManagementAspect.class, Aspect.class);
        assertNotNull("ManagementAspect is not annotated with @Aspect", aspect);

        Map<Method, Around> around = PluginUtils.findMethodAnnotation(ManagementAspect.class, Around.class);
        Map<Method, Before> before = PluginUtils.findMethodAnnotation(ManagementAspect.class, Before.class);
        Map<Method, After> after = PluginUtils.findMethodAnnotation(ManagementAspect.class, After.class);

        boolean found = !around.isEmpty() || (!before.isEmpty() && !after.isEmpty());
        assertEquals("ManagementAspect does not contain methods annotated with @Around OR @Before and @After", true, found);
    }

    /**
     * Verifies that the pointcut expression of the {@link ManagementAspect}
     * does not match any method except the {@link IPluginExecutable#execute()} method.
     */
    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 5)
    public void pointcutExpression_matchesCorrectlyInterrupted() {
        IPluginExecutable executable = PluginUtils.getExecutable(InterruptedPluginExecutable.class, ManagementAspect.class);
        assertEquals("Executable must implement the Advised interface", true, executable instanceof Advised);
        Advised advised = (Advised) executable;

        PointcutAdvisor pointcutAdvisor = PluginUtils.getPointcutAdvisor(advised);
        assertNotNull("PointcutAdvisor not found because there is no pointcut or the pointcut does not match", pointcutAdvisor);

        PointcutExpressionImpl pointcutExpression = PluginUtils.getPointcutExpression(advised);
        Method interruptedMethod = ReflectionUtils.findMethod(InterruptedPluginExecutable.class, PluginUtils.EXECUTE_METHOD.getName());
        ShadowMatch shadowMatch = pointcutExpression.matchesMethodExecution(interruptedMethod);
        assertEquals("Pointcut does not match InterruptedPluginExecutable.execute()", true, shadowMatch.alwaysMatches());

        IPluginExecutable executable_log = getExecutable(
            LoggerPluginExecutable.class, LoggingAspect.class);
        Advised advised_log = (Advised) executable_log;

        String expression = getBestExpression(advised_log);
        assertTrue("Pointcut expression must include '"
                + IPluginExecutable.class.getName() + "'",
            expression.contains(IPluginExecutable.class.getName()));
        assertTrue(
            "Pointcut expression must include '" + EXECUTE_METHOD.getName()
                + "'", expression.contains(EXECUTE_METHOD.getName()));
    }

    /**
     * Tests if the {@link ManagementAspect} interrupts the plugin after the given timeout.
     */
    @Test(timeout = PluginUtils.PLUGIN_TEST_TIMEOUT)
    @GitHubClassroomGrading(maxScore = 15)
    public void managementAspect_interruptsCorrectly() {
        IPluginExecutable executable = PluginUtils.getExecutable(InterruptedPluginExecutable.class, ManagementAspect.class);
        assertEquals("EventBus must be empty", 0, eventBus.size());
        executable.execute();

        List<Event> events = eventBus.getEvents();
        assertEquals("EventBus must contain 2 events", 2, events.size());

        long duration = events.get(1).getTime() - events.get(0).getTime();
        assertTrue("Plugin was not interrupted 2 seconds after starting it", duration < 3000);
    }


    @Test(timeout = PLUGIN_TEST_TIMEOUT)
    @GitHubClassroomGrading(maxScore = 15)
    public void managementAspect_timeoutCorrectly() {
        IPluginExecutable executable = getExecutable(TimeoutPluginExecutable.class, ManagementAspect.class);
        assertEquals("EventBus must be empty", 0, eventBus.size());
        executable.execute();

        List<Event> events = eventBus.getEvents();
        assertEquals("EventBus must contain 2 events", 2, events.size());
        assertEquals("Plugin was not interrupted 2 seconds after starting it", true, events.get(1).getTime() - events.get(0).getTime() < 3000);
    }
}
