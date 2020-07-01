package ua.org.java.dynamoit.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import ua.org.java.dynamoit.components.tablegrid.Attributes;

import java.util.LinkedHashMap;

public class TableDef {

    private String name;
    private final SimpleStringProperty hashAttribute = new SimpleStringProperty();
    private final SimpleStringProperty rangeAttribute = new SimpleStringProperty();
    private final ObservableMap<String, Attributes.Type> attributeTypesMap = FXCollections.observableMap(new LinkedHashMap<>());

    public TableDef(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHashAttribute() {
        return hashAttribute.get();
    }

    public SimpleStringProperty hashAttributeProperty() {
        return hashAttribute;
    }

    public void setHashAttribute(String hashAttribute) {
        this.hashAttribute.set(hashAttribute);
    }

    public String getRangeAttribute() {
        return rangeAttribute.get();
    }

    public SimpleStringProperty rangeAttributeProperty() {
        return rangeAttribute;
    }

    public void setRangeAttribute(String rangeAttribute) {
        this.rangeAttribute.set(rangeAttribute);
    }

    public ObservableMap<String, Attributes.Type> getAttributeTypesMap() {
        return attributeTypesMap;
    }
}
