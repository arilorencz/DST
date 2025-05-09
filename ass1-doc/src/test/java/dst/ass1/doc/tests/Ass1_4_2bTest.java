package dst.ass1.doc.tests;

import dst.ass1.grading.GitHubClassroomGrading;
import dst.ass1.doc.DocumentTestData;
import dst.ass1.doc.EmbeddedMongo;
import dst.ass1.doc.MongoService;
import dst.ass1.grading.LocalGradingClassRule;
import dst.ass1.grading.LocalGradingRule;
import org.bson.Document;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Ass1_4_2bTest {

    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();

    @ClassRule
    public static EmbeddedMongo embeddedMongo = new EmbeddedMongo();

    @Rule
    public MongoService mongo = new MongoService(new DocumentTestData());

    @GitHubClassroomGrading(maxScore = 10)
    @Test(timeout = 2000)
    public void testFindOpenRestaurantsInRadius_Success() {
        // Center around TU Wien area
        double centerLng = 16.3699;
        double centerLat = 48.199;
        double radiusKm = 1;
        int hour = 12; // Noon

        List<Document> results = mongo.getDocumentQuery().findOpenRestaurantsInRadius(
                centerLng, centerLat, radiusKm, hour);

        assertEquals(3, results.size()); // Should find 3 McDonald's locations
        for (Document doc : results) {
            assertEquals("Restaurant", doc.getString("category"));
            assertTrue(doc.getInteger("openHour") <= hour);
            assertTrue(doc.getInteger("closingHour") > hour);
            assertEquals("place", doc.getString("type"));
        }
    }

    @GitHubClassroomGrading(maxScore = 10)
    @Test(timeout = 2000)
    public void testFindOpenRestaurantsInRadius_ClosedHour() {
        double centerLng = 16.3699;
        double centerLat = 48.199;
        double radiusKm = 1.0;
        int hour = 5; // Early morning when most are closed

        List<Document> results = mongo.getDocumentQuery().findOpenRestaurantsInRadius(
                centerLng, centerLat, radiusKm, hour);

        assertEquals(1, results.size()); // Only the 24h McDonald's should be open
        Document restaurant = results.get(0);
        assertEquals(0, restaurant.getInteger("openHour").intValue());
        assertEquals(24, restaurant.getInteger("closingHour").intValue());
    }

    @GitHubClassroomGrading(maxScore = 5)
    @Test(timeout = 2000, expected = IllegalArgumentException.class)
    public void testFindOpenRestaurantsInRadius_InvalidHour() {
        double centerLng = 16.3699;
        double centerLat = 48.199;
        double radiusKm = 1.0;
        int hour = 24; // Invalid hour

        mongo.getDocumentQuery().findOpenRestaurantsInRadius(centerLng, centerLat, radiusKm, hour);
    }

}
