package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.ILocationDAO;
import dst.ass1.jpa.model.ILocation;
import dst.ass1.jpa.model.impl.Location;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

public class LocationDAO implements ILocationDAO {
    private EntityManager em;

    public LocationDAO(EntityManager em) {
        this.em = em;
    }

    @Override
    public ILocation findById(Long id) {
        return em.find(Location.class, id);
    }

    @Override
    public List<ILocation> findAll() {
        TypedQuery<ILocation> query = em.createQuery(
                "SELECT l FROM Location l", ILocation.class);
        return query.getResultList();
    }
}
