package dst.ass2.service.facade.filter;

import dst.ass2.service.auth.client.IAuthenticationClient;

import javax.ws.rs.container.ContainerRequestContext;

public interface IAuthenticationFilter {

    void filter(ContainerRequestContext requestContext);

    void setAuthClient(IAuthenticationClient client);
}
