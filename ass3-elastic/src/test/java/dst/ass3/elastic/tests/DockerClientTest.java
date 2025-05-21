package dst.ass3.elastic.tests;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListContainersCmd;
import dst.ass3.elastic.ContainerInfo;
import dst.ass3.elastic.IContainerService;
import dst.ass3.elastic.IElasticityFactory;
import dst.ass3.elastic.impl.ElasticityFactory;
import dst.ass3.grading.GitHubClassroomGrading;
import dst.ass3.grading.LocalGradingClassRule;
import dst.ass3.grading.LocalGradingRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class DockerClientTest {
    @Rule
    public Timeout timeout = new Timeout(60, TimeUnit.SECONDS);

    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();

    IElasticityFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new ElasticityFactory();
    }

    @Test(timeout = 60000)
    @GitHubClassroomGrading(maxScore = 10)
    public void testListContainersCallsDockerClient() throws Exception {
        // Arrange
        DockerClient mockClient = mock(DockerClient.class);
        ListContainersCmd mockCmd = mock(ListContainersCmd.class);
        when(mockClient.listContainersCmd()).thenReturn(mockCmd);
        when(mockCmd.exec()).thenReturn(Collections.emptyList());

        IContainerService service = spy(factory.createContainerService());
        doReturn(mockClient).when(service).createClient();

        // Act
        List<ContainerInfo> result = service.listContainers();

        // Assert
        verify(mockClient, times(1)).listContainersCmd();
        verify(mockCmd, times(1)).exec();
        assertTrue(result.isEmpty());
    }
}
