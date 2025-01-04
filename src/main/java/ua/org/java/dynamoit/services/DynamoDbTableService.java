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

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.document.DocumentTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import ua.org.java.dynamoit.model.profile.ProfileDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static ua.org.java.dynamoit.utils.Utils.getHashKey;
import static ua.org.java.dynamoit.utils.Utils.getRangeKey;

public class DynamoDbTableService {

    private static final int BATCH_SIZE = 25;
    private static final int BASE_DELAY_MS = 100;

    private TableDescription tableDescription;
    private DynamoDbTable<EnhancedDocument> table;
    private final DynamoDbClient dbClient;
    private final DynamoDbEnhancedClient enhancedClient;
    private final String tableName;

    private static final Map<Integer, DynamoDbTableService> INSTANCES = new HashMap<>();

    public static DynamoDbTableService getOrCreate(ProfileDetails profileDetails, String tableName, DynamoDbClientRegistry dbClientRegistry) {
        return INSTANCES.computeIfAbsent((profileDetails.toString() + tableName).hashCode(), integer -> {
            DynamoDbTableService service = new DynamoDbTableService(dbClientRegistry.getOrCreateDynamoDBClient(profileDetails), dbClientRegistry.getOrCreateDocumentClient(profileDetails), tableName);

            service.init();

            return service;
        });
    }

    private DynamoDbTableService(DynamoDbClient dbClient, DynamoDbEnhancedClient enhancedClient, String tableName) {
        this.dbClient = dbClient;
        this.enhancedClient = enhancedClient;
        this.tableName = tableName;
    }

    public void init() {
        tableDescription = dbClient.describeTable(DescribeTableRequest.builder().tableName(tableName).build()).table();
        buildTableScheme();
    }

    public TableDescription getTableDescription() {
        return this.tableDescription;
    }

    public void save(List<EnhancedDocument> documents) {
        batchWriteFunction(documents, WriteBatch.Builder::addPutItem);
    }

    public void delete(List<EnhancedDocument> documents) {
        batchWriteFunction(documents, WriteBatch.Builder::addDeleteItem);
    }

    private void batchWriteFunction(List<EnhancedDocument> documents, BiConsumer<WriteBatch.Builder<EnhancedDocument>, EnhancedDocument> documentConsumer) {
        BiFunction<List<EnhancedDocument>, Integer, List<EnhancedDocument>> executeBatchFunction = (batch, tryCount) -> Flowable.fromIterable(batch)
                .buffer(BATCH_SIZE)
                .map(list -> {
                    WriteBatch.Builder<EnhancedDocument> writeBatchBuilder = WriteBatch.builder(EnhancedDocument.class).mappedTableResource(table);
                    list.forEach(doc -> documentConsumer.accept(writeBatchBuilder, doc));

                    return writeBatchBuilder.build();
                })
                .delay(Double.valueOf(BASE_DELAY_MS * Math.pow(2, tryCount)).longValue(), TimeUnit.MILLISECONDS)
                .parallel()
                .runOn(Schedulers.io())
                .map(wb -> enhancedClient.batchWriteItem(r -> r.writeBatches(wb)))
                .flatMap(result -> Flowable.fromIterable(result.unprocessedPutItemsForTable(table)))
                .sequential()
                .toList()
                .blockingGet();

        int tryCount = 0;

        for(List<EnhancedDocument> docs = documents; !docs.isEmpty(); tryCount++) {
            docs = executeBatchFunction.apply(docs, tryCount);
        }
    }


    private void buildTableScheme() {
        DocumentTableSchema.Builder schemaBuilder = TableSchema.documentSchemaBuilder()
                .addIndexPartitionKey(TableMetadata.primaryIndexName(), hash(), AttributeValueType.S);

        range().ifPresent(key -> schemaBuilder.addIndexSortKey(TableMetadata.primaryIndexName(), key, AttributeValueType.S));

        getFullProjectedIndexes()
                .forEach(indexDescription -> {
                    indexDescription.keySchema().forEach(key -> {
                        if (key.keyType() == KeyType.HASH) {
                            schemaBuilder.addIndexPartitionKey(indexDescription.indexName(), key.attributeName(), AttributeValueType.S);
                        } else if (key.keyType() == KeyType.RANGE) {
                            schemaBuilder.addIndexSortKey(indexDescription.indexName(), key.attributeName(), AttributeValueType.S);
                        }
                    });
                });

        table = enhancedClient.table(tableName, schemaBuilder.build());
    }

    private String hash() {
        return getHashKey(tableDescription).orElse("");
    }

    private Optional<String> range() {
        return getRangeKey(tableDescription);
    }

    private Stream<GlobalSecondaryIndexDescription> getFullProjectedIndexes() {
        return tableDescription.globalSecondaryIndexes()
                .stream()
                .filter(indexDescription -> indexDescription.projection().projectionType() == ProjectionType.ALL);
    }

}
