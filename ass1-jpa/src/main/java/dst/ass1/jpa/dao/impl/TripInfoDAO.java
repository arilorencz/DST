package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.ITripInfoDAO;
import dst.ass1.jpa.model.ITripInfo;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

public class TripInfoDAO implements ITripInfoDAO {
    private EntityManager em;

    public TripInfoDAO(EntityManager em) {
        this.em = em;
    }

    @Override
    public ITripInfo findById(Long id) {
        return em.find(ITripInfo.class, id);
    }

    @Override
    public List<ITripInfo> findAll() {
        TypedQuery<ITripInfo> query = em.createQuery(
                "SELECT ti FROM TripInfo ti", ITripInfo.class);
        return query.getResultList();
    }
}
