package ua.org.java.dynamoit.db;

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

public class DynamoDBService {

    private ProfilesConfigFile profilesConfigFile = new ProfilesConfigFile();
    private Map<String, AmazonDynamoDB> profileDynamoDBClientMap = new HashMap<>();
    private Map<String, DynamoDB> profileDocumentClientMap = new HashMap<>();

    public CompletableFuture<Set<String>> getAvailableProfiles() {
        return CompletableFuture.supplyAsync(() -> profilesConfigFile.getAllBasicProfiles().keySet());
    }

    public CompletableFuture<List<String>> getListOfTables(String profile) {
        return CompletableFuture.supplyAsync(() -> getOrCreateDynamoDBClient(profile).listTables().getTableNames());
    }

    public AmazonDynamoDB getOrCreateDynamoDBClient(String profileName) {
        AmazonDynamoDB dynamoDB = profileDynamoDBClientMap.get(profileName);
        if (dynamoDB == null) {
            dynamoDB = AmazonDynamoDBClientBuilder.standard().withCredentials(new ProfileCredentialsProvider(profileName)).build();
            profileDynamoDBClientMap.put(profileName, dynamoDB);
        }
        return dynamoDB;
    }

    public DynamoDB getOrCreateDocumentClient(String profileName) {
        DynamoDB dynamoDB = profileDocumentClientMap.get(profileName);
        if (dynamoDB == null) {
            dynamoDB = new DynamoDB(getOrCreateDynamoDBClient(profileName));
            profileDocumentClientMap.put(profileName, dynamoDB);
        }
        return dynamoDB;
    }

}
