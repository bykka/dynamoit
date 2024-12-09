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

package ua.org.java.dynamoit.services;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import ua.org.java.dynamoit.model.profile.ProfileDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DynamoDbService {

    private final DynamoDbClientRegistry dbClientRegistry;

    public DynamoDbService(DynamoDbClientRegistry dbClientRegistry) {
        this.dbClientRegistry = dbClientRegistry;
    }

    public CompletableFuture<List<String>> getListOfTables(ProfileDetails profileDetails) {
        return CompletableFuture.supplyAsync(() -> {

            List<String> tableNames = new ArrayList<>();
            DynamoDbClient dbClient = dbClientRegistry.getOrCreateDynamoDBClient(profileDetails);
            var listTablesResult = dbClient.listTablesPaginator();
            listTablesResult
                    .stream()
                    .iterator()
                    .forEachRemaining(listTablesResponse -> tableNames.addAll(listTablesResponse.tableNames()));

            return tableNames;
        });
    }

}
