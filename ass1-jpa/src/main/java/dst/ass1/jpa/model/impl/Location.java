package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.IDriver;
import dst.ass1.jpa.model.ILocation;
import dst.ass1.jpa.util.Constants;

import javax.persistence.*;

@Entity
@Table(name = Constants.T_LOCATION)
public class Location implements ILocation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private String name;

    @Column
    private Long locationId;


    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    @Override
    public String getName() { return name; }

    @Override
    public void setName(String name) { this.name = name; }

    @Override
    public Long getLocationId() {
        return locationId;
    }

    @Override
    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }
}