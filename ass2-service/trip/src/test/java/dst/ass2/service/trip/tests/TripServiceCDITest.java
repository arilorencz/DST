package dst.ass2.service.trip.tests;

import dst.ass1.grading.GitHubClassroomGrading;
import dst.ass2.service.api.trip.ITripService;
import dst.ass2.service.trip.TripApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.ManagedBean;
import javax.inject.Named;
import javax.inject.Singleton;
import java.lang.reflect.Field;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.hibernate.validator.internal.util.Contracts.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TripApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TripServiceCDITest implements ApplicationContextAware {
    private static final Logger LOG = LoggerFactory.getLogger(TripServiceCDITest.class);

    private ITripService tripService;
    private ApplicationContext ctx;

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.ctx = ctx;
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 5)
    public void testServiceIsExposedAsCDIService() {
        // Verify it has the ManagedBean annotation
        Class<?> clazz = AopProxyUtils.ultimateTargetClass(tripService);
        boolean cdiExposed = clazz.isAnnotationPresent(ManagedBean.class) ||
            clazz.isAnnotationPresent(Singleton.class) || clazz.isAnnotationPresent(Named.class);
        assertTrue(cdiExposed,
            "TripService should be managed");
    }

    @Before
    public void setUp() {
        LOG.info("Test resolving beans from application context");
        tripService = ctx.getBean(ITripService.class);
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 5)
    public void testDependenciesInjectedViaCDI() {
        // Check if dependencies are injected via CDI
        assertNotNull(tripService.getEntityManager(),
            "EntityManager should be injected");
        assertNotNull(tripService.getMatchingService(),
            "IMatchingService should be injected");
    }

}
