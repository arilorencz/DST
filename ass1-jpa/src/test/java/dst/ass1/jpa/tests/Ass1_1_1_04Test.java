package dst.ass1.jpa.tests;

import dst.ass1.grading.GitHubClassroomGrading;
import dst.ass1.grading.LocalGradingClassRule;
import dst.ass1.grading.LocalGradingRule;
import dst.ass1.jpa.ORMService;
import dst.ass1.jpa.model.IDriver;
import dst.ass1.jpa.util.Constants;
import org.hibernate.PropertyValueException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import javax.persistence.PersistenceException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

/**
 * Tests the IDriver tel not null constraint.
 */
public class Ass1_1_1_04Test {
    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();

    @Rule
    public ORMService orm = new ORMService();

    @GitHubClassroomGrading(maxScore = 1)
    @Test(timeout = 2000)
    public void testConstraintDriverTelNotNull() {
        IDriver e1 = orm.getModelFactory().createDriver();
        e1.setTel(null);
        var e = assertThrows(PersistenceException.class, () -> {
            orm.em().getTransaction().begin();
            orm.em().persist(e1);
            orm.em().flush();
        });
        assertThat(e.getMessage(), containsString("not-null property"));
        assertThat(e.getCause(), is(instanceOf(PropertyValueException.class)));
    }

    @GitHubClassroomGrading(maxScore = 1)
    @Test(timeout = 2000)
    public void testConstraintJdbcDriverTelNotNull() {
        assertFalse(orm.getDatabaseGateway().isNullable(Constants.T_DRIVER, Constants.M_DRIVER_TEL));
    }

}
