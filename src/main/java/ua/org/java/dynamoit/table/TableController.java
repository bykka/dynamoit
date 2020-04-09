package ua.org.java.dynamoit.table;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.util.StringUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import ua.org.java.dynamoit.db.DynamoDBService;
import ua.org.java.dynamoit.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static ua.org.java.dynamoit.utils.Utils.asStream;

public class TableController {

    public static final int PAGE_SIZE = 100;
    private AmazonDynamoDB dbClient;
    private DynamoDB documentClient;
    private Table table;
    private TableContext context;
    private TableModel tableModel;

    public TableController(TableContext context, TableModel tableModel, DynamoDBService dynamoDBService) {
        this.context = context;
        this.tableModel = tableModel;

        dbClient = dynamoDBService.getOrCreateDynamoDBClient(context.getProfileName());
        documentClient = dynamoDBService.getOrCreateDocumentClient(context.getProfileName());
        table = documentClient.getTable(context.getTableName());

        CompletableFuture.supplyAsync(() -> dbClient.describeTable(context.getTableName()))
                .thenAccept(describeTable -> Platform.runLater(() -> {
                    Utils.getHashKey(describeTable).ifPresent(tableModel::setHashAttribute);
                    Utils.getRangeKey(describeTable).ifPresent(tableModel::setRangeAttribute);
                    tableModel.setDescribeTableResult(describeTable);
                }))
                .thenCompose(aVoid -> queryPageItems())
                .thenAccept(page -> Platform.runLater(() -> {
                    tableModel.setCurrentPage(page);
                    tableModel.getRows().addAll(asStream(page).collect(Collectors.toList()));
                }));
    }


    public void onReachScrollEnd() {
        if (tableModel.getCurrentPage().hasNextPage()) {
            CompletableFuture
                    .supplyAsync(() -> tableModel.getCurrentPage().nextPage())
                    .thenAccept(page -> Platform.runLater(() -> {
                        tableModel.setCurrentPage(page);
                        tableModel.getRows().addAll(asStream(page).collect(Collectors.toList()));
                    }));
        }
    }

    public void onRefresh() {
        queryPageItems().thenAccept(page -> Platform.runLater(() -> {
            tableModel.setCurrentPage(page);
            tableModel.getRows().setAll(asStream(page).collect(Collectors.toList()));
        }));
    }

    public void onCreateItem(String json){
        createItem(json).thenRun(this::onRefresh);
    }

    public void onUpdateItem(String json){
        updateItem(json).thenRun(this::onRefresh);
    }

    public void onDeleteItem(Item item) {
        delete(item).thenRun(this::onRefresh);
    }

    public void onClearFilters(){
        tableModel.getAttributeFilterMap().values().forEach(simpleStringProperty -> simpleStringProperty.set(null));
        onRefresh();
    }

    // fixme DynamoDB methods
    private CompletableFuture<Page<Item, ?>> queryPageItems() {
        SimpleStringProperty hashValueProperty = tableModel.getAttributeFilterMap().get(tableModel.getHashAttribute());
        if (hashValueProperty != null && !StringUtils.isNullOrEmpty(hashValueProperty.get())) {
            return queryItems(tableModel.getAttributeFilterMap());
        }
        return scanItems(tableModel.getAttributeFilterMap());
    }

    private CompletableFuture<Void> createItem(String json) {
        return CompletableFuture.runAsync(() -> table.putItem(Item.fromJSON(json)));
    }

    private CompletableFuture<Void> updateItem(String json) {
        return CompletableFuture.runAsync(() -> table.putItem(Item.fromJSON(json)));
    }

    private CompletableFuture<Page<Item, ?>> scanItems(Map<String, SimpleStringProperty> attributeFilterMap) {
        return CompletableFuture.supplyAsync(() -> {
            ScanSpec scanSpec = new ScanSpec();
            List<ScanFilter> filters = attributeFilterMap.entrySet().stream()
                    .filter(entry -> Objects.nonNull(entry.getValue().get()) && entry.getValue().get().trim().length() > 0)
                    .map(entry -> new ScanFilter(entry.getKey()).eq(entry.getValue().get()))
                    .collect(Collectors.toList());
            if (!filters.isEmpty()) {
                scanSpec.withScanFilters(filters.toArray(new ScanFilter[]{}));
            }
            return table.scan(scanSpec.withMaxPageSize(PAGE_SIZE)).firstPage();
        });
    }

    private CompletableFuture<Page<Item, ?>> queryItems(Map<String, SimpleStringProperty> attributeFilterMap) {
        return CompletableFuture.supplyAsync(() -> {
            QuerySpec querySpec = new QuerySpec();
            querySpec.withHashKey(tableModel.getHashAttribute(), attributeFilterMap.get(tableModel.getHashAttribute()).get());
            if (tableModel.getRangeAttribute() != null && !StringUtils.isNullOrEmpty(attributeFilterMap.get(tableModel.getRangeAttribute()).get())) {
                querySpec.withRangeKeyCondition(new RangeKeyCondition(tableModel.getRangeAttribute()).eq(attributeFilterMap.get(tableModel.getRangeAttribute()).get()));
            }
            List<QueryFilter> filters = attributeFilterMap.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(tableModel.getHashAttribute()) && !entry.getKey().equals(tableModel.getRangeAttribute()))
                    .filter(entry -> !StringUtils.isNullOrEmpty(entry.getValue().get()))
                    .map(entry -> new QueryFilter(entry.getKey()).eq(entry.getValue().get()))
                    .collect(Collectors.toList());
            if (!filters.isEmpty()) {
                querySpec.withQueryFilters(filters.toArray(new QueryFilter[]{}));
            }
            return table.query(querySpec.withMaxPageSize(PAGE_SIZE)).firstPage();
        });
    }

    public CompletableFuture<Void> delete(Item item) {
        return CompletableFuture.runAsync(() -> {
            if (tableModel.getRangeAttribute() == null) {
                table.deleteItem(tableModel.getHashAttribute(), item.get(tableModel.getHashAttribute()));
            } else {
                table.deleteItem(tableModel.getHashAttribute(), item.get(tableModel.getHashAttribute()), tableModel.getRangeAttribute(), item.get(tableModel.getRangeAttribute()));
            }
        });
    }
}
