package dst.ass1.jooq.dao;

import dst.ass1.jooq.model.IRiderPreference;
import dst.ass1.jooq.util.TupleResult;

import java.util.List;

public interface IRiderPreferenceDAO extends GenericDAO<IRiderPreference> {

    /**
     * Update the preference map of a rider. Adds new preferences or alters existing ones. Does not
     * remove or alter not specified preferences.
     * Also updates all instance fields of the model.
     *
     * @param model the model containing the preferences to alter or to add
     */
    void updatePreferences(IRiderPreference model);

    /**
     * Finds the most popular vehicle class for each area based on rider preferences.
     *
     * @return List of TupleResult containing area and most common vehicle class
     */
    List<TupleResult<String, String>> getMostPopularVehicleClassPerArea();
}
