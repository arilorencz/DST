package dst.ass1.doc;

import org.bson.Document;

import java.util.List;

public interface IDocumentQuery {

    /**
     * Calculates the density of locations within a specified polygon area.
     *
     * @param coordinates List of coordinate pairs defining the polygon boundary
     * @return Document containing location count and density per square kilometer
     */
    Document calculateLocationDensity(List<List<Double>> coordinates);

    /**
     * Finds restaurants within a radius that are open at the specified hour and match the specified category.
     *
     * @param centerLng center longitude of the search area
     * @param centerLat center latitude of the search area
     * @param radiusKm radius in kilometers
     * @param hour hour of the day (0-23)
     * @return List of matching restaurant documents
     * @throws IllegalArgumentException if hour is not between 0 and 23
     */
    List<Document> findOpenRestaurantsInRadius(double centerLng, double centerLat,
                                                      double radiusKm, int hour);

    /**
     * Retrieves a list of documents with the specified type.
     *
     * @param type the type of the documents to search for
     * @return a List of Document objects that match the specified type
     */
    List<Document> findDocumentsByType(String type);
}
