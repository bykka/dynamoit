package ua.org.java.dynamoit;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.util.Objects;

public class MainModel {

    private ObservableList<TableDef> availableTables = FXCollections.observableArrayList();
    private SimpleStringProperty filter = new SimpleStringProperty("");
    private FilteredList<TableDef> filteredTables = availableTables.filtered(Objects::nonNull);
    private ObservableList<String> savedFilters = FXCollections.observableArrayList();
    private SimpleStringProperty selectedProfile = new SimpleStringProperty();
    private ObservableList<String> availableProfiles = FXCollections.observableArrayList();

    public MainModel() {
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

    public static class TableDef {

        private String name;

        public TableDef(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
