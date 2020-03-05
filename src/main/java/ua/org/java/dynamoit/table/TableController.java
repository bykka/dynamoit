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

    public TableController(TableContext context, DynamoDBService dynamoDBService) {
        this.context = context;
        dbClient = dynamoDBService.getOrCreateDynamoDBClient(context.getProfileName());
        documentClient = dynamoDBService.getOrCreateDocumentClient(context.getProfileName());
    }

    public CompletableFuture<ItemCollection<ScanOutcome>> scanItems(Map<String, SimpleStringProperty> attributeFilterMap) {
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

    public CompletableFuture<ItemCollection<QueryOutcome>> queryItems() {
        Table table = documentClient.getTable(context.getTableName());
        return CompletableFuture.supplyAsync(() -> table.query(new QuerySpec().withMaxPageSize(100)));
    }

    public CompletableFuture<DescribeTableResult> describeTable() {
        return CompletableFuture.supplyAsync(() -> dbClient.describeTable(context.getTableName()));
    }


}
