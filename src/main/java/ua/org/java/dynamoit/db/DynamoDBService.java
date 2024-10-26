/*
 * This file is part of DynamoIt.
 *
 *     DynamoIt is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     DynamoIt is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with DynamoIt.  If not, see <https://www.gnu.org/licenses/>.
 */

package ua.org.java.dynamoit.db;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import ua.org.java.dynamoit.model.profile.LocalProfileDetails;
import ua.org.java.dynamoit.model.profile.PreconfiguredProfileDetails;
import ua.org.java.dynamoit.model.profile.ProfileDetails;
import ua.org.java.dynamoit.model.profile.RemoteProfileDetails;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static ua.org.java.dynamoit.utils.RegionsUtils.DEFAULT_REGION;

public class DynamoDBService {

    private final Map<Integer, DynamoDbClient> profileDynamoDBClientMap = new HashMap<>();
    private final Map<Integer, DynamoDbEnhancedClient> profileDocumentClientMap = new HashMap<>();

    public Stream<ProfileDetails> getAvailableProfiles() {
        // config file contains profile and region values
        return ProfileFile.defaultProfileFile().profiles()
                .values()
                .stream()
                .map(profile -> new PreconfiguredProfileDetails(profile.name(), profile.property("region").orElse(DEFAULT_REGION)));
    }

    public CompletableFuture<List<String>> getListOfTables(ProfileDetails profileDetails) {
        return CompletableFuture.supplyAsync(() -> {

            List<String> tableNames = new ArrayList<>();
            DynamoDbClient dbClient = getOrCreateDynamoDBClient(profileDetails);
            var listTablesResult = dbClient.listTablesPaginator();
            listTablesResult
                    .stream()
                    .iterator()
                    .forEachRemaining(listTablesResponse -> tableNames.addAll(listTablesResponse.tableNames()));

            return tableNames;
        });
    }

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
