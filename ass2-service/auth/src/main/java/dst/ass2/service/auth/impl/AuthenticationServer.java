package dst.ass2.service.auth.impl;

import dst.ass2.service.api.auth.proto.*;
import dst.ass2.service.api.auth.AuthenticationException;
import dst.ass2.service.api.auth.IAuthenticationService;
import dst.ass2.service.api.auth.NoSuchUserException;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import javax.annotation.ManagedBean;
import javax.inject.Inject;

@ManagedBean
public class AuthenticationServer extends AuthServiceGrpc.AuthServiceImplBase {
    @Inject
    IAuthenticationService authService;

    @Override
    public void authenticate(AuthenticationRequest request,
                             StreamObserver<AuthenticationResponse> responseObserver) {
        try {
            String token = authService.authenticate(request.getEmail(), request.getPassword());
            AuthenticationResponse response = AuthenticationResponse.newBuilder()
                    .setToken(token)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NoSuchUserException e) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(e.getMessage())
                            .asRuntimeException());
        } catch (AuthenticationException e) {
            responseObserver.onError(
                    Status.UNAUTHENTICATED
                            .withDescription(e.getMessage())
                            .asRuntimeException());
        }
    }

    @Override
    public void validateToken(TokenValidationRequest request,
                              StreamObserver<TokenValidationResponse> responseObserver) {
        boolean valid = authService.isValid(request.getToken());
        TokenValidationResponse response = TokenValidationResponse.newBuilder()
                .setValid(valid)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
