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

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

import java.util.stream.Stream;

public class DynamoDbTableService {

    private static final int WRITE_CHUNK_SIZE = 25;

    private final DynamoDbClient dbClient;
    private final DynamoDbEnhancedClient enhancedClient;
    private final String tableName;

    public DynamoDbTableService(DynamoDbClient dbClient, DynamoDbEnhancedClient enhancedClient, String tableName) {
        this.dbClient = dbClient;
        this.enhancedClient = enhancedClient;
        this.tableName = tableName;
    }

    public TableDescription describeTable() {
        return dbClient.describeTable(DescribeTableRequest.builder().tableName(tableName).build()).table();
    }

    public void batchWrite(Stream<EnhancedDocument> documents) {
//        documents
//                .collect()
//                .map(list -> {
//                    WriteBatch.Builder<EnhancedDocument> writeBatchBuilder = WriteBatch.builder(EnhancedDocument.class).mappedTableResource(table);
//                    list.forEach(writeBatchBuilder::addPutItem);
//
//                    return writeBatchBuilder.build();
//                })
//                .forEach(wb -> enhancedClient.batchWriteItem(r -> r.writeBatches(wb)));
    }


}
