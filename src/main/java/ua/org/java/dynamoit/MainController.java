package ua.org.java.dynamoit;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfilesConfigFile;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class MainController {

    private ProfilesConfigFile profilesConfigFile = new ProfilesConfigFile();
    private Map<String, AmazonDynamoDB> profileDynamoDBClientMap = new HashMap<>();
    private Map<String, DynamoDB> profileDocumentClientMap = new HashMap<>();

    public CompletableFuture<Set<String>> getAvailableProfiles() {
        return CompletableFuture.supplyAsync(() -> profilesConfigFile.getAllBasicProfiles().keySet());
    }

    public CompletableFuture<List<String>> getListOfTables(String profile) {
        AmazonDynamoDB dynamoDB = getOrCreateDynamoDBClient(profile);

        return CompletableFuture.supplyAsync(() -> dynamoDB.listTables().getTableNames());
    }

    private AmazonDynamoDB getOrCreateDynamoDBClient(String profileName) {
        AmazonDynamoDB dynamoDB = profileDynamoDBClientMap.get(profileName);
        if (dynamoDB == null) {
            dynamoDB = AmazonDynamoDBClientBuilder.standard().withCredentials(new ProfileCredentialsProvider(profileName)).build();
            profileDynamoDBClientMap.put(profileName, dynamoDB);
        }
        return dynamoDB;
    }

    private DynamoDB getOrCreateDocumentClient(String profileName) {
        DynamoDB dynamoDB = profileDocumentClientMap.get(profileName);
        if (dynamoDB == null) {
            dynamoDB = new DynamoDB(getOrCreateDynamoDBClient(profileName));
            profileDocumentClientMap.put(profileName, dynamoDB);
        }
        return dynamoDB;
    }
//
//    fun scanItems(tableName: String): ItemCollection<ScanOutcome>? {
//        val table = documentClient.getTable(tableName)
//        return table.scan(ScanSpec().withMaxPageSize(100))
//    }
//
//    fun describeTable(tableName: String): DescribeTableResult? {
//        return dynamoDbClient.describeTable(tableName)
//    }

}
