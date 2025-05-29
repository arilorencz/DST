package dst.ass2.service.auth.client.impl;

import dst.ass2.service.api.auth.AuthenticationException;
import dst.ass2.service.api.auth.NoSuchUserException;
import dst.ass2.service.api.auth.proto.*;
import dst.ass2.service.auth.client.AuthenticationClientProperties;
import dst.ass2.service.auth.client.IAuthenticationClient;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class GrpcAuthenticationClient implements IAuthenticationClient {

    private final ManagedChannel channel;
    private final AuthServiceGrpc.AuthServiceBlockingStub blockingStub;

    public GrpcAuthenticationClient(AuthenticationClientProperties properties) {
        this.channel = ManagedChannelBuilder
                .forAddress(properties.getHost(), properties.getPort())
                .usePlaintext()
                .build();
        this.blockingStub = AuthServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public String authenticate(String email, String password) throws NoSuchUserException, AuthenticationException {
        AuthenticationRequest request = AuthenticationRequest.newBuilder()
                .setEmail(email)
                .setPassword(password)
                .build();

        try {
            AuthenticationResponse response = blockingStub.authenticate(request);
            return response.getToken();
        } catch (StatusRuntimeException e) {
            Status.Code code = e.getStatus().getCode();
            String msg = e.getStatus().getDescription();

            if (code == Status.Code.NOT_FOUND) {
                throw new NoSuchUserException(msg);
            } else if (code == Status.Code.UNAUTHENTICATED) {
                throw new AuthenticationException(msg != null ? msg : "Authentication failed");
            } else {
                throw new AuthenticationException("Unexpected error: " + code);
            }
        }
    }

    @Override
    public boolean isTokenValid(String token) {
        TokenValidationRequest request = TokenValidationRequest.newBuilder()
                .setToken(token)
                .build();
        try {
            TokenValidationResponse response = blockingStub.validateToken(request);
            return response.getValid();
        } catch (StatusRuntimeException e) {
            return false;
        }
    }

    @Override
    public void close() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdownNow();
        }
    }
}
