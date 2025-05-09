package dst.ass2.service.facade.trip;

import dst.ass2.service.api.trip.rest.ITripServiceResource;

public interface ITripServiceResourceFacade extends ITripServiceResource {

    ITripServiceResource getDelegate();

    void setDelegate(ITripServiceResource delegate);
}
