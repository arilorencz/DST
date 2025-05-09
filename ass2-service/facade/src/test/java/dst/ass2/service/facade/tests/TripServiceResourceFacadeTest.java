package dst.ass2.service.facade.tests;

import dst.ass2.service.api.trip.EntityNotFoundException;
import dst.ass2.service.facade.grading.GitHubClassroomGrading;
import dst.ass2.service.facade.trip.ITripServiceResourceFacade;
import dst.ass2.service.api.trip.rest.ITripServiceResource;
import dst.ass2.service.facade.ServiceFacadeApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.junit4.SpringRunner;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ServiceFacadeApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ActiveProfiles("AuthenticationResourceTest")
public class TripServiceResourceFacadeTest implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Test(timeout = 10000)
    @GitHubClassroomGrading(maxScore = 10)
    public void testFacadeCorrect() throws EntityNotFoundException {
        ITripServiceResourceFacade facade = applicationContext.getBean(ITripServiceResourceFacade.class);

        assertNotNull(facade);
        Class<?> facadeClass = AopProxyUtils.ultimateTargetClass(facade);
        Provider annotation = facadeClass.getAnnotation(Provider.class);

        assertNotNull(annotation);
        assertNotNull(facade.getDelegate());

        ITripServiceResource mockResource = mock(ITripServiceResource.class);

        when(mockResource.getTrip(1L)).thenReturn(Response.ok().build());

        facade.setDelegate(mockResource);

        facade.getTrip(1L);

        verify(mockResource, times(1)).getTrip(1L);
    }
}
