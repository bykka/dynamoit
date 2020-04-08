package ua.org.java.dynamoit.table;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Page;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.LongBinding;
import javafx.beans.property.SimpleObjectProperty;
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
    private Map<String, SimpleStringProperty> attributeFilterMap = new HashMap<>();
    private SimpleObjectProperty<Page<Item, ?>> currentPage = new SimpleObjectProperty<>();
    private SimpleObjectProperty<DescribeTableResult> describeTableResult = new SimpleObjectProperty<>();
    private LongBinding totalCount = Bindings.createLongBinding(() -> describeTableResult.get() == null ? 0 : describeTableResult.get().getTable().getItemCount(), describeTableResult);
    private SimpleStringProperty hashAttribute = new SimpleStringProperty();
    private SimpleStringProperty rangeAttribute = new SimpleStringProperty();

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

    public Long getTotalCount() {
        return totalCount.get();
    }

    public LongBinding totalCountProperty() {
        return totalCount;
    }

    public Map<String, SimpleStringProperty> getAttributeFilterMap() {
        return attributeFilterMap;
    }

    public Page<Item, ?> getCurrentPage() {
        return currentPage.get();
    }

    public SimpleObjectProperty<Page<Item, ?>> currentPageProperty() {
        return currentPage;
    }

    public void setCurrentPage(Page<Item, ?> currentPage) {
        this.currentPage.set(currentPage);
    }

    public DescribeTableResult getDescribeTableResult() {
        return describeTableResult.get();
    }

    public SimpleObjectProperty<DescribeTableResult> describeTableResultProperty() {
        return describeTableResult;
    }

    public void setDescribeTableResult(DescribeTableResult describeTableResult) {
        this.describeTableResult.set(describeTableResult);
    }

    public String getHashAttribute() {
        return hashAttribute.get();
    }

    public SimpleStringProperty hashAttributeProperty() {
        return hashAttribute;
    }

    public String getRangeAttribute() {
        return rangeAttribute.get();
    }

    public SimpleStringProperty rangeAttributeProperty() {
        return rangeAttribute;
    }

    public void setHashAttribute(String hashAttribute) {
        this.hashAttribute.set(hashAttribute);
    }

    public void setRangeAttribute(String rangeAttribute) {
        this.rangeAttribute.set(rangeAttribute);
    }
}
