package dst.ass1.jpa.dao;

import dst.ass1.jpa.model.ITrip;

import java.math.BigDecimal;
import java.util.List;

public interface ITripDAO extends GenericDAO<ITrip> {

    /**
     * Finds all completed trips that have more than the specified number of stops.
     *
     * @param minStops minimum number of stops (exclusive)
     * @return List of trips matching the criteria, ordered by number of stops descending
     * @throws IllegalArgumentException if minStops is negative
     */
    List<ITrip> findCompletedTripsWithMinStops(int minStops);

    /**
     * Finds trips matching multiple optional criteria.
     * You can ignore the currency and just filter by value.
     *
     * @param minFare         minimum total fare (optional)
     * @param maxFare         maximum total fare (optional)
     * @param minDriverRating minimum driver rating (optional)
     * @param minStops        minimum number of stops (optional)
     * @return List of trips matching the criteria, ordered by creation date descending
     */
    List<ITrip> findTripsWithCriteria(BigDecimal minFare, BigDecimal maxFare,
                                      Double minDriverRating, Long minStops);
}
