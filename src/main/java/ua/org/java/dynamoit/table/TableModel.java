package ua.org.java.dynamoit.table;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Page;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import ua.org.java.dynamoit.MainModel;

import java.util.HashMap;
import java.util.Map;

public class TableModel {

    private MainModel mainModel;

    private ObservableList<Item> rows = FXCollections.observableArrayList();
    private IntegerBinding rowsSize = Bindings.createIntegerBinding(() -> rows.size(), rows);
    private SimpleStringProperty totalCount = new SimpleStringProperty();
    private Map<String, SimpleStringProperty> attributeFilterMap = new HashMap<>();
    private Page<Item, ?> currentPage;

    public TableModel(MainModel mainModel) {
        this.mainModel = mainModel;
    }

    public MainModel getMainModel() {
        return mainModel;
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

    public String getTotalCount() {
        return totalCount.get();
    }

    public SimpleStringProperty totalCountProperty() {
        return totalCount;
    }

    public void setTotalCount(String totalCount) {
        this.totalCount.set(totalCount);
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
