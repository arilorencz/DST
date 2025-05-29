package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.*;
import dst.ass1.jpa.util.Constants;
import org.hibernate.annotations.Target;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = Constants.T_MATCH)
public class Match implements IMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private Date date;

    @Embedded
    @Target(Money.class)
    private IMoney fare;

    @ManyToOne(targetEntity = Driver.class, optional = false)
    private IDriver driver;

    @ManyToOne(targetEntity = Vehicle.class, optional = false)
    private IVehicle vehicle;

    @OneToOne(targetEntity = Trip.class, optional = false)
    private ITrip trip;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public IMoney getFare() {
        return fare;
    }

    @Override
    public void setFare(IMoney money) {
        this.fare = money;
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
    public IVehicle getVehicle() {
        return vehicle;
    }

    @Override
    public void setVehicle(IVehicle vehicle) {
        this.vehicle = vehicle;
    }

    @Override
    public IDriver getDriver() {
        return driver;
    }

    @Override
    public void setDriver(IDriver driver) {
        this.driver = driver;
    }
}