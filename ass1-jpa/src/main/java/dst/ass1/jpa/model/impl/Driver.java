package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.IDriver;
import dst.ass1.jpa.model.IEmployment;
import dst.ass1.jpa.model.IVehicle;
import dst.ass1.jpa.util.Constants;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Entity
public class Driver extends PlatformUser implements IDriver {

    @OneToMany(targetEntity = Employment.class)
    private Collection<IEmployment> employments = new ArrayList<>();

    @ManyToOne(targetEntity = Vehicle.class,optional = false)
    private IVehicle vehicle;

    @Override
    public Collection<IEmployment> getEmployments() {
        return employments;
    }

    @Override
    public void setEmployments(Collection<IEmployment> employments) {
        this.employments = employments;
    }

    @Override
    public void addEmployment(IEmployment employment) {
        if (employment == null) {
            return;
        }
        this.employments.add(employment);
    }

    @Override
    public IVehicle getVehicle() {
        return vehicle;
    }

    @Override
    public void setVehicle(IVehicle vehicle) {
        this.vehicle = vehicle;
    }
}
