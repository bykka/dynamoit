package ua.org.java.dynamoit;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.util.Objects;

public class MainModel {

    private ObservableList<String> availableTables = FXCollections.observableArrayList();
    private SimpleStringProperty filter = new SimpleStringProperty("");
    private FilteredList<String> filteredTables = availableTables.filtered(Objects::nonNull);
    private ObservableList<String> savedFilters = FXCollections.observableArrayList();
    private SimpleStringProperty selectedProfile = new SimpleStringProperty();
    private ObservableList<String> availableProfiles = FXCollections.observableArrayList();

    public MainModel() {
        filter.addListener((observable, oldValue, newValue) -> filteredTables.setPredicate(value -> value.contains(filter.get())));
    }

    public ObservableList<String> getAvailableTables() {
        return availableTables;
    }

    public String getFilter() {
        return filter.get();
    }

    public SimpleStringProperty filterProperty() {
        return filter;
    }

    public FilteredList<String> getFilteredTables(){
         return this.filteredTables;
    }

    public ObservableList<String> getSavedFilters() {
        return savedFilters;
    }

    public String getSelectedProfile() {
        return selectedProfile.get();
    }

    public SimpleStringProperty selectedProfileProperty() {
        return selectedProfile;
    }

    public ObservableList<String> getAvailableProfiles() {
        return availableProfiles;
    }

    public void setAvailableProfiles(ObservableList<String> availableProfiles) {
        this.availableProfiles = availableProfiles;
    }
}
