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

package ua.org.java.dynamoit.model;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import ua.org.java.dynamoit.components.tablegrid.Attributes;

import java.util.LinkedHashMap;

public class TableDef {

    private final String name;
    private final SimpleStringProperty hashAttribute = new SimpleStringProperty();
    private final SimpleStringProperty rangeAttribute = new SimpleStringProperty();
    private final ObservableMap<String, Attributes.Type> attributeTypesMap = FXCollections.observableMap(new LinkedHashMap<>());
    private final SimpleLongProperty totalCount = new SimpleLongProperty();

    public TableDef(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getHashAttribute() {
        return hashAttribute.get();
    }

    public void setHashAttribute(String hashAttribute) {
        this.hashAttribute.set(hashAttribute);
    }

    public String getRangeAttribute() {
        return rangeAttribute.get();
    }

    public void setRangeAttribute(String rangeAttribute) {
        this.rangeAttribute.set(rangeAttribute);
    }

    public ObservableMap<String, Attributes.Type> getAttributeTypesMap() {
        return attributeTypesMap;
    }

    public SimpleLongProperty totalCountProperty() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount.set(totalCount);
    }
}
