package dst.ass2.aop.management;

import dst.ass2.aop.IPluginExecutable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Aspect
public class ManagementAspect {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    @Around("execution(* dst.ass2.aop.IPluginExecutable.execute(..)) && @annotation(dst.ass2.aop.management.Timeout)")
    public Object enforceTimeout(final ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Timeout timeoutAnnotation = method.getAnnotation(Timeout.class);
        long timeoutMillis = timeoutAnnotation.value();

        Object target = joinPoint.getTarget();

        Future<?> timeoutTask = scheduler.schedule(() -> {
            try {
                if (target instanceof IPluginExecutable) {
                    ((IPluginExecutable) target).interrupted();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, timeoutMillis, TimeUnit.MILLISECONDS);

        try {
            Object result = joinPoint.proceed();  // execute the actual plugin
            timeoutTask.cancel(false); // cancel if completed in time
            return result;
        } catch (Throwable t) {
            timeoutTask.cancel(false); // cancel in case of exception
            throw t;
        }
    }

}
