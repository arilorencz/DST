package dst.ass1.jpa.tests;

import dst.ass1.grading.GitHubClassroomGrading;
import dst.ass1.grading.LocalGradingClassRule;
import dst.ass1.grading.LocalGradingRule;
import dst.ass1.jpa.dao.IDriverDAO;
import dst.ass1.jpa.model.IDriver;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Ass1_2_2Test extends Ass1_TestBase {
    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();

    private IDriverDAO driverDAO;

    @Before
    public void setup() {
        driverDAO = daoFactory.createDriverDAO();
    }

    @GitHubClassroomGrading(maxScore = 10)
    @Test(timeout = 2000)
    public void testFindTopPerformingDrivers_Success() {
        Long minTrips = 1L;
        LocalDate now = LocalDate.now();
        Date startDate = TestData.localDateToDate(now.minusDays(31));
        Date endDate = TestData.localDateToDate(now);

        List<IDriver> results = driverDAO.findTopPerformingDrivers(minTrips, startDate, endDate);

        assertEquals(1, results.size());
        IDriver topDriver = results.get(0);
        assertEquals(TestData.DRIVER_3_NAME, topDriver.getName());
    }

    @GitHubClassroomGrading(maxScore = 10)
    @Test(timeout = 2000)
    public void testFindTopPerformingDrivers_NoResults() {
        Long minTrips = 1L;
        LocalDate now = LocalDate.now();
        Date startDate = TestData.localDateToDate(now.minusYears(1));
        Date endDate = TestData.localDateToDate(now.minusMonths(6));

        List<IDriver> results = driverDAO.findTopPerformingDrivers(minTrips, startDate, endDate);

        assertTrue(results.isEmpty());
    }

    @GitHubClassroomGrading(maxScore = 5)
    @Test(timeout = 2000, expected = IllegalArgumentException.class)
    public void testFindTopPerformingDrivers_InvalidDateRange() {
        Long minTrips = 1L;
        LocalDate now = LocalDate.now();
        Date startDate = TestData.localDateToDate(now);
        Date endDate = TestData.localDateToDate(now.minusDays(1));

        driverDAO.findTopPerformingDrivers(minTrips, startDate, endDate); // Should throw IllegalArgumentException
    }


}
