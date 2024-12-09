package ua.org.java.dynamoit.services;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

public class DynamoDbTableService {

    private final DynamoDbClient dbClient;
    private final String tableName;

    public DynamoDbTableService(DynamoDbClient dbClient, String tableName) {
        this.dbClient = dbClient;
        this.tableName = tableName;
    }

    public TableDescription describeTable() {
        return dbClient.describeTable(DescribeTableRequest.builder().tableName(tableName).build()).table();
    }

}
