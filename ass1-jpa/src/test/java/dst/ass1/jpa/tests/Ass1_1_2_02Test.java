package dst.ass1.jpa.tests;

import dst.ass1.grading.GitHubClassroomGrading;
import dst.ass1.grading.LocalGradingClassRule;
import dst.ass1.grading.LocalGradingRule;
import dst.ass1.jpa.ORMService;
import dst.ass1.jpa.model.IRider;
import dst.ass1.jpa.util.Constants;
import org.hibernate.PropertyValueException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import javax.persistence.PersistenceException;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

/**
 * Tests the constraint for IRider email.
 */
public class Ass1_1_2_02Test {
    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();

    @Rule
    public ORMService orm = new ORMService();

    @GitHubClassroomGrading(maxScore = 3)
    @Test(timeout = 2000)
    public void testUniqueConstraint() {
        new UniqueConstraintTester<>(() -> {
            IRider rider = orm.getModelFactory().createRider();
            rider.setName("name");
            rider.setTel("tel");
            return rider;
        }, e -> e.setEmail("unique@example.com"))
            .run(orm.getEntityManager());
    }

    @GitHubClassroomGrading(maxScore = 3)
    @Test(timeout = 2000)
    public void testNotNullConstraint() {
        IRider e1 = orm.getModelFactory().createRider();
        e1.setEmail(null);
        e1.setTel("tel");
        var e = assertThrows(PersistenceException.class, () -> {
            orm.em().getTransaction().begin();
            orm.em().persist(e1);
            orm.em().flush();
        });
        assertThat(e.getMessage(), containsString("not-null property"));
        assertThat(e.getCause(), is(instanceOf(PropertyValueException.class)));
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Test(timeout = 2000)
    public void testRiderEmailNotNullConstraintJdbc() throws SQLException {
        assertFalse(orm.getDatabaseGateway().isNullable(Constants.T_RIDER, Constants.M_RIDER_EMAIL));
    }

}
