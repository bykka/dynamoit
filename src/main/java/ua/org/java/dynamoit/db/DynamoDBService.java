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

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfilesConfigFile;
import com.amazonaws.profile.path.AwsProfileFileLocationProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import ua.org.java.dynamoit.model.profile.PreconfiguredProfileDetails;
import ua.org.java.dynamoit.model.profile.ProfileDetails;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ua.org.java.dynamoit.model.Regions.ALL_REGIONS;

public class DynamoDBService {

    private final Map<String, AmazonDynamoDB> profileDynamoDBClientMap = new HashMap<>();
    private final Map<String, DynamoDB> profileDocumentClientMap = new HashMap<>();

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

    public CompletableFuture<List<String>> getListOfTables(String profile, String region) {
        return CompletableFuture.supplyAsync(() -> {
            String lastEvaluatedTableName = null;
            List<String> tableNames = new ArrayList<>();
            do {
                AmazonDynamoDB dbClient = getOrCreateDynamoDBClient(profile, region);
                ListTablesResult listTablesResult = lastEvaluatedTableName == null ? dbClient.listTables() : dbClient.listTables(lastEvaluatedTableName);
                lastEvaluatedTableName = listTablesResult.getLastEvaluatedTableName();
                tableNames.addAll(listTablesResult.getTableNames());
            } while (lastEvaluatedTableName != null);

            return tableNames;
        });
    }

    public AmazonDynamoDB getOrCreateDynamoDBClient(String profileName, String region) {
        return profileDynamoDBClientMap.computeIfAbsent(profileName + region, __ -> {
            AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard()
                    .withCredentials(new ProfileCredentialsProvider(profileName))
                    .withRegion(region);

            return builder.build();
        });
    }

    public DynamoDB getOrCreateDocumentClient(String profileName, String region) {
        return profileDocumentClientMap.computeIfAbsent(profileName + region, key -> new DynamoDB(getOrCreateDynamoDBClient(profileName, region)));
    }

}
