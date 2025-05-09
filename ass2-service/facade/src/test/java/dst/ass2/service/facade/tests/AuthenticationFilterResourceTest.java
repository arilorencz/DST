package dst.ass2.service.facade.tests;

import dst.ass2.service.api.trip.TripDTO;
import dst.ass2.service.facade.ServiceFacadeApplication;
import dst.ass2.service.facade.ServiceFacadeApplicationConfig;
import dst.ass2.service.facade.grading.GitHubClassroomGrading;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ServiceFacadeApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("AuthenticationResourceTest")
public class AuthenticationFilterResourceTest {

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;
    private HttpHeaders headers;


    @Before
    public void setUp() {
        headers = new HttpHeaders();
        restTemplate = new RestTemplate();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        BufferingClientHttpRequestFactory bufferingClientHttpRequestFactory = new BufferingClientHttpRequestFactory(requestFactory);
        requestFactory.setOutputStreaming(false);
        restTemplate.setRequestFactory(bufferingClientHttpRequestFactory);
    }


    @Test(timeout = 10000)
    @GitHubClassroomGrading(maxScore = 10)
    public void testAuthentication_InterceptsTripCalls() {
        Long trip = 1L;
        String url = url("/trips/" + trip);

        try {
            restTemplate.getForEntity(url, TripDTO.class);
            assertTrue(false);
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
        }

        try {
            headers = new HttpHeaders();
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + ServiceFacadeApplicationConfig.MockAuthenticationClient.TOKEN);
            HttpEntity<?> request = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.GET, request, TripDTO.class, headers);
            assertTrue(false);
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode(), not(HttpStatus.UNAUTHORIZED));
        } catch (HttpServerErrorException.InternalServerError e) {
            assertThat(e.getStatusCode(), not(HttpStatus.UNAUTHORIZED));
        }
    }


    private String url(String uri) {
        return "http://localhost:" + port + uri;
    }

}
