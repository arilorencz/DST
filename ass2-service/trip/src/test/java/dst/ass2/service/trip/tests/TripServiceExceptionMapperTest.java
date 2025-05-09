package dst.ass2.service.trip.tests;

import dst.ass1.grading.GitHubClassroomGrading;
import dst.ass2.service.api.trip.DriverNotAvailableException;
import dst.ass2.service.api.trip.EntityNotFoundException;
import dst.ass2.service.api.trip.InvalidTripException;
import dst.ass2.service.trip.TripApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.junit4.SpringRunner;

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TripApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TripServiceExceptionMapperTest implements ApplicationContextAware {


    private ApplicationContext ctx;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }


    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 2)
    public void testInvalidTripExceptionMapperIsRegistered() {
        // Get all beans of type ExceptionMapper from the application context
        Class<? extends Exception> exceptionClass = InvalidTripException.class;
        boolean hasMapper = hasExceptionMapper(exceptionClass);

        assertTrue("No ExceptionMapper registered for InvalidTripException", hasMapper);
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 2)
    public void testIllegalStateExceptionMapperIsRegistered() {
        // Get all beans of type ExceptionMapper from the application context
        Class<? extends Exception> exceptionClass = IllegalStateException.class;
        boolean hasMapper = hasExceptionMapper(exceptionClass);

        assertTrue("No ExceptionMapper registered for IllegalStateException", hasMapper);
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 2)
    public void testEntityNotFoundExceptionMapperIsRegistered() {
        // Get all beans of type ExceptionMapper from the application context
        Class<? extends Exception> exceptionClass = EntityNotFoundException.class;
        boolean hasMapper = hasExceptionMapper(exceptionClass);

        assertTrue("No ExceptionMapper registered for EntityNotFoundException", hasMapper);
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 2)
    public void testDriverNotAvailableExceptionMapperIsRegistered() {
        // Get all beans of type ExceptionMapper from the application context
        Class<? extends Exception> exceptionClass = DriverNotAvailableException.class;
        boolean hasMapper = hasExceptionMapper(exceptionClass);

        assertTrue("No ExceptionMapper registered for DriverNotAvailableException", hasMapper);
    }

    private boolean hasExceptionMapper(Class<? extends Exception> obj) {
        Map<String, ExceptionMapper> mappers = ctx.getBeansOfType(ExceptionMapper.class);
        boolean hasMapper = mappers.values().stream()
                .anyMatch(mapper -> {
                    // Use reflection to check the generic type parameter
                    Type[] genericInterfaces = mapper.getClass().getGenericInterfaces();
                    if (mapper.getClass().getAnnotation(Provider.class) == null) {
                        return false;
                    }
                    for (Type genericInterface : genericInterfaces) {
                        if (genericInterface instanceof ParameterizedType) {
                            ParameterizedType pt = (ParameterizedType) genericInterface;
                            if (pt.getRawType().equals(ExceptionMapper.class)) {
                                Type[] typeArguments = pt.getActualTypeArguments();

                                if (typeArguments.length > 0 &&
                                        typeArguments[0].equals(obj)) {
                                    return true;
                                }
                            }
                        }
                    }
                    return false;
                });
        return hasMapper;
    }
}
