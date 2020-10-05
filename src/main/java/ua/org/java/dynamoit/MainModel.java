/*
 * This file is part of DynamoIt.
 *
 *     DynamoIt is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     DynamoIt is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with DynamoIt.  If not, see <https://www.gnu.org/licenses/>.
 */

package ua.org.java.dynamoit;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import ua.org.java.dynamoit.model.TableDef;

import java.util.LinkedHashMap;
import java.util.Objects;

public class MainModel {

    private final ObservableMap<String, ProfileModel> availableProfiles = FXCollections.observableMap(new LinkedHashMap<>());

    public ObservableMap<String, ProfileModel> getAvailableProfiles() {
        return availableProfiles;
    }

    public void addProfile(String profile, String region) {
        this.availableProfiles.put(profile, new ProfileModel(profile, region));
    }

    public static class ProfileModel {

        private final ObservableList<TableDef> availableTables = FXCollections.observableArrayList();
        private final SimpleStringProperty filter = new SimpleStringProperty("");
        private final FilteredList<TableDef> filteredTables = availableTables.filtered(Objects::nonNull);
        private final ObservableList<String> savedFilters = FXCollections.observableArrayList();
        private final String profile;
        private final String region;

        public ProfileModel(String profile, String region) {
            this.profile = profile;
            this.region = region;
            filter.addListener((observable, oldValue, newValue) -> filteredTables.setPredicate(value -> value.getName().contains(filter.get())));
        }

        public ObservableList<TableDef> getAvailableTables() {
            return availableTables;
        }

        public String getFilter() {
            return filter.get();
        }

        public SimpleStringProperty filterProperty() {
            return filter;
        }

        public FilteredList<TableDef> getFilteredTables(){
            return this.filteredTables;
        }

        public ObservableList<String> getSavedFilters() {
            return savedFilters;
        }

        public String getProfile() {
            return profile;
        }

        public String getRegion() {
            return region;
        }
    }

}
