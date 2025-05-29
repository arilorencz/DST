package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.ITripDAO;
import dst.ass1.jpa.model.ILocation;
import dst.ass1.jpa.model.ITrip;
import dst.ass1.jpa.model.TripState;
import dst.ass1.jpa.model.impl.Trip;

import javax.persistence.EntityManager;
import javax.persistence.JoinColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TripDAO implements ITripDAO {
    private EntityManager em;

    public TripDAO(EntityManager em) {
        this.em = em;
    }

    @Override
    public ITrip findById(Long id) {
        return em.find(Trip.class, id);
    }

    @Override
    public List<ITrip> findAll() {
        TypedQuery<ITrip> query = em.createQuery(
                "SELECT t FROM Trip t", ITrip.class);
        return query.getResultList();
    }

    @Override
    public List<ITrip> findCompletedTripsWithMinStops(int minStops) {
        if (minStops < 0) {
            throw new IllegalArgumentException("minStops must be >= 0");
        }

        return new ArrayList<>(em.createNamedQuery("findCompletedWithMinStops", Trip.class)
                .setParameter("minStops", minStops)
                .getResultList()
        );
    }

    @Override
    public List<ITrip> findTripsWithCriteria(BigDecimal minFare, BigDecimal maxFare, Double minDriverRating, Long minStops) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Trip> cq = cb.createQuery(Trip.class);

        Root<Trip> trip = cq.from(Trip.class);

        // joins and Path
        Join<Object, Object> match = trip.join("match", JoinType.LEFT);
        Join<Object, Object> driver = match.join("driver", JoinType.LEFT);
        Join<Object, Object> tripInfo = trip.join("tripInfo", JoinType.LEFT);
        Join<Object, Object> receipt = tripInfo.join("tripReceipt", JoinType.LEFT);


        // list of predicates
        List<Predicate> predicates = new ArrayList<>();

        if (minFare != null) {
            predicates.add(cb.greaterThanOrEqualTo(receipt.get("total").get("currencyValue"), minFare));
        }
        if (maxFare != null) {
            predicates.add(cb.lessThanOrEqualTo(receipt.get("total").get("currencyValue"), maxFare));
        }

        if (minDriverRating != null) {
            predicates.add(cb.greaterThan(driver.get("avgRating"), minDriverRating));
        }

        if (minStops != null) {
            predicates.add(cb.ge(cb.size(trip.get("stops")), minStops.intValue()));
        }

        // final query
        Predicate[] arr = new Predicate[predicates.size()];

        cq.select(trip)
                .where(cb.and(predicates.toArray(arr)))
                .orderBy(cb.desc(trip.get("created")));

        return new ArrayList<>(em.createQuery(cq).getResultList());
    }
}
