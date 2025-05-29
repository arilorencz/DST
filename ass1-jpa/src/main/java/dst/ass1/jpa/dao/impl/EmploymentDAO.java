package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.IEmploymentDAO;
import dst.ass1.jpa.model.IEmployment;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

public class EmploymentDAO implements IEmploymentDAO {
    private EntityManager em;

    public EmploymentDAO(EntityManager em) {
        this.em = em;
    }

    @Override
    public IEmployment findById(Long id) {
        return em.find(IEmployment.class, id);
    }

    @Override
    public List<IEmployment> findAll() {
        TypedQuery<IEmployment> query = em.createQuery(
                "SELECT e FROM Employment e", IEmployment.class);
        return query.getResultList();
    }
}
