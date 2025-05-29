package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.IOrganizationDAO;
import dst.ass1.jpa.model.IOrganization;
import dst.ass1.jpa.model.impl.Organization;
import dst.ass1.jpa.util.Constants;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

public class OrganizationDAO implements IOrganizationDAO {
    private EntityManager em;

    public OrganizationDAO(EntityManager em) {
        this.em = em;
    }

    @Override
    public IOrganization findById(Long id) {
        return em.find(Organization.class, id);
    }

    @Override
    public List<IOrganization> findAll() {
        TypedQuery<IOrganization> query = em.createQuery(
                "SELECT o FROM Organization o", IOrganization.class);
        return query.getResultList();
    }

    @Override
    public List<IOrganization> findOrganizationsByVehicleTypeAndDriverRating(String vehicleType, double minRating) {
        if (minRating < 0) {
            throw new IllegalArgumentException("minStops must be >= 0");
        }
        return new ArrayList<>(em.createNamedQuery(Constants.Q_ORGANIZATION_BY_VEHICLE_DRIVE_RATING, Organization.class)
                .setParameter("vehicleType", vehicleType)
                .setParameter("minRating", minRating)
                .getResultList());
    }
}
