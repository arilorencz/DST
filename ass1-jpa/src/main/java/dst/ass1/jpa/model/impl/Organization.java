package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.IEmployment;
import dst.ass1.jpa.model.IOrganization;
import dst.ass1.jpa.model.IVehicle;
import dst.ass1.jpa.util.Constants;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = Constants.T_ORGANIZATION)
@NamedQuery(
        name = Constants.Q_ORGANIZATION_BY_VEHICLE_DRIVE_RATING,
        query = "SELECT DISTINCT o FROM Organization o " +
                "JOIN o.employments e " +
                "JOIN e.id.driver d " +
                "WHERE e.active = TRUE " +
                "AND d.avgRating >= :minRating " +
                "AND d.vehicle.type = :vehicleType"
)
public class Organization implements IOrganization {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    @OneToMany(targetEntity = Employment.class)
    private Collection<IEmployment> employments = new ArrayList<>();

    @ManyToMany(targetEntity = Organization.class)
    @JoinTable(
            name = Constants.J_ORGANIZATION_PARTS,
            joinColumns = @JoinColumn(name = Constants.I_ORGANIZATION_PARTS),
            inverseJoinColumns = @JoinColumn(name = Constants.I_ORGANIZATION_PART_OF)
    )
    private Collection<IOrganization> parts = new ArrayList<>();

    @ManyToMany(targetEntity = Organization.class, mappedBy = Constants.M_ORGANIZATION_PARTS)
    private Collection<IOrganization> partOf = new ArrayList<>();

    @ManyToMany(targetEntity = Vehicle.class)
    private Collection<IVehicle> vehicles = new ArrayList<>();

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Collection<IOrganization> getParts() {
        return new ArrayList<>(parts);
    }

    @Override
    public void setParts(Collection<IOrganization> parts) {
        this.parts = parts;
    }

    @Override
    public void addPart(IOrganization part) {
        if (part == null) {
            return;
        }
        this.parts.add( part);
    }

    @Override
    public Collection<IOrganization> getPartOf() {
        return partOf;
    }

    @Override
    public void setPartOf(Collection<IOrganization> partOf) {
        this.partOf = partOf;
    }

    @Override
    public void addPartOf(IOrganization partOf) {
        if (partOf == null) {
            return;
        }
        this.partOf.add(partOf);
    }

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
    public Collection<IVehicle> getVehicles() {
        return vehicles;
    }

    @Override
    public void setVehicles(Collection<IVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    @Override
    public void addVehicle(IVehicle vehicle) {
        if (vehicle == null) {
            return;
        }
        this.vehicles.add(vehicle);
    }
}
