package dst.ass3.worker.tests;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import dst.ass3.grading.GitHubClassroomGrading;
import dst.ass3.grading.LocalGradingClassRule;
import dst.ass3.grading.LocalGradingRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class DockerImageTest {
    private static final String DEFAULT_HOST = "tcp://127.0.0.1:2375";
    DefaultDockerClientConfig config = DefaultDockerClientConfig
            .createDefaultConfigBuilder()
            .withDockerHost(DEFAULT_HOST)
            .build();

    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();

    private static final String IMAGE_NAME = "dst/ass3-worker:latest";


    @Test(timeout = 20000)
    @GitHubClassroomGrading(maxScore = 5)
    public void testShouldRunPipInstallCommands() throws InterruptedException {
        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

        CreateContainerResponse container = dockerClient.createContainerCmd(IMAGE_NAME)
            .withEntrypoint("pip")
            .withCmd("freeze")
            .exec();

        dockerClient.startContainerCmd(container.getId()).exec();
        StringBuilder logs = new StringBuilder();

        // Create the callback to handle log frames
        ResultCallback.Adapter<Frame> callback = new ResultCallback.Adapter<>() {
            @Override
            public void onNext(Frame frame) {
                logs.append(new String(frame.getPayload()));
            }
        };

        // Run the log command with the callback
        LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(container.getId())
            .withStdOut(true)
            .withStdErr(true)
            .withFollowStream(true);

        logContainerCmd.exec(callback).awaitCompletion(10, TimeUnit.SECONDS);

        assertTrue("Pip does not freeze correctly", logs.toString().contains("pika"));
    }

    @Test(timeout = 20000)
    @GitHubClassroomGrading(maxScore = 5)
    public void testScriptAsEntrypoint() {
        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();
        InspectImageResponse image = dockerClient.inspectImageCmd(IMAGE_NAME).exec();

        String[] entrypoint = image.getConfig().getEntrypoint();
        boolean containsPythonCommand = false;
        boolean containsScriptName = false;

        for (String entrypointPart : entrypoint) {
            if (entrypointPart.contains("python")) {
                containsPythonCommand = true;
            }
            if (entrypointPart.contains("worker.py")) {
                containsScriptName = true;
            }
        }

        assertTrue("Entrypoint does not contain python command", containsPythonCommand);
        assertTrue("Entrypoint does not contain script name", containsScriptName);
    }

    @Test(timeout = 20000)
    @GitHubClassroomGrading(maxScore = 5)
    public void testDockerfileHasCorrectBaseImage() throws Exception {
        String dockerfilePath = "Dockerfile";
        List<String> lines = Files.readAllLines(Paths.get(dockerfilePath));
        boolean containsCorrectBaseImage = false;
        for (String line : lines) {
            if (line.contains("FROM python:3-slim")) {
                containsCorrectBaseImage = true;
                break;
            }
        }
        assertTrue("Dockerfile is not from correct base image (python:3-slim)", containsCorrectBaseImage);
    }
}
