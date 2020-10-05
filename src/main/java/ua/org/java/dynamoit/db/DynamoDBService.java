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
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DynamoDBService {

    private final ProfilesConfigFile profilesConfigFile = new ProfilesConfigFile();
    private final Map<String, AmazonDynamoDB> profileDynamoDBClientMap = new HashMap<>();
    private final Map<String, DynamoDB> profileDocumentClientMap = new HashMap<>();

    public Set<String> getAvailableProfiles() {
        return profilesConfigFile.getAllBasicProfiles().keySet();
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
