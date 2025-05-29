package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.*;
import dst.ass1.jpa.util.Constants;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

public class Trip implements ITrip {

    private Long id;

    private Date created;

    private Date updated;

    private TripState state;

    private ILocation pickup;

    private ILocation destination;

    private Collection<ILocation> stops = new ArrayList<>();

    private ITripInfo tripInfo;

    private IMatch match;

    private IRider rider;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public Date getUpdated() {
        return updated;
    }

    @Override
    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    @Override
    public TripState getState() {
        return state;
    }

    @Override
    public void setState(TripState state) {
        this.state = state;
    }

    @Override
    public ILocation getPickup() {
        return pickup;
    }

    @Override
    public void setPickup(ILocation pickup) {
        this.pickup = (Location) pickup;
    }

    @Override
    public ILocation getDestination() {
        return destination;
    }

    @Override
    public void setDestination(ILocation destination) {
        this.destination = (Location) destination;
    }

    @Override
    public Collection<ILocation> getStops() {
        return new ArrayList<>(stops);
    }

    @Override
    public void setStops(Collection<ILocation> stops) {
        this.stops = stops;
    }

    @Override
    public void addStop(ILocation stop) {
        if (stop == null) {
            return;
        }
        this.stops.add(stop);
    }

    @Override
    public ITripInfo getTripInfo() {
        return tripInfo;
    }

    @Override
    public void setTripInfo(ITripInfo tripInfo) {
        this.tripInfo = tripInfo;
    }

    @Override
    public IMatch getMatch() {
        return match;
    }

    @Override
    public void setMatch(IMatch match) {
        this.match = match;
    }

    @Override
    public IRider getRider() {
        return rider;
    }

    @Override
    public void setRider(IRider rider) {
        this.rider = rider;
    }
}
