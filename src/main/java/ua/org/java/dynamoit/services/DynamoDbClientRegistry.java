package ua.org.java.dynamoit.services;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import ua.org.java.dynamoit.model.profile.LocalProfileDetails;
import ua.org.java.dynamoit.model.profile.PreconfiguredProfileDetails;
import ua.org.java.dynamoit.model.profile.ProfileDetails;
import ua.org.java.dynamoit.model.profile.RemoteProfileDetails;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages aws dynamodb clients for profiles
 */
public class DynamoDbClientRegistry {

    private final Map<Integer, DynamoDbClient> profileDynamoDBClientMap = new HashMap<>();
    private final Map<Integer, DynamoDbEnhancedClient> profileDocumentClientMap = new HashMap<>();

    public DynamoDbClient getOrCreateDynamoDBClient(ProfileDetails profileDetails) {
        return profileDynamoDBClientMap.computeIfAbsent(profileDetails.hashCode(), __ -> switch (profileDetails) {
            case PreconfiguredProfileDetails p -> DynamoDbClient.builder()
                    .credentialsProvider(ProfileCredentialsProvider.create(p.getName()))
                    .region(Region.of(p.getRegion()))
                    .build();
            case LocalProfileDetails p -> DynamoDbClient.builder()
                    .endpointOverride(URI.create(p.getEndPoint()))
                    .build();
            case RemoteProfileDetails p -> DynamoDbClient.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(p.getAccessKeyId(), p.getSecretKey())))
                    .region(Region.of(p.getRegion()))
                    .build();
            default -> throw new RuntimeException("That profile details is not supported");
        });
    }

    public DynamoDbEnhancedClient getOrCreateDocumentClient(ProfileDetails profileDetails) {
        return profileDocumentClientMap.computeIfAbsent(profileDetails.hashCode(), key -> DynamoDbEnhancedClient.builder()
                .dynamoDbClient(getOrCreateDynamoDBClient(profileDetails))
                .build());
    }

}
