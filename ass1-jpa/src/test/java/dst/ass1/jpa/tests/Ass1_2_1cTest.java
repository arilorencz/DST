package dst.ass1.jpa.tests;

import dst.ass1.grading.GitHubClassroomGrading;
import dst.ass1.grading.LocalGradingClassRule;
import dst.ass1.grading.LocalGradingRule;
import dst.ass1.jpa.dao.IOrganizationDAO;
import dst.ass1.jpa.model.IOrganization;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Ass1_2_1cTest extends Ass1_TestBase {
    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();

    private IOrganizationDAO organizationDao;

    @Before
    public void setUp() throws Exception {
        this.organizationDao = daoFactory.createOrganizationDAO();
    }

    @GitHubClassroomGrading(maxScore = 10)
    @Test(timeout = 2000)
    public void testFindOrganizationsByVehicleTypeAndDriverRating_Success() {
        String vehicleType = TestData.TYPE_1;
        double minRating = 3.5;

        List<IOrganization> results = organizationDao.findOrganizationsByVehicleTypeAndDriverRating(vehicleType, minRating);

        assertEquals(2, results.size());
        // Organization 2 and 4, 5 have Driver4 (rating 4.09) with vehicle type1, but his employment is inactive for org 2
        assertTrue(results.stream().anyMatch(org -> org.getName().equals(TestData.ORGANIZATION_5_NAME)));
        assertTrue(results.stream().anyMatch(org -> org.getName().equals(TestData.ORGANIZATION_4_NAME)));
    }

    @GitHubClassroomGrading(maxScore = 10)
    @Test(timeout = 2000)
    public void testFindOrganizationsByVehicleTypeAndDriverRating_NoResults() {
        String vehicleType = TestData.TYPE_2;
        double minRating = 4.0;

        List<IOrganization> results = organizationDao.findOrganizationsByVehicleTypeAndDriverRating(vehicleType, minRating);

        assertTrue(results.isEmpty());
    }

    @GitHubClassroomGrading(maxScore = 5)
    @Test(timeout = 2000, expected = IllegalArgumentException.class)
    public void testFindOrganizationsByVehicleTypeAndDriverRating_InvalidRating() {
        organizationDao.findOrganizationsByVehicleTypeAndDriverRating(TestData.TYPE_1, -1);
    }


}
