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

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Ass1_2_1aTest extends Ass1_TestBase {
    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();

    private IDriverDAO dao;
    private Class<? extends IDriver> entityClass;

    @Before
    public void setUp() throws Exception {
        dao = daoFactory.createDriverDAO();
        entityClass = modelFactory.createDriver().getClass();
    }

    @GitHubClassroomGrading(maxScore = 10)
    @Test(timeout = 2000)
    public void testFindActiveHighlyRatedDrivers_ValidRating() {
        double minRating = 3.5;

        List<IDriver> result = dao.findActiveHighlyRatedDrivers(minRating);

        assertEquals(2, result.size());
        assertEquals(TestData.DRIVER_4_AVG_RATING, result.get(0).getAvgRating()); // 4.09
        assertEquals(TestData.DRIVER_3_AVG_RATING, result.get(1).getAvgRating()); // 3.9
    }

    @GitHubClassroomGrading(maxScore = 10)
    @Test(timeout = 2000, expected = IllegalArgumentException.class)
    public void testFindActiveHighlyRatedDrivers_InvalidRating() {
        dao.findActiveHighlyRatedDrivers(-1); // Should throw IllegalArgumentException
    }

    @GitHubClassroomGrading(maxScore = 5)
    @Test(timeout = 2000)
    public void testFindActiveHighlyRatedDrivers_NoResults() {
        double minRating = 4.5;

        List<IDriver> result = dao.findActiveHighlyRatedDrivers(minRating);

        assertTrue(result.isEmpty());
    }


}
