package dst.ass2.service.facade.auth;

import dst.ass2.service.api.auth.rest.IAuthenticationResource;
import dst.ass2.service.auth.client.IAuthenticationClient;

public interface IAuthenticationResourceFacade extends IAuthenticationResource {

    /**
     * The delegate of the IAuthenticationResourceFacade is the IAuthenticationClient
     * instance that the facade uses to forward requests.
     * @return the client
     */
    IAuthenticationClient getDelegate();

    void setDelegate(IAuthenticationClient delegate);
}
