package dst.ass1.doc.tests;

import dst.ass1.doc.DocumentTestData;
import dst.ass1.doc.EmbeddedMongo;
import dst.ass1.doc.MongoService;
import dst.ass1.grading.GitHubClassroomGrading;
import dst.ass1.grading.LocalGradingClassRule;
import dst.ass1.grading.LocalGradingRule;
import org.bson.Document;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class Ass1_4_3_01Test {
    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();

    @ClassRule
    public static EmbeddedMongo embeddedMongo = new EmbeddedMongo();

    @Rule
    public MongoService mongo = new MongoService(new DocumentTestData());

    @GitHubClassroomGrading(maxScore = 20)
    @Test(timeout = 2000)
    public void testCalculateLocationDensity_TUWienArea() {
        // Polygon around TU Wien area including TU Wien, DSG, and CVL
        List<List<Double>> coordinates = Arrays.asList(
                Arrays.asList(16.369, 48.195),  // Southwest corner
                Arrays.asList(16.372, 48.195),  // Southeast corner
                Arrays.asList(16.372, 48.200),  // Northeast corner
                Arrays.asList(16.369, 48.200),  // Northwest corner
                Arrays.asList(16.369, 48.195)   // Close polygon
        );

        Document result = mongo.getDocumentQuery().calculateLocationDensity(coordinates);

        assertNotNull(result);
        assertEquals(3, result.getLong("count").intValue()); // TU Wien, DSG, CVL
        assertTrue(result.getDouble("areaKm2") > 0);
        // Area should be roughly 0.15 km² with 3 locations
        assertTrue(result.getDouble("densityPerKm2") > 15);
    }

    @GitHubClassroomGrading(maxScore = 20)
    @Test(timeout = 2000)
    public void testCalculateLocationDensity_UniversityArea() {
        // Polygon covering three universities and one street in Vienna
        List<List<Double>> coordinates = Arrays.asList(
                Arrays.asList(16.337443, 48.236530),
                Arrays.asList(16.408456, 48.213561),
                Arrays.asList(16.360372, 48.213069),
                Arrays.asList(16.337443, 48.236530)
        );

        Document result = mongo.getDocumentQuery().calculateLocationDensity(coordinates);

        assertNotNull(result);
        assertEquals(4, result.getLong("count").intValue()); // Should find 3 universities and one street
        double area = result.getDouble("areaKm2");
        assertTrue(area > 0);
    }

    @GitHubClassroomGrading(maxScore = 5)
    @Test(timeout = 2000)
    public void testCalculateLocationDensity_NoLocations() {
        // Polygon in area without any locations (south of Vienna)
        List<List<Double>> coordinates = Arrays.asList(
                Arrays.asList(16.35, 48.15),
                Arrays.asList(16.40, 48.15),
                Arrays.asList(16.40, 48.18),
                Arrays.asList(16.35, 48.18),
                Arrays.asList(16.35, 48.15)
        );
        Document result = mongo.getDocumentQuery().calculateLocationDensity(coordinates);

        assertNotNull(result);
        assertEquals(0, result.getLong("count").intValue());
        assertTrue(result.getDouble("areaKm2") > 0);
        assertEquals(0.0, result.getDouble("densityPerKm2"), 0.001);
    }


}
