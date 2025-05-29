package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.ITrip;
import dst.ass1.jpa.model.ITripInfo;
import dst.ass1.jpa.model.ITripReceipt;
import dst.ass1.jpa.util.Constants;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = Constants.T_TRIP_INFO)
@NamedQuery(
        name = "findTripInfosWithHighRatingInDateRange",
        query = "SELECT ti.trip.match.driver " +
                "FROM TripInfo ti " +
                "WHERE ti.driverRating > 5 " +
                "AND ti.completed BETWEEN :startDate AND :endDate " +
                "GROUP BY ti.trip.match.driver " +
                "HAVING COUNT(ti) >= :minTrips"
)
@NamedQuery(
        name = "findDriversWithLowRatings",
        query = "SELECT DISTINCT ti.trip.match.driver " +
                "FROM TripInfo ti " +
                "WHERE ti.driverRating < 3"
)

public class TripInfo implements ITripInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private Date completed;

    @Column
    private Double distance;

    @Column
    private int driverRating;

    @Column
    private int riderRating;

    @OneToOne(targetEntity = Trip.class, optional = false)
    private ITrip trip;

    @OneToOne(targetEntity = TripReceipt.class, optional = false)
    private ITripReceipt tripReceipt;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Date getCompleted() {
        return completed;
    }

    @Override
    public void setCompleted(Date date) {
        this.completed = date;
    }

    @Override
    public Double getDistance() {
        return distance;
    }

    @Override
    public void setDistance(Double distance) {
        this.distance = distance;
    }

    @Override
    public Integer getDriverRating() {
        return driverRating;
    }

    @Override
    public void setDriverRating(Integer driverRating) {
        this.driverRating = driverRating;
    }

    @Override
    public Integer getRiderRating() {
        return riderRating;
    }

    @Override
    public void setRiderRating(Integer riderRating) {
        this.riderRating = riderRating;
    }

    @Override
    public ITrip getTrip() {
        return trip;
    }

    @Override
    public void setTrip(ITrip trip) {
        this.trip = trip;
    }

    @Override
    public ITripReceipt getReceipt() {
        return tripReceipt;
    }

    @Override
    public void setReceipt(ITripReceipt receipt) {
        this.tripReceipt = receipt;
    }
}
