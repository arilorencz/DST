package dst.ass1.jpa.tests;

import dst.ass1.grading.GitHubClassroomGrading;
import dst.ass1.grading.LocalGradingClassRule;
import dst.ass1.grading.LocalGradingRule;
import dst.ass1.jpa.dao.ITripDAO;
import dst.ass1.jpa.model.ITrip;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Ass1_2_3Test extends Ass1_TestBase {
    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();

    private ITripDAO tripDAO;

    @Before
    public void setUp() throws Exception {
        tripDAO = daoFactory.createTripDAO();
    }

    @GitHubClassroomGrading(maxScore = 5)
    @Test(timeout = 2000)
    public void testFindTripsWithCriteria_AllParameters() {
        BigDecimal minFare = new BigDecimal("20.0");
        BigDecimal maxFare = new BigDecimal("350.0");
        Double minDriverRating = 2.0;
        Long minStops = 2L;

        List<ITrip> results = tripDAO.findTripsWithCriteria(minFare, maxFare, minDriverRating, minStops);

        assertEquals(1, results.size());
        ITrip trip = results.get(0);
        assertEquals(testData.trip1Id, trip.getId());
        assertEquals(3, trip.getStops().size());
        assertTrue(trip.getMatch().getDriver().getAvgRating() > minDriverRating);
        assertTrue(trip.getTripInfo().getReceipt().getTotal().getCurrencyValue().compareTo(minFare) > 0);
        assertTrue(trip.getTripInfo().getReceipt().getTotal().getCurrencyValue().compareTo(maxFare) < 0);
    }

    @GitHubClassroomGrading(maxScore = 5)
    @Test(timeout = 2000)
    public void testFindTripsWithCriteria_OnlyFareRange() {
        BigDecimal minFare = new BigDecimal("20.0");
        BigDecimal maxFare = new BigDecimal("30.0");

        List<ITrip> results = tripDAO.findTripsWithCriteria(minFare, maxFare, null, null);

        assertEquals(3, results.size()); // Should find trips 4, 5, and 17
        for (ITrip trip : results) {
            BigDecimal fare = trip.getTripInfo().getReceipt().getTotal().getCurrencyValue();
            assertTrue(fare.compareTo(minFare) >= 0);
            assertTrue(fare.compareTo(maxFare) <= 0);
        }
    }

    @GitHubClassroomGrading(maxScore = 5)
    @Test(timeout = 2000)
    public void testFindTripsWithCriteria_OnlyMinStops() {
        Long minStops = 2L;

        List<ITrip> results = tripDAO.findTripsWithCriteria(null, null, null, minStops);

        assertEquals(2, results.size()); // Should find trips 1 and 6
        for (ITrip trip : results) {
            assertTrue(trip.getStops().size() >= minStops);
        }
    }

    @GitHubClassroomGrading(maxScore = 5)
    @Test(timeout = 2000)
    public void testFindTripsWithCriteria_NoResults() {
        BigDecimal minFare = new BigDecimal("1000.0"); // No trips have such high fares

        List<ITrip> results = tripDAO.findTripsWithCriteria(minFare, null, null, null);

        assertTrue(results.isEmpty());
    }

}
