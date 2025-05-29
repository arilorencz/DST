package dst.ass1.doc.impl;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import dst.ass1.doc.IDocumentRepository;
import dst.ass1.jpa.model.ILocation;
import dst.ass1.jpa.util.Constants;
import org.bson.Document;

import java.util.Map;

public class DocumentRepository implements IDocumentRepository {

    private final MongoCollection<Document> collection;
    private MongoDatabase db;

    public DocumentRepository() {
        var mongoClient = MongoClients.create();  // Uses default MongoDB URI (localhost)
        db = mongoClient.getDatabase(Constants.MONGO_DB_NAME);
        this.collection = db.getCollection(Constants.COLL_LOCATION_DATA);

        collection.createIndex(Indexes.ascending("location_id"));
        collection.createIndex(Indexes.geo2dsphere("geo"));
    }
    @Override
    public void insert(ILocation location, Map<String, Object> locationProperties) {
        Document doc = new Document()
                .append("location_id", location.getLocationId())
                .append("name", location.getName());

        if (locationProperties != null) {
            doc.putAll(locationProperties);
        }

        collection.insertOne(doc);
    }
}
