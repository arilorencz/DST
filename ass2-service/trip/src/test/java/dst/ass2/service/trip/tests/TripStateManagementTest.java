package dst.ass2.service.trip.tests;

import dst.ass1.grading.GitHubClassroomGrading;
import dst.ass1.jpa.model.IModelFactory;
import dst.ass1.jpa.model.ITrip;
import dst.ass1.jpa.model.TripState;
import dst.ass2.service.api.trip.DriverNotAvailableException;
import dst.ass2.service.api.trip.EntityNotFoundException;
import dst.ass2.service.api.trip.ITripService;
import dst.ass2.service.api.trip.MatchDTO;
import dst.ass2.service.trip.TripApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.transaction.Transactional;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TripApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TripStateManagementTest implements ApplicationContextAware {

    private ITripService tripService;
    private ApplicationContext ctx;
    private IModelFactory modelFactory;

    @Before
    public void setUp() {
        modelFactory = ctx.getBean(IModelFactory.class);


        tripService = ctx.getBean(ITripService.class);
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 6)
    public void testMatchUsesLock() {
        // Create a mock EntityManager
        EntityManager mockEm = mock(EntityManager.class);

        // Set the mocked EntityManager
        tripService.setEntityManager(mockEm);

        // Setup necessary mocks for the test
        ITrip trip = modelFactory.createTrip();
        trip.setState(TripState.QUEUED);

        // Setup the EntityManager to return our mocked entities
        when(mockEm.find(eq(Object.class), eq(1L), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(trip);


        // Attempt to call the method (may throw exceptions which is fine for this test)
        try {
            MatchDTO matchDTO = new MatchDTO();
            matchDTO.setDriverId(2L);
            matchDTO.setVehicleId(3L);
            tripService.match(1L, matchDTO);
        } catch (Exception e) {
            // Ignore exceptions for this test
        }

        // Verify EntityManager.find was called with the correct parameters
        verify(mockEm).find(trip.getClass(), 1L, LockModeType.PESSIMISTIC_WRITE);
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 4)
    public void matchIsTransactional() throws NoSuchMethodException {
        // Check if the match method has the correct transaction attributes
        Class<?> clazz = AopProxyUtils.ultimateTargetClass(tripService);

        Method matchMethod = clazz.getMethod("match", Long.class, MatchDTO.class);

        // Verify method is annotated with @Transactional
        Transactional transactionalAnnotation = matchMethod.getAnnotation(Transactional.class);
        assertNotNull("match method should be annotated with @Transactional", transactionalAnnotation);

        // Verify it uses REQUIRES_NEW transaction type
        assertEquals(Transactional.TxType.REQUIRES_NEW, transactionalAnnotation.value(),
            "match method should use REQUIRES_NEW transaction type");

        // Check rollback behavior - by default Spring rolls back on RuntimeException and Error
        // Since EntityNotFoundException and DriverNotAvailableException are likely RuntimeExceptions,
        // they should trigger rollback by default

        // Verify the method throws exceptions that would trigger rollback
        Class<?>[] exceptionTypes = matchMethod.getExceptionTypes();
        List<Class<?>> exceptionList = Arrays.asList(exceptionTypes);

        assertTrue(
            "match method should declare EntityNotFoundException", exceptionList.contains(EntityNotFoundException.class));
        assertTrue(
            "match method should declare DriverNotAvailableException", exceptionList.contains(DriverNotAvailableException.class));


    }
}
