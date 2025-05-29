package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.IRiderDAO;
import dst.ass1.jpa.model.IRider;
import dst.ass1.jpa.model.impl.Rider;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

public class RiderDAO implements IRiderDAO {
    private EntityManager em;

    public RiderDAO(EntityManager em) {
        this.em = em;
    }

    @Override
    public IRider findById(Long id) {
        return em.find(Rider.class, id);
    }

    @Override
    public List<IRider> findAll() {
        TypedQuery<IRider> query = em.createQuery(
                "SELECT r FROM Rider r", IRider.class);
        return query.getResultList();
    }

    @Override
    public IRider findByEmail(String email) {
        return em.createNamedQuery("riderByEmail", Rider.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }
}
