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

package ua.org.java.dynamoit.components.tablegrid;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndexDescription;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import ua.org.java.dynamoit.components.main.MainModel;
import ua.org.java.dynamoit.model.TableDef;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class TableGridModel {

    private final MainModel.ProfileModel profileModel;

    private TableDef tableDef;
    private TableDescription originalTableDescription;
    private String tableName;
    private String profile;

    private final ObservableList<EnhancedDocument> rows = FXCollections.observableArrayList();
    private final IntegerBinding rowsSize = Bindings.createIntegerBinding(rows::size, rows);
    private Iterator<Page<EnhancedDocument>> pageIterator;
    private List<GlobalSecondaryIndexDescription> fullProjectionIndexes = List.of();

    private final ObservableMap<String, SimpleStringProperty> attributeFilterMap = FXCollections.observableHashMap();

    public TableGridModel(MainModel.ProfileModel profileModel) {
        this.profileModel = profileModel;
    }

    public MainModel.ProfileModel getProfileModel() {
        return profileModel;
    }

    public TableDef getTableDef() {
        return tableDef;
    }

    public void setTableDef(TableDef tableDef) {
        this.tableDef = tableDef;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public ObservableList<EnhancedDocument> getRows() {
        return rows;
    }

    public Number getRowsSize() {
        return rowsSize.get();
    }

    public IntegerBinding rowsSizeProperty() {
        return rowsSize;
    }

    public ObservableMap<String, SimpleStringProperty> getAttributeFilterMap() {
        return attributeFilterMap;
    }

    public Iterator<Page<EnhancedDocument>> getPageIterator() {
        return pageIterator;
    }

    public void setPageIterator(Iterator<Page<EnhancedDocument>> pageIterator) {
        this.pageIterator = pageIterator;
    }

    public TableDescription getOriginalTableDescription() {
        return originalTableDescription;
    }

    public TableGridModel setOriginalTableDescription(TableDescription originalTableDescription) {
        this.originalTableDescription = originalTableDescription;

        this.fullProjectionIndexes = originalTableDescription.globalSecondaryIndexes().stream()
                .filter(__ -> __.projection().projectionType().equals(ProjectionType.ALL)).toList();

        return this;
    }

    /**
     * Returns a stream of {@link GlobalSecondaryIndexDescription} objects representing the full projection indexes of this table.
     * A full projection index is a global secondary index that replicates all attributes from the primary table.
     *
     * @return A stream of {@link GlobalSecondaryIndexDescription} objects.
     */
    public List<GlobalSecondaryIndexDescription> getFullProjectionIndexes() {
        return this.fullProjectionIndexes;
    }
}
