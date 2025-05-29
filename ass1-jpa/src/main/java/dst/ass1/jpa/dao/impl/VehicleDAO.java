package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.IVehicleDAO;
import dst.ass1.jpa.model.ITrip;
import dst.ass1.jpa.model.IVehicle;
import dst.ass1.jpa.model.impl.Vehicle;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

public class VehicleDAO implements IVehicleDAO {
    private EntityManager em;

    public VehicleDAO(EntityManager em) {
        this.em = em;
    }

    @Override
    public IVehicle findById(Long id) {
        return em.find(Vehicle.class, id);
    }

    @Override
    public List<IVehicle> findAll() {
        TypedQuery<IVehicle> query = em.createQuery(
                "SELECT v FROM Vehicle v", IVehicle.class);
        return query.getResultList();
    }
}
