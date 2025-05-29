package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.IDriver;
import dst.ass1.jpa.model.IEmploymentKey;
import dst.ass1.jpa.model.IOrganization;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.*;

@Embeddable
public final class EmploymentKey implements IEmploymentKey, Serializable {
    @ManyToOne(targetEntity = Driver.class)
    private IDriver driver;

    @ManyToOne(targetEntity = Organization.class)
    private IOrganization organization;


    @Override
    public IDriver getDriver() {
        return driver;
    }

    @Override
    public void setDriver(IDriver driver) {
        this.driver = driver;
    }

    @Override
    public IOrganization getOrganization() {
        return organization;
    }

    @Override
    public void setOrganization(IOrganization organization) {
        this.organization = organization;
    }

    @Override
    public int hashCode() {
        return Objects.hash(driver, organization);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EmploymentKey)) {
            return false;
        }
        EmploymentKey other = (EmploymentKey) obj;
        return Objects.equals(this.driver, other.driver) &&
                Objects.equals(this.organization, other.organization);
    }

    @Override
    public String toString() {
        return "EmploymentKey{driverId=" + driver + ", organizationId=" + organization + "}";
    }
}