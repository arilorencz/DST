package dst.ass2.service.facade.tests;

import dst.ass2.service.api.auth.AuthenticationException;
import dst.ass2.service.api.auth.NoSuchUserException;
import dst.ass2.service.auth.client.IAuthenticationClient;
import dst.ass2.service.facade.ServiceFacadeApplication;
import dst.ass2.service.facade.auth.IAuthenticationResourceFacade;
import dst.ass2.service.facade.grading.GitHubClassroomGrading;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.ws.rs.ext.Provider;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ServiceFacadeApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("AuthenticationResourceTest")
public class AuthResourceFacadeTest implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 10)
    public void testFacadeCorrectAuth() throws AuthenticationException, NoSuchUserException {
        IAuthenticationResourceFacade facade = applicationContext.getBean(IAuthenticationResourceFacade.class);

        assertNotNull(facade);
        Class<?> facadeClass = AopProxyUtils.ultimateTargetClass(facade);
        Provider annotation = facadeClass.getAnnotation(Provider.class);

        assertNotNull(annotation);
        assertNotNull(facade.getDelegate());

        IAuthenticationClient mockResource = mock(IAuthenticationClient.class);

        when(mockResource.authenticate("email", "password")).thenReturn("response");

        facade.setDelegate(mockResource);

        facade.authenticate("email", "password");

        verify(mockResource, times(1)).authenticate("email", "password");


    }
}
