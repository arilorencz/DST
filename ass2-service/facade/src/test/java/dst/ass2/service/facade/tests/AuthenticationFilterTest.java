package dst.ass2.service.facade.tests;

import dst.ass2.service.auth.client.IAuthenticationClient;
import dst.ass2.service.facade.ServiceFacadeApplication;
import dst.ass2.service.facade.filter.IAuthenticationFilter;
import dst.ass2.service.facade.grading.GitHubClassroomGrading;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ServiceFacadeApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("AuthenticationResourceTest")
public class AuthenticationFilterTest implements ApplicationContextAware {


    private ApplicationContext ctx;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 5)
    public void testAuthenticationFilter() {
        IAuthenticationFilter filter = ctx.getBean(IAuthenticationFilter.class);
        assertNotNull(filter);
        assertNotNull(filter.getClass().getAnnotation(Provider.class));

        Class<?>[] interfaces = filter.getClass().getInterfaces();
        boolean foundContainerRequestFilter = false;
        for (int i = 0; i < interfaces.length; i++) {
            if (interfaces[i].equals(ContainerRequestFilter.class)) {
                foundContainerRequestFilter = true;
            }
        }
        assertTrue("Class does not implement ContainerRequestFilter", foundContainerRequestFilter);
    }

    @Test(timeout = 10000)
    @GitHubClassroomGrading(maxScore = 5)
    public void testFilterWithValidToken() throws IOException {
        IAuthenticationFilter filter = ctx.getBean(IAuthenticationFilter.class);

        IAuthenticationClient clientMock = mock(IAuthenticationClient.class);

        when(clientMock.isTokenValid("valid-token")).thenReturn(true);

        filter.setAuthClient(clientMock);

        // Create a real request context with a valid token
        ContainerRequestContext context = new TestContainerRequestContext();
        context.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer valid-token");

        // Call the filter method
        filter.filter(context);

        // If no exception is thrown and the request is not aborted, the test passes
        assertNull(
            "Request should not be aborted with valid token", ((TestContainerRequestContext) context).getAbortResponse());
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 5)
    public void testFilterWithInvalidToken() throws IOException {
        IAuthenticationFilter filter = ctx.getBean(IAuthenticationFilter.class);

        IAuthenticationClient clientMock = mock(IAuthenticationClient.class);

        when(clientMock.isTokenValid("invalid-token")).thenReturn(false);

        filter.setAuthClient(clientMock);

        // Create a real request context with a valid token
        ContainerRequestContext context = new TestContainerRequestContext();
        context.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");

        // Call the filter method
        filter.filter(context);

        // Verify the request was aborted with 401 Unauthorized
        Response response = ((TestContainerRequestContext) context).getAbortResponse();

        assertEquals("Response status should be 401 Unauthorized", Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 5)
    public void testFilterWithMissingAuthorizationHeader() throws IOException {
        IAuthenticationFilter filter = ctx.getBean(IAuthenticationFilter.class);

        // Create a request context without an Authorization header
        ContainerRequestContext context = new TestContainerRequestContext();

        // Call the filter method
        filter.filter(context);

        // Verify the request was aborted with 401 Unauthorized
        Response response = ((TestContainerRequestContext) context).getAbortResponse();
        assertNotNull("Request should be aborted when Authorization header is missing", response);
        assertEquals("Response status should be 401 Unauthorized", Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    // A simple implementation of ContainerRequestContext for testing
    private static class TestContainerRequestContext implements ContainerRequestContext {
        private final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        private Response abortResponse;

        @Override
        public MultivaluedMap<String, String> getHeaders() {
            return headers;
        }

        @Override
        public String getHeaderString(String name) {
            List<String> values = headers.get(name);
            return values != null && !values.isEmpty() ? values.get(0) : null;
        }

        @Override
        public void abortWith(Response response) {
            this.abortResponse = response;
        }

        public Response getAbortResponse() {
            return abortResponse;
        }

        // Implement other required methods with default implementations
        @Override
        public Object getProperty(String name) {
            return null;
        }

        @Override
        public void setProperty(String name, Object object) {
        }

        @Override
        public void removeProperty(String name) {
        }

        @Override
        public UriInfo getUriInfo() {
            return null;
        }

        @Override
        public void setRequestUri(URI requestUri) {
        }

        @Override
        public void setRequestUri(URI baseUri, URI requestUri) {
        }

        @Override
        public Request getRequest() {
            return null;
        }

        @Override
        public String getMethod() {
            return "GET";
        }

        @Override
        public void setMethod(String method) {
        }

        @Override
        public java.io.InputStream getEntityStream() {
            return null;
        }

        @Override
        public void setEntityStream(java.io.InputStream input) {
        }

        @Override
        public SecurityContext getSecurityContext() {
            return null;
        }

        @Override
        public void setSecurityContext(SecurityContext context) {
        }

        @Override
        public Collection<String> getPropertyNames() {
            return List.of();
        }

        @Override
        public Date getDate() {
            return null;
        }

        @Override
        public Locale getLanguage() {
            return null;
        }

        @Override
        public int getLength() {
            return 0;
        }

        @Override
        public MediaType getMediaType() {
            return null;
        }

        @Override
        public List<MediaType> getAcceptableMediaTypes() {
            return List.of();
        }

        @Override
        public List<Locale> getAcceptableLanguages() {
            return List.of();
        }

        @Override
        public Map<String, Cookie> getCookies() {
            return Map.of();
        }

        @Override
        public boolean hasEntity() {
            return false;
        }
    }
}
