package ua.org.java.dynamoit.table;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
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

    public CompletableFuture<DescribeTableResult> describeTable() {
        return CompletableFuture.supplyAsync(() -> dbClient.describeTable(context.getTableName()));
    }



}
