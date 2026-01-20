package ua.org.java.dynamoit;

import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;

public abstract class DynamoDBTest {

    protected static DynamoDBProxyServer server;

    @BeforeAll
    public static void beforeAll() throws Exception {
        server = ServerRunner.createServerFromCommandLineArgs(new String[]{"-inMemory"});
        server.start();

        try (DynamoDbClient dbClient = DynamoDbClient.builder().endpointOverride(URI.create("http://localhost:8000")).build()) {
            dbClient.createTable(CreateTableRequest.builder()
                    .tableName("Users")
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("id")
                                    .attributeType("S")
                                    .build())
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("id")
                            .keyType(KeyType.HASH)
                            .build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(1L)
                            .writeCapacityUnits(1L)
                            .build())
                    .build());

            dbClient.putItem(PutItemRequest.builder()
                            .tableName("Users")
                            .item(EnhancedDocument.fromJson("""
                                    {
                                        "id": "1",
                                        "name": "John"
                                    }
                                    """).toMap())
                    .build());
            dbClient.putItem(PutItemRequest.builder()
                            .tableName("Users")
                            .item(EnhancedDocument.fromJson("""
                                    {
                                        "id": "2",
                                        "name": "William"
                                    }
                                    """).toMap())
                    .build());
        }
    }

    @AfterAll
    public static void afterAll() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

}
