package ua.org.java.dynamoit.components.tablegrid;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Page;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import ua.org.java.dynamoit.MainModel;
import ua.org.java.dynamoit.model.TableDef;

import java.util.HashMap;
import java.util.Map;

public class TableGridModel {

    private final MainModel mainModel;

    private TableDef tableDef;
    private String tableName;

    private final ObservableList<Item> rows = FXCollections.observableArrayList();
    private final IntegerBinding rowsSize = Bindings.createIntegerBinding(rows::size, rows);
    private Page<Item, ?> currentPage;

    private final Map<String, SimpleStringProperty> attributeFilterMap = new HashMap<>();

    public TableGridModel(MainModel mainModel) {
        this.mainModel = mainModel;
    }

    public MainModel getMainModel() {
        return mainModel;
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

    public ObservableList<Item> getRows() {
        return rows;
    }

    public Number getRowsSize() {
        return rowsSize.get();
    }

    public IntegerBinding rowsSizeProperty() {
        return rowsSize;
    }

    public Map<String, SimpleStringProperty> getAttributeFilterMap() {
        return attributeFilterMap;
    }

    public Page<Item, ?> getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Page<Item, ?> currentPage) {
        this.currentPage = currentPage;
    }

}
