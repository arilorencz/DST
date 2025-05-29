package dst.ass3.elastic.impl;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import dst.ass3.elastic.ContainerException;
import dst.ass3.elastic.ContainerInfo;
import dst.ass3.elastic.ContainerNotFoundException;
import dst.ass3.elastic.IContainerService;
import dst.ass3.messaging.Region;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ContainerService implements IContainerService {
    private static final String IMAGE_NAME = "dst/ass3-worker";
    private static final String NETWORK_NAME = "dst";
    private static final String DOCKER_HOST = "tcp://127.0.0.1:2375";
    private final DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(DOCKER_HOST)
            .build();

    @Override
    public List<ContainerInfo> listContainers() throws ContainerException {
        try (var docker = createClient()) {
            return docker.listContainersCmd().exec()
                    .stream()
                    .filter(c -> c.getImage().equals("dst/ass3-worker"))
                    .map(c -> {
                        var info = new ContainerInfo();
                        info.setContainerId(c.getId());
                        info.setImage(c.getImage());
                        info.setRunning(true);

                        try {
                            String[] parts = c.getCommand().split(" ");
                            if (parts.length >= 3) {
                                info.setWorkerRegion(Region.valueOf(parts[2].toUpperCase()));
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to parse region from command: " + c.getCommand());
                        }

                        return info;
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new ContainerException("Unable to connect to the Docker instance", e);
        }
    }

    @Override
    public void stopContainer(String containerId) throws ContainerException {
        try (var docker = createClient()) {
            docker.stopContainerCmd(containerId).exec();
        } catch (IOException e) {
            throw new ContainerException("Unable to connect to the Docker instance", e);
        } catch (NotFoundException e) {
            throw new ContainerNotFoundException("Container not found: " + containerId);
        }
    }

    @Override
    public ContainerInfo startWorker(Region region) throws ContainerException {
        try (var docker = createClient()) {
            var container = docker.createContainerCmd("dst/ass3-worker")
                    .withHostConfig(HostConfig.newHostConfig()
                            .withAutoRemove(true)
                            .withNetworkMode("dst"))
                    .withCmd("python3", "worker.py", region.toString().toLowerCase())
                    .exec();

            docker.startContainerCmd(container.getId()).exec();

            var info = new ContainerInfo();
            info.setContainerId(container.getId());
            info.setImage("dst/ass3-worker");
            info.setWorkerRegion(region);

            try {
                var inspect = docker.inspectContainerCmd(container.getId()).exec();
                info.setRunning(inspect.getState().getRunning());
            } catch (Exception e) {
                info.setRunning(false);
            }

            return info;

        } catch (IOException e) {
            throw new ContainerException("Unable to connect to the Docker instance", e);
        } catch (NotFoundException e) {
            throw new ContainerNotFoundException("Image not found: dst/ass3-worker");
        }
    }

    @Override
    public DockerClient createClient() {
        return DockerClientBuilder.getInstance(config).build();
    }
}
