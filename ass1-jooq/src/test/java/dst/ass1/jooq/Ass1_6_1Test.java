package dst.ass1.jooq;

import dst.ass1.grading.GitHubClassroomGrading;
import dst.ass1.grading.LocalGradingClassRule;
import dst.ass1.grading.LocalGradingRule;
import dst.ass1.jooq.connection.DataSource;
import dst.ass1.jooq.dao.IRiderPreferenceDAO;
import dst.ass1.jooq.dao.impl.DAOFactory;
import dst.ass1.jooq.model.IModelFactory;
import dst.ass1.jooq.model.IRiderPreference;
import dst.ass1.jooq.model.impl.ModelFactory;
import dst.ass1.jooq.util.TupleResult;
import org.jooq.DSLContext;
import org.junit.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class Ass1_6_1Test {
    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();

    private static DSLContext dslContext;
    private static IRiderPreferenceDAO riderPreferenceDAO;
    private IRiderPreference riderPreference;
    private static IModelFactory modelFactory;

    @Before
    public void setup() {
        dslContext.execute("DELETE FROM PREFERENCE");
        dslContext.execute("DELETE FROM RIDER_PREFERENCE");

        riderPreference = modelFactory.createRiderPreference();
        riderPreference.setRiderId(1L);
        riderPreference.setArea("vienna");
        riderPreference.setVehicleClass("standard");
        riderPreference.setPreferences(Map.of("foo", "bar"));
    }

    @BeforeClass
    public static void setupAll() throws SQLException {
        dslContext = DataSource.getConnection();
        var factory = new DAOFactory(dslContext);
        riderPreferenceDAO = factory.createRiderPreferenceDao();
        modelFactory = new ModelFactory();
    }

    @Test
    @GitHubClassroomGrading(maxScore = 15)
    public void insertsRider() {
        riderPreferenceDAO.insert(riderPreference);

        assertEquals(riderPreference, riderPreferenceDAO.findById(riderPreference.getRiderId()));
    }

    @Test
    @GitHubClassroomGrading(maxScore = 15)
    public void deletesRider() {
        riderPreferenceDAO.insert(riderPreference);

        riderPreferenceDAO.delete(riderPreference.getRiderId());

        assertNull(riderPreferenceDAO.findById(riderPreference.getRiderId()));
    }

    @Test
    @GitHubClassroomGrading(maxScore = 10)
    public void onlyDeletesSpecifiedRider() {
        var riderPreference2 = modelFactory.createRiderPreference();
        riderPreference2.setRiderId(2L);
        riderPreference2.setArea("Graz");
        riderPreference2.setVehicleClass("standard");
        riderPreference2.setPreferences(Map.of("foo", "bar"));

        riderPreferenceDAO.insert(riderPreference);
        riderPreferenceDAO.insert(riderPreference2);

        riderPreferenceDAO.delete(riderPreference.getRiderId());

        assertNull(riderPreferenceDAO.findById(riderPreference.getRiderId()));
        assertEquals(riderPreference2, riderPreferenceDAO.findById(riderPreference2.getRiderId()));
    }

    @Test
    @GitHubClassroomGrading(maxScore = 15)
    public void deletesPreferencesAfterRiderWasDeleted() {
        riderPreferenceDAO.insert(riderPreference);

        riderPreferenceDAO.delete(riderPreference.getRiderId());

        var count = org.jooq.impl.DSL.field("count (*)", Integer.class);
        var select =
            dslContext
                .select(count)
                .from("preference p")
                .where("p.rider_id = " + riderPreference.getRiderId())
                .fetch();
        assertEquals(1, select.size());
        assertEquals(0, select.get(0).get(count).intValue());
    }

    @Test
    @GitHubClassroomGrading(maxScore = 20)
    public void updatesPreferences() {
        riderPreferenceDAO.insert(riderPreference);

        riderPreference.setPreferences(Map.of("foo", "foo", "test", "test3"));
        riderPreferenceDAO.updatePreferences(riderPreference);

        assertEquals(riderPreference, riderPreferenceDAO.findById(riderPreference.getRiderId()));
    }

    @Test
    @GitHubClassroomGrading(maxScore = 15)
    public void testGetMostPopularVehicleClassPerArea_Success() {
        IModelFactory modelFactory = new ModelFactory();
        IRiderPreference pref1 = modelFactory.createRiderPreference();
        pref1.setRiderId(1L);
        pref1.setVehicleClass("SUV");
        pref1.setArea("Downtown");

        IRiderPreference pref2 = modelFactory.createRiderPreference();
        pref2.setRiderId(2L);
        pref2.setVehicleClass("SUV");
        pref2.setArea("Downtown");

        IRiderPreference pref3 = modelFactory.createRiderPreference();
        pref3.setRiderId(3L);
        pref3.setVehicleClass("Sedan");
        pref3.setArea("Downtown");

        IRiderPreference pref4 = modelFactory.createRiderPreference();
        pref4.setRiderId(4L);
        pref4.setVehicleClass("Sedan");
        pref4.setArea("Suburbs");

        IRiderPreference pref5 = modelFactory.createRiderPreference();
        pref5.setRiderId(5L);
        pref5.setVehicleClass("Van");
        pref5.setArea("Suburbs");

        riderPreferenceDAO.insert(pref1);
        riderPreferenceDAO.insert(pref2);
        riderPreferenceDAO.insert(pref3);
        riderPreferenceDAO.insert(pref4);
        riderPreferenceDAO.insert(pref5);

        List<TupleResult<String, String>> results = riderPreferenceDAO.getMostPopularVehicleClassPerArea();

        assertEquals(2, results.size());
        assertEquals("SUV", results.get(0).getSecond()); // Most popular in Downtown
        assertEquals("Sedan", results.get(1).getSecond()); // Tied with Van in Suburbs
    }

    @Test
    @GitHubClassroomGrading(maxScore = 10)
    public void testGetMostPopularVehicleClassPerArea_NoPreferences() {
        List<TupleResult<String, String>> results = riderPreferenceDAO.getMostPopularVehicleClassPerArea();

        assertTrue(results.isEmpty());
    }

    @Test
    @GitHubClassroomGrading(maxScore = 10)
    public void testGetMostPopularVehicleClassPerArea_SinglePreference() {
        IRiderPreference pref = modelFactory.createRiderPreference();
        pref.setRiderId(1L);
        pref.setVehicleClass("SUV");
        pref.setArea("Downtown");

        riderPreferenceDAO.insert(pref);

        List<TupleResult<String, String>> results = riderPreferenceDAO.getMostPopularVehicleClassPerArea();

        assertEquals(1, results.size());
        assertEquals("Downtown", results.get(0).getFirst());
        assertEquals("SUV", results.get(0).getSecond());
    }

}
