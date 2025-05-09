package dst.ass1.jpa.tests;

import dst.ass1.grading.GitHubClassroomGrading;
import dst.ass1.grading.LocalGradingClassRule;
import dst.ass1.grading.LocalGradingRule;
import dst.ass1.jpa.dao.ITripDAO;
import dst.ass1.jpa.model.ITrip;
import dst.ass1.jpa.model.TripState;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Ass1_2_1bTest extends Ass1_TestBase {
    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();

    private ITripDAO tripDAO;

    @Before
    public void setUp() throws Exception {
        tripDAO = daoFactory.createTripDAO();
    }

    @GitHubClassroomGrading(maxScore = 10)
    @Test(timeout = 2000)
    public void testFindCompletedTripsWithMinStops_Success() {
        int minStops = 2; // Trip1 has 3 stops (location3, location4, location5)

        List<ITrip> result = tripDAO.findCompletedTripsWithMinStops(minStops);

        assertEquals(1, result.size());
        assertEquals(testData.trip1Id, result.get(0).getId());
        assertEquals(3, result.get(0).getStops().size());
        assertEquals(TripState.COMPLETED, result.get(0).getState());
    }

    @GitHubClassroomGrading(maxScore = 10)
    @Test(timeout = 2000)
    public void testFindCompletedTripsWithMinStops_NoResults() {
        int minStops = 4; // No trips have more than 4 stops

        List<ITrip> result = tripDAO.findCompletedTripsWithMinStops(minStops);

        assertTrue(result.isEmpty());
    }

    @GitHubClassroomGrading(maxScore = 5)
    @Test(expected = IllegalArgumentException.class)
    public void testFindCompletedTripsWithMinStops_InvalidInput() {
        tripDAO.findCompletedTripsWithMinStops(-1); // Should throw IllegalArgumentException
    }


}
