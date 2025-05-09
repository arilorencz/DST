package dst.ass1.jpa.tests;

import dst.ass1.grading.GitHubClassroomGrading;
import dst.ass1.grading.LocalGradingClassRule;
import dst.ass1.grading.LocalGradingRule;
import dst.ass1.jpa.ORMService;
import dst.ass1.jpa.util.Constants;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests the IVehicle license unique constraint.
 */
public class Ass1_1_1_02Test {

    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();

    @Rule
    public ORMService orm = new ORMService();

    @GitHubClassroomGrading(maxScore = 1)
    @Test(timeout = 2000)
    public void testConstraintVehicleLicenseUnique() {
        new UniqueConstraintTester<>(() -> orm.getModelFactory().createVehicle(), e -> e.setLicense("uniquevalue"))
            .run(orm.getEntityManager());
    }

    @GitHubClassroomGrading(maxScore = 1)
    @Test(timeout = 2000)
    public void testConstraintJdbcVehicleLicenseUnique() {
        assertTrue(orm.getDatabaseGateway().isIndex(Constants.T_VEHICLE, Constants.M_VEHICLE_LICENSE, false));
        assertTrue(orm.getDatabaseGateway().isNullable(Constants.T_VEHICLE, Constants.M_VEHICLE_LICENSE));
    }

}
