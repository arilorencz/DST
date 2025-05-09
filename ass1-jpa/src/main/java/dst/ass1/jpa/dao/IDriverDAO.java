package dst.ass1.jpa.dao;

import dst.ass1.jpa.model.IDriver;

import java.util.Date;
import java.util.List;

public interface IDriverDAO extends GenericDAO<IDriver> {

    /**
     * Finds all drivers who have an average rating above the specified threshold
     * and are currently active in any organization.
     *
     * @param minRating The minimum average rating threshold (must be above 0)
     * @return List of drivers meeting the criteria, sorted by average rating descending
     * @throws IllegalArgumentException if minRating is below 0
     */
    List<IDriver> findActiveHighlyRatedDrivers(double minRating);

    /**
     * Finds drivers who have achieved excellent performance metrics within a date range:
     * - Completed at least X trips with ratings above 5 and between startDate and endDate
     * - Have never received a rating below 3
     *
     * @param minTrips  minimum number of trips with rating above 5 required
     * @param startDate start date of the period (inclusive)
     * @param endDate   end date of the period (inclusive)
     * @return List of drivers meeting all criteria, ordered by average rating
     * @throws IllegalArgumentException if minTrips is less than 1 or if endDate is before startDate
     */
    List<IDriver> findTopPerformingDrivers(Long minTrips, Date startDate, Date endDate);
}
