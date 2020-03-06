package ua.org.java.dynamoit.table;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import javafx.beans.property.SimpleStringProperty;
import ua.org.java.dynamoit.db.DynamoDBService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TableController {

    private AmazonDynamoDB dbClient;
    private DynamoDB documentClient;
    private TableContext context;
    private CompletableFuture<DescribeTableResult> describeTableResult;

    public TableController(TableContext context, DynamoDBService dynamoDBService) {
        this.context = context;
        dbClient = dynamoDBService.getOrCreateDynamoDBClient(context.getProfileName());
        documentClient = dynamoDBService.getOrCreateDocumentClient(context.getProfileName());

        describeTableResult = CompletableFuture.supplyAsync(() -> dbClient.describeTable(context.getTableName()));
    }

    private CompletableFuture<ItemCollection<ScanOutcome>> scanItems(Map<String, SimpleStringProperty> attributeFilterMap) {
        Table table = documentClient.getTable(context.getTableName());
        return CompletableFuture.supplyAsync(() -> {
            ScanSpec scanSpec = new ScanSpec();
            List<ScanFilter> filters = attributeFilterMap.entrySet().stream()
                    .filter(entry -> Objects.nonNull(entry.getValue().get()) && entry.getValue().get().trim().length() > 0)
                    .map(entry -> new ScanFilter(entry.getKey()).eq(entry.getValue().get()))
                    .collect(Collectors.toList());
            if (!filters.isEmpty()) {
                scanSpec.withScanFilters(filters.toArray(new ScanFilter[]{}));
            }
            return table.scan(scanSpec.withMaxPageSize(100));
        });
    }

    public CompletableFuture<Page<Item, ?>> queryPageItems(Map<String, SimpleStringProperty> attributeFilterMap) {
        Table table = documentClient.getTable(context.getTableName());

        return CompletableFuture.supplyAsync(() -> {
            ScanSpec scanSpec = new ScanSpec();
            List<ScanFilter> filters = attributeFilterMap.entrySet().stream()
                    .filter(entry -> Objects.nonNull(entry.getValue().get()) && entry.getValue().get().trim().length() > 0)
                    .map(entry -> new ScanFilter(entry.getKey()).eq(entry.getValue().get()))
                    .collect(Collectors.toList());
            if (!filters.isEmpty()) {
                scanSpec.withScanFilters(filters.toArray(new ScanFilter[]{}));
            }
            ItemCollection<ScanOutcome> scan = table.scan(scanSpec.withMaxPageSize(100));
            return scan.firstPage();
        });
    }

    private CompletableFuture<ItemCollection<QueryOutcome>> queryItems(Map<String, SimpleStringProperty> attributeFilterMap) {
        Table table = documentClient.getTable(context.getTableName());
        return CompletableFuture.supplyAsync(() -> {
            QuerySpec querySpec = new QuerySpec();
            List<QueryFilter> filters = attributeFilterMap.entrySet().stream()
                    .filter(entry -> Objects.nonNull(entry.getValue().get()) && entry.getValue().get().trim().length() > 0)
                    .map(entry -> new QueryFilter(entry.getKey()).eq(entry.getValue().get()))
                    .collect(Collectors.toList());
            if (!filters.isEmpty()) {
                querySpec.withQueryFilters(filters.toArray(new QueryFilter[]{}));
            }
            return table.query(querySpec.withMaxPageSize(100));
        });
    }

    public CompletableFuture<DescribeTableResult> describeTable() {
        return describeTableResult;
    }


}
