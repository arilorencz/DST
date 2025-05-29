package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.IDriverDAO;
import dst.ass1.jpa.model.IDriver;
import dst.ass1.jpa.model.impl.Driver;
import dst.ass1.jpa.model.impl.TripInfo;
import dst.ass1.jpa.util.Constants;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;


public class DriverDAO implements IDriverDAO {

    private EntityManager em;

    public DriverDAO(EntityManager em) {
        this.em = em;
    }

    @Override
    public IDriver findById(Long id) {
        return em.find(IDriver.class, id);
    }

    @Override
    public List<IDriver> findAll() {
        TypedQuery<IDriver> query = em.createQuery(
                "SELECT d FROM Driver d", IDriver.class);
        return query.getResultList();

    }

    @Override
    public List<IDriver> findActiveHighlyRatedDrivers(double minRating) {
        if (minRating < 0) {
            throw new IllegalArgumentException("Minimum rating must be >= 0");
        }

        TypedQuery<IDriver> query = em
                .createNamedQuery(Constants.Q_DRIVER_HIGHLY_RATED_ACTIVE, IDriver.class)
                .setParameter("rating", minRating);

        return query.getResultList();
    }

    @Override
    public List<IDriver> findTopPerformingDrivers(Long minTrips, Date startDate, Date endDate) {
        if (minTrips == null || minTrips < 0) {
            throw new IllegalArgumentException("minTrips must be >= 0");
        }

        if (startDate == null || endDate == null || startDate.after(endDate)) {
            throw new IllegalArgumentException("Invalid date range: startDate must be before or equal to endDate");
        }

        // all drivers with min trips in the range of start/end
        List<IDriver> highRated = em.createNamedQuery("findTripInfosWithHighRatingInDateRange", IDriver.class)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setParameter("minTrips", minTrips)
                .getResultList();

        // all drivers who have ANY low rating
        List<IDriver> lowRated = em.createNamedQuery("findDriversWithLowRatings", IDriver.class)
                .getResultList();

        // filter out drivers with low ratings
        Set<IDriver> lowRatedSet = new HashSet<>(lowRated);
        return highRated.stream()
                .filter(driver -> !lowRatedSet.contains(driver))
                .collect(Collectors.toList());
    }
}
