package dst.ass2.aop.logging;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import java.lang.reflect.Field;
import java.util.logging.Logger;

@Aspect
public class LoggingAspect {

    @Pointcut("execution(* dst.ass2.aop.IPluginExecutable.execute(..)) && !@annotation(dst.ass2.aop.logging.Invisible)")
    public void pluginExecution() {}

    @Before("pluginExecution()")
    public void logBeforeExecution(JoinPoint joinPoint) {
        Object target = joinPoint.getTarget();
        Logger logger = extractLogger(target);
        String className = target.getClass().getName();
        String message = "Plugin " + className + " started to execute";
        if (logger != null) {
            logger.info(message);
        } else {
            System.out.println("[java] " + message);
        }
    }

    @After("pluginExecution()")
    public void logAfterExecution(JoinPoint joinPoint) {
        Object target = joinPoint.getTarget();
        Logger logger = extractLogger(target);
        String className = target.getClass().getName();
        String message = "Plugin " + className + " is finished";
        if (logger != null) {
            logger.info(message);
        } else {
            System.out.println("[java] " + message);
        }
    }

    private Logger extractLogger(Object pluginInstance) {
        try {
            for (Field field : pluginInstance.getClass().getDeclaredFields()) {
                if (Logger.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return (Logger) field.get(pluginInstance);
                }
            }
        } catch (IllegalAccessException e) {
            // Fail silently and fall back to System.out
        }
        return null;
    }
}
