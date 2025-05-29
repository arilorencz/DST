package dst.ass1.doc.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import dst.ass1.doc.IDocumentQuery;
import dst.ass1.jpa.util.Constants;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;

public class DocumentQuery implements IDocumentQuery {

    private static final Logger logger = Logger.getLogger(DocumentQuery.class.getName());

    private final MongoCollection<Document> collection;

    public DocumentQuery(MongoDatabase db) {
        this.collection = db.getCollection(Constants.COLL_LOCATION_DATA);
    }

    @Override
    public Document calculateLocationDensity(List<List<Double>> coordinates) {
        Document polygon = new Document("type", "Polygon")
                .append("coordinates", List.of(coordinates));

        List<Document> result = collection.aggregate(
                List.of(
                        Aggregates.match(Filters.geoWithin("geo", polygon)),
                        Aggregates.count("count")
                )
        ).into(new ArrayList<>());

        // bounding box area (approximate in degrees lat/long)
        Number countNumber = result.isEmpty() ? 0 : (Number) result.get(0).get("count");
        long count = countNumber.longValue();

        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLng = Double.MAX_VALUE, maxLng = -Double.MAX_VALUE;

        for (List<Double> coord : coordinates) {
            double lng = coord.get(0);
            double lat = coord.get(1);
            minLat = Math.min(minLat, lat);
            maxLat = Math.max(maxLat, lat);
            minLng = Math.min(minLng, lng);
            maxLng = Math.max(maxLng, lng);
        }

        // degree-to-km conversion (1 degree latitude ≈ 111.2 km,  degree longitude ≈ 74.5 km)
        double areaKm2 = (maxLat - minLat) * 111.2 * (maxLng - minLng) * 74.5;
        double density = areaKm2 == 0 ? 0 : count / areaKm2;

        // output document
        Document output = new Document("count", count)
                .append("areaKm2", areaKm2)
                .append("densityPerKm2", density);

        logger.info("Location density calculated:");
        logger.info(" - Count: " + count);
        logger.info(" - Area (km²): " + areaKm2);
        logger.info(" - Density: " + density);

        return output;
    }

    @Override
    public List<Document> findOpenRestaurantsInRadius(double centerLng, double centerLat, double radiusKm, int hour) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("Hour must be between 0 and 23.");
        }

        double radiusMeters = radiusKm * 1000;
        Point currentLoc = new Point(new Position(centerLng, centerLat));

        logger.info("Searching for open restaurants within " + radiusKm + " km radius at hour " + hour);
        logger.info(" - Center coordinates: (" + centerLat + ", " + centerLng + ")");

        List<Document> results = collection.aggregate(List.of(
                new Document("$geoNear", new Document()
                        .append("near", currentLoc)
                        .append("distanceField", "dist")
                        .append("maxDistance", radiusMeters)
                        .append("spherical", true)
                        .append("query", new Document("category", "Restaurant")
                                .append("openHour", new Document("$lte", hour))
                                .append("closingHour", new Document("$gte", hour))
                        )
                ),
                Aggregates.project(Projections.fields(
                        Projections.include("location_id", "type", "name", "geo", "category", "openHour", "closingHour")
                )))).into(new ArrayList<>());
        logger.info(" - Restaurants found: " + results.size());
        results.stream().limit(5).forEach(doc ->
            logger.info("   -> " + doc.getString("name") + " (opens: " + doc.get("openHour") + ", closes: " + doc.get("closingHour") + ")")
        );
        return results;
    }

    @Override
    public List<Document> findDocumentsByType(String type) {
        List<Document> results = collection.find(eq("type", type)).into(new ArrayList<>());

        logger.info("Queried documents with type = \"" + type + "\"");
        logger.info("Number of documents found: " + results.size());

        return results;
    }
}
