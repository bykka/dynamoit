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

package ua.org.java.dynamoit.components.main;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import ua.org.java.dynamoit.model.TableDef;
import ua.org.java.dynamoit.model.profile.PreconfiguredProfileDetails;
import ua.org.java.dynamoit.model.profile.ProfileDetails;
import ua.org.java.dynamoit.model.profile.RemoteProfileDetails;
import ua.org.java.dynamoit.utils.HighlightColors;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;

public class MainModel {

    private final ObservableMap<String, ProfileModel> availableProfiles = FXCollections.observableMap(new LinkedHashMap<>());

    public ObservableMap<String, ProfileModel> getAvailableProfiles() {
        return availableProfiles;
    }

    public void addProfile(ProfileDetails profileDetails) {
        this.availableProfiles.put(profileDetails.getName(), new ProfileModel(profileDetails));
    }

    public static class ProfileModel {

        private final ObservableList<TableDef> availableTables = FXCollections.observableArrayList();
        private final SimpleStringProperty filter = new SimpleStringProperty("");
        private final FilteredList<TableDef> filteredTables = availableTables.filtered(Objects::nonNull);
        private final ObservableList<String> savedFilters = FXCollections.observableArrayList();
        private final SimpleStringProperty region = new SimpleStringProperty();
        private HighlightColors color;
        private final ProfileDetails profileDetails;

        public ProfileModel(ProfileDetails profileDetails) {
            this.profileDetails = profileDetails;

            if (profileDetails instanceof PreconfiguredProfileDetails p) {
                this.region.setValue(p.getRegion());
            } else if (profileDetails instanceof RemoteProfileDetails r) {
                this.region.setValue(r.getRegion());
            }

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

        public FilteredList<TableDef> getFilteredTables() {
            return this.filteredTables;
        }

        public ObservableList<String> getSavedFilters() {
            return savedFilters;
        }

        public ProfileDetails getProfileDetails() {
            return profileDetails;
        }

        public String getProfile() {
            return profileDetails.getName();
        }

        public String getRegion() {
            return region.get();
        }

        public SimpleStringProperty regionProperty() {
            return this.region;
        }

        public Optional<HighlightColors> getColor() {
            return Optional.ofNullable(color);
        }

        public void setColor(HighlightColors color) {
            this.color = color;
        }
    }

}
