package dst.ass1.jpa.dao;

import dst.ass1.jpa.model.IOrganization;

import java.util.List;

public interface IOrganizationDAO extends GenericDAO<IOrganization> {

    /**
     * Finds organizations that have active drivers with specified vehicle type and minimum rating.
     *
     * @param vehicleType the type of vehicle to search for (e.g., "type1", "type2")
     * @param minRating   minimum driver rating threshold (must be between 0 and 5)
     * @return List of organizations meeting the criteria
     * @throws IllegalArgumentException if minRating is not between 0 and 5
     */
    List<IOrganization> findOrganizationsByVehicleTypeAndDriverRating(String vehicleType, double minRating);
}
