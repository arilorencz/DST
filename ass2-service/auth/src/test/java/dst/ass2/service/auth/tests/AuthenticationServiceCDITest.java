package dst.ass2.service.auth.tests;

import dst.ass1.grading.GitHubClassroomGrading;
import dst.ass2.service.api.auth.IAuthenticationService;
import dst.ass2.service.auth.AuthenticationServiceApplication;
import dst.ass2.service.auth.ICachingAuthenticationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.ManagedBean;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.hibernate.validator.internal.util.Contracts.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = AuthenticationServiceApplication.class)
@Transactional
@ActiveProfiles("testdata")
public class AuthenticationServiceCDITest implements ApplicationContextAware {
    @PersistenceContext
    private EntityManager em;

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationServiceCDITest.class);

    private ICachingAuthenticationService authenticationService;
    private ApplicationContext ctx;

    @Before
    public void setUp() {
        LOG.info("Test resolving beans from application context");
        authenticationService = ctx.getBean(ICachingAuthenticationService.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 5)
    public void testServiceIsExposedAndInjetedAsCDIService() {
        // Verify it has the ManagedBean annotation
        Class<?> clazz = AopProxyUtils.ultimateTargetClass(authenticationService);
        boolean cdiExposed = clazz.isAnnotationPresent(ManagedBean.class) ||
            clazz.isAnnotationPresent(Singleton.class) || clazz.isAnnotationPresent(Named.class);
        assertTrue(cdiExposed,
            "AuthenticationService should be managed");
        assertNotNull(authenticationService.getEntityManager(), "EntityManager should be injected");

    }


}
