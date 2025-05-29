package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.IMatchDAO;
import dst.ass1.jpa.model.IMatch;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

public class MatchDAO implements IMatchDAO {
    private EntityManager em;

    public MatchDAO(EntityManager em) {
        this.em = em;
    }

    @Override
    public IMatch findById(Long id) {
        return em.find(IMatch.class, id);
    }

    @Override
    public List<IMatch> findAll() {
        TypedQuery<IMatch> query = em.createQuery(
                "SELECT m FROM Match m", IMatch.class);
        return query.getResultList();
    }
}
