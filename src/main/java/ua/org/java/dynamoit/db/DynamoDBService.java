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

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfilesConfigFile;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.profile.path.AwsProfileFileLocationProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import ua.org.java.dynamoit.model.profile.LocalProfileDetails;
import ua.org.java.dynamoit.model.profile.PreconfiguredProfileDetails;
import ua.org.java.dynamoit.model.profile.ProfileDetails;
import ua.org.java.dynamoit.model.profile.RemoteProfileDetails;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ua.org.java.dynamoit.utils.RegionsUtils.ALL_REGIONS;

public class DynamoDBService {

    private final Map<Integer, AmazonDynamoDB> profileDynamoDBClientMap = new HashMap<>();
    private final Map<Integer, DynamoDB> profileDocumentClientMap = new HashMap<>();

    public Stream<ProfileDetails> getAvailableProfiles() {
        Function<String, String> cutProfilePrefix = profileName -> profileName.startsWith("profile ") ? profileName.substring(8).trim() : profileName;
        // config file contains profile and region values
        Map<String, ProfileDetails> profileMap = new ProfilesConfigFile(AwsProfileFileLocationProvider.DEFAULT_CONFIG_LOCATION_PROVIDER.getLocation()).getAllBasicProfiles()
                .values()
                .stream()
                .map(profile -> new PreconfiguredProfileDetails(cutProfilePrefix.apply(profile.getProfileName()), profile.getRegion()))
                .collect(Collectors.toMap(ProfileDetails::getName, profile -> profile));

        // by default it uses credentials config with contains profile and access keys
        return new ProfilesConfigFile().getAllBasicProfiles()
                .values()
                .stream()
                .map(profile -> cutProfilePrefix.apply(profile.getProfileName()))
                .map(profileName -> profileMap.computeIfAbsent(profileName, __ -> new PreconfiguredProfileDetails(profileName, ALL_REGIONS.get(0))));
    }

    public CompletableFuture<List<String>> getListOfTables(ProfileDetails profileDetails) {
        return CompletableFuture.supplyAsync(() -> {
            String lastEvaluatedTableName = null;
            List<String> tableNames = new ArrayList<>();
            do {
                AmazonDynamoDB dbClient = getOrCreateDynamoDBClient(profileDetails);
                ListTablesResult listTablesResult = lastEvaluatedTableName == null ? dbClient.listTables() : dbClient.listTables(lastEvaluatedTableName);
                lastEvaluatedTableName = listTablesResult.getLastEvaluatedTableName();
                tableNames.addAll(listTablesResult.getTableNames());
            } while (lastEvaluatedTableName != null);

            return tableNames;
        });
    }

    public AmazonDynamoDB getOrCreateDynamoDBClient(ProfileDetails profileDetails) {
        return profileDynamoDBClientMap.computeIfAbsent(profileDetails.hashCode(), __ -> {
            if (profileDetails instanceof PreconfiguredProfileDetails p) {
                return AmazonDynamoDBClientBuilder.standard()
                        .withCredentials(new ProfileCredentialsProvider(p.getName()))
                        .withRegion(p.getRegion())
                        .build();
            } else if (profileDetails instanceof LocalProfileDetails p) {
                return AmazonDynamoDBClientBuilder.standard()
                        .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(p.getEndPoint(), ""))
                        .build();
            } else if (profileDetails instanceof RemoteProfileDetails p) {
                return AmazonDynamoDBClientBuilder.standard()
                        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(p.getAccessKeyId(), p.getSecretKey())))
                        .withRegion(p.getRegion())
                        .build();
            }
            throw new RuntimeException("That profile details is not supported");
        });
    }

    public DynamoDB getOrCreateDocumentClient(ProfileDetails profileDetails) {
        return profileDocumentClientMap.computeIfAbsent(profileDetails.hashCode(), key -> new DynamoDB(getOrCreateDynamoDBClient(profileDetails)));
    }

}
