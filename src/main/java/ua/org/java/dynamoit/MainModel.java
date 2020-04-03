package ua.org.java.dynamoit;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.util.List;
import java.util.Objects;

public class MainModel {

    private ObservableList<String> availableTables = FXCollections.observableArrayList();
    private SimpleStringProperty filter = new SimpleStringProperty("");
    private FilteredList<String> filteredTables = availableTables.filtered(Objects::nonNull);
    private ObservableList<String> savedFilters = FXCollections.observableArrayList();

    public MainModel() {
        filter.addListener((observable, oldValue, newValue) -> filteredTables.setPredicate(value -> value.contains(filter.get())));
    }

    public List<String> getAvailableTables() {
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
}
