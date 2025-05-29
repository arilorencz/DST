package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.IEmployment;
import dst.ass1.jpa.model.IEmploymentKey;
import dst.ass1.jpa.util.Constants;
import org.hibernate.annotations.Target;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = Constants.T_EMPLOYMENT)
@NamedQuery(
    name = Constants.Q_DRIVER_HIGHLY_RATED_ACTIVE,
    query = "SELECT DISTINCT d FROM Employment e JOIN e.id.driver d WHERE d.avgRating > :rating AND e.active = TRUE ORDER BY d.avgRating DESC"
)
public class Employment implements IEmployment {

    @EmbeddedId
    @Target(EmploymentKey.class)
    private IEmploymentKey id;

    private Date since;

    private boolean active;

    @Override
    public IEmploymentKey getId() {
        return id;
    }

    @Override
    public void setId(IEmploymentKey employmentKey) {
        this.id =employmentKey;
    }

    @Override
    public Date getSince() {
        return since;
    }

    @Override
    public void setSince(Date since) {
        this.since = since;
    }

    @Override
    public Boolean isActive() {
        return active;
    }

    @Override
    public void setActive(Boolean active) {
        this.active = active;
    }
}