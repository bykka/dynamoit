package ua.org.java.dynamoit.table;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import ua.org.java.dynamoit.db.DynamoDBService;

import java.util.concurrent.CompletableFuture;

public class TableController {

    private AmazonDynamoDB dbClient;
    private DynamoDB documentClient;
    private TableContext context;

    public TableController(TableContext context, DynamoDBService dynamoDBService) {
        this.context = context;
        dbClient = dynamoDBService.getOrCreateDynamoDBClient(context.getProfileName());
        documentClient = dynamoDBService.getOrCreateDocumentClient(context.getProfileName());
    }

    public CompletableFuture<ItemCollection<ScanOutcome>> scanItems() {
        Table table = documentClient.getTable(context.getTableName());
        return CompletableFuture.supplyAsync(() -> table.scan(new ScanSpec().withMaxPageSize(100)));
    }

    public CompletableFuture<ItemCollection<QueryOutcome>> queryItems() {
        Table table = documentClient.getTable(context.getTableName());
        return CompletableFuture.supplyAsync(() -> table.query(new QuerySpec().withMaxPageSize(100)));
    }

    public CompletableFuture<DescribeTableResult> describeTable() {
        return CompletableFuture.supplyAsync(() -> dbClient.describeTable(context.getTableName()));
    }



}
