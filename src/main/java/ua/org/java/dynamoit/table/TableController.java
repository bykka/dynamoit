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

public class TableController {

    private AmazonDynamoDB dbClient;
    private DynamoDB documentClient;
    private Table table;
    private TableContext context;
    private TableModel tableModel;
    private String hashAttribute;
    private String rangeAttribute;

    public TableController(TableContext context, TableModel tableModel, DynamoDBService dynamoDBService) {
        this.context = context;
        this.tableModel = tableModel;

        dbClient = dynamoDBService.getOrCreateDynamoDBClient(context.getProfileName());
        documentClient = dynamoDBService.getOrCreateDocumentClient(context.getProfileName());
        table = documentClient.getTable(context.getTableName());

        CompletableFuture.supplyAsync(() -> dbClient.describeTable(context.getTableName()))
                .thenAccept(describeTable -> {
                    Utils.getHashKey(describeTable).ifPresent(s -> hashAttribute = s);
                    Utils.getRangeKey(describeTable).ifPresent(s -> rangeAttribute = s);
                    Platform.runLater(() -> {
                        tableModel.setDescribeTableResult(describeTable);
                    });
                });
    }

    public CompletableFuture<Page<Item, ?>> queryPageItems(Map<String, SimpleStringProperty> attributeFilterMap) {
        SimpleStringProperty hashValueProperty = attributeFilterMap.get(hashAttribute);
        if (hashValueProperty != null && !StringUtils.isNullOrEmpty(hashValueProperty.get())) {
            return queryItems(attributeFilterMap);
        }
        return scanItems(attributeFilterMap);
    }

    public CompletableFuture<Void> createItem(String json) {
        return CompletableFuture.runAsync(() -> table.putItem(Item.fromJSON(json)));
    }

    public CompletableFuture<Void> updateItem(String json) {
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
            return table.scan(scanSpec.withMaxPageSize(100)).firstPage();
        });
    }

    private CompletableFuture<Page<Item, ?>> queryItems(Map<String, SimpleStringProperty> attributeFilterMap) {
        return CompletableFuture.supplyAsync(() -> {
            QuerySpec querySpec = new QuerySpec();
            querySpec.withHashKey(hashAttribute, attributeFilterMap.get(hashAttribute).get());
            if (!StringUtils.isNullOrEmpty(attributeFilterMap.get(rangeAttribute).get())) {
                querySpec.withRangeKeyCondition(new RangeKeyCondition(rangeAttribute).eq(attributeFilterMap.get(rangeAttribute).get()));
            }
            List<QueryFilter> filters = attributeFilterMap.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(hashAttribute) && !entry.getKey().equals(rangeAttribute))
                    .filter(entry -> !StringUtils.isNullOrEmpty(entry.getValue().get()))
                    .map(entry -> new QueryFilter(entry.getKey()).eq(entry.getValue().get()))
                    .collect(Collectors.toList());
            if (!filters.isEmpty()) {
                querySpec.withQueryFilters(filters.toArray(new QueryFilter[]{}));
            }
            return table.query(querySpec.withMaxPageSize(100)).firstPage();
        });
    }

    public CompletableFuture<Void> delete(Item item) {
        return CompletableFuture.runAsync(() -> {
            if (rangeAttribute == null) {
                table.deleteItem(hashAttribute, item.get(hashAttribute));
            } else {
                table.deleteItem(hashAttribute, item.get(hashAttribute), rangeAttribute, item.get(rangeAttribute));
            }
        });
    }
}
