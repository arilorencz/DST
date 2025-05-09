package dst.ass1.doc.tests;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import dst.ass1.grading.GitHubClassroomGrading;
import dst.ass1.doc.EmbeddedMongo;
import dst.ass1.doc.IDocumentRepository;
import dst.ass1.doc.MockLocation;
import dst.ass1.doc.MongoService;
import dst.ass1.grading.LocalGradingClassRule;
import dst.ass1.grading.LocalGradingRule;
import dst.ass1.jpa.util.Constants;
import org.bson.Document;
import org.junit.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Ass1_4_1GradingTest {

    @ClassRule
    public static EmbeddedMongo embeddedMongo = new EmbeddedMongo();

    @ClassRule
    public static LocalGradingClassRule afterAll = new LocalGradingClassRule();

    @Rule
    public LocalGradingRule grading = new LocalGradingRule();

    @Rule
    public MongoService mongo = new MongoService();

    private MockLocation l1;

    private static final String GEO_INDEX_NAME = "geo_2dsphere";

    private Map<String, Object> m1Properties;

    @Before
    public void setUp() throws Exception {
        l1 = new MockLocation();
        l1.setName("testName1");
        l1.setLocationId(1L);


        m1Properties = new HashMap<>();
        m1Properties.put("1337", "7331");
        m1Properties.put("Foo", "Bar");
        m1Properties.put("Complex", Arrays.asList(1, 2, 3, 5, 8));

    }

    @Test
    @GitHubClassroomGrading(maxScore = 5)
    public void checkIndexApplied() {
        IDocumentRepository documentRepository = mongo.getDocumentRepository();
        MongoDatabase mongoDatabase = mongo.getMongoDatabase();

        documentRepository.insert(l1, m1Properties);

        MongoCollection<Document> collection = mongoDatabase.getCollection(Constants.COLL_LOCATION_DATA);

        boolean indexExists = false;

        for (Document cur : collection.listIndexes()) {
            Document index = (Document) cur.get("key");

            indexExists = index.containsKey(Constants.I_LOCATION);

            if (indexExists) {
                break;
            }
        }

        Assert.assertTrue("No index was created for " + Constants.I_LOCATION, indexExists);
    }

    @Test
    @GitHubClassroomGrading(maxScore = 5)
    public void checkGeoIndexApplied() {
        IDocumentRepository documentRepository = mongo.getDocumentRepository();
        MongoDatabase mongoDatabase = mongo.getMongoDatabase();

        documentRepository.insert(l1, m1Properties);

        MongoCollection<Document> collection = mongoDatabase.getCollection(Constants.COLL_LOCATION_DATA);

        boolean sphereIndexExists = false;

        for (Document cur : collection.listIndexes()) {
            if (GEO_INDEX_NAME.equals(cur.getString("name"))) {
                sphereIndexExists = true;
            }

            if (sphereIndexExists) {
                break;
            }
        }

        Assert.assertTrue("No index was created for " + Constants.M_LOCATION_GEO, sphereIndexExists);
    }
}
