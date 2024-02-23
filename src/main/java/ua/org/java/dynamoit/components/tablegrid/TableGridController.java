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

package ua.org.java.dynamoit.components.tablegrid;

import com.amazonaws.services.dynamodbv2.document.ScanFilter;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Observable;
import javafx.application.HostServices;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.Pair;
import org.reactfx.EventStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import ua.org.java.dynamoit.EventBus;
import ua.org.java.dynamoit.components.tablegrid.parser.expression.FilterExpressionBuilder;
import ua.org.java.dynamoit.db.DynamoDBService;
import ua.org.java.dynamoit.model.TableDef;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static ua.org.java.dynamoit.components.tablegrid.Attributes.*;
import static ua.org.java.dynamoit.utils.Utils.*;

public class TableGridController {

    private static final Logger LOG = Logger.getLogger(TableGridController.class.getName());

    /**
     * Maximum number of items in one page
     */
    private static final int PAGE_SIZE = 100;

    /**
     * Maximum number of items for one batch put request
     */
    private static final int BATCH_SIZE = 25;

    private final DynamoDbClient dbClient;
    private final DynamoDbTable<EnhancedDocument> table;
    private final TableGridContext context;
    private final TableGridModel tableModel;
    private final EventBus eventBus;
    private final Executor uiExecutor;
    private final HostServices hostServices;
    private final DynamoDbEnhancedClient documentClient;

    public TableGridController(TableGridContext context,
                               TableGridModel tableModel,
                               DynamoDBService dynamoDBService,
                               EventBus eventBus,
                               Executor uiExecutor,
                               HostServices hostServices
    ) {
        this.context = context;
        this.tableModel = tableModel;
        this.eventBus = eventBus;
        this.uiExecutor = uiExecutor;
        this.hostServices = hostServices;

        tableModel.getProfileModel().getAvailableTables().stream()
                .filter(tableDef -> tableDef.getName().equals(context.tableName()))
                .findFirst()
                .ifPresent(tableModel::setTableDef);

        tableModel.setTableName(context.tableName());
        tableModel.setProfile(context.tableName());

        dbClient = dynamoDBService.getOrCreateDynamoDBClient(context.profileDetails());
        documentClient = dynamoDBService.getOrCreateDocumentClient(context.profileDetails());
        table = documentClient.table(context.tableName(), TableSchema.documentSchemaBuilder().build());
    }

    public void init() {
        eventBus.activity(
                supplyAsync(() -> {
                    if (tableModel.getOriginalTableDescription() == null) {
                        return supplyAsync(() -> dbClient.describeTable(DescribeTableRequest.builder().tableName(context.tableName()).build()).table())
                                .thenAcceptAsync(this::bindToModel, uiExecutor);
                    } else {
                        bindToModel(tableModel.getTableDef());
                        return CompletableFuture.completedFuture(Boolean.TRUE);
                    }
                })
                        .thenCompose(__ -> __)
                        .thenRun(this::applyContext)
                        .thenCompose(__ -> queryPageItems())
                        .thenAcceptAsync(this::bindToModel, uiExecutor)
        );
    }

    public void onReachScrollEnd() {
        if (tableModel.getPageIterator().hasNext()) {
            eventBus.activity(
                    supplyAsync(tableModel::getPageIterator)
                            .thenApply(TableGridController::iteratePage)
                            .thenAcceptAsync(pair -> {
                                tableModel.getTableDef().getAttributeTypesMap().putAll(defineAttributesTypes(pair.getKey()));
//                                tableModel.setPageIterator(pair.getValue());
                                tableModel.getRows().addAll(pair.getKey());
                            }, uiExecutor)
            );
        }
    }

    public CompletableFuture<Void> onRefreshData() {
        return eventBus.activity(
                runAsync(() -> tableModel.getRows().clear(), uiExecutor)
                        .thenComposeAsync(aVoid -> queryPageItems().thenAcceptAsync(this::bindToModel, uiExecutor))
        );
    }

    public EventStream<Boolean> validateItem(EventStream<String> textStream) {
        return validateItem(textStream, false);
    }

    public EventStream<Boolean> validateItem(EventStream<String> textStream, boolean jsonOnly) {
        return textStream.successionEnds(Duration.ofMillis(100))
                .map(text -> {
                    try {
                        EnhancedDocument item = EnhancedDocument.fromJson(text);

                        if (jsonOnly) {
                            return true;
                        }
                        return item.isPresent(hash()) && (range() == null || item.isPresent(range()));
                    } catch (Exception e) {
                        return false;
                    }
                });
    }

    public void onCreateItem(String json, boolean isRaw) {
        eventBus.activity(
                createItem(json, isRaw).thenRun(this::onRefreshData)
        );
    }

    public void onUpdateItem(String json, boolean isRaw) {
        eventBus.activity(
                updateItem(json, isRaw).thenRun(this::onRefreshData)
        );
    }

    public void onDeleteItems(List<EnhancedDocument> items) {
        eventBus.activity(
                delete(items).thenRun(this::onRefreshData)
        );
    }

    public void onPatchItems(List<EnhancedDocument> items, String jsonPatch, boolean isRaw) {
        eventBus.activity(
                patchItems(items, jsonPatch, isRaw).thenRun(this::onRefreshData)
        );
    }

    public void onClearFilters() {
        tableModel.getAttributeFilterMap().values().forEach(simpleStringProperty -> simpleStringProperty.set(null));
        onRefreshData();
    }

    public void onSaveToFile(File file) {
        eventBus.activity(
                executeQueryOrSearch().thenAccept(items -> {
                    try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                        JsonGenerator generator = new JsonFactory(new ObjectMapper()).createGenerator(writer);
                        generator.writeStartArray();
                        asStream(items).flatMap(page -> page.items().stream()).forEach(o -> {
                            try {
                                generator.writeRawValue(o.toJson());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        generator.writeEndArray();
                        generator.flush();
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                })
        );
    }

    public void onLoadFromFile(File file) {
        eventBus.activity(
                runAsync(() -> {
                    try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                        JsonNode root = new ObjectMapper().readTree(reader);
                        Observable.fromIterable(root::elements)
                                .map(jsonNode -> EnhancedDocument.fromJson(jsonNode.toString()))
                                .buffer(25)
                                .map(list -> {
                                    WriteBatch.Builder<EnhancedDocument> writeBatchBuilder = WriteBatch.builder(EnhancedDocument.class).mappedTableResource(table);
                                    list.forEach(writeBatchBuilder::addPutItem);

                                    return writeBatchBuilder.build();
                                })
                                .subscribe(wb -> documentClient.batchWriteItem(r -> r.writeBatches(wb)));
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                }),
                "Can't load json data from the file",
                "Data in the file is not properly formatted or does not correspond to the db schema."
        ).whenComplete((v, throwable) -> onRefreshData());
    }

    /**
     * Iterate pages to get around PAGE_SIZE number of items if exist
     *
     * @param pages iterator of pages
     * @return list of items and next pages
     */
    private static Pair<List<EnhancedDocument>, Iterator<Page<EnhancedDocument>>> iteratePage(Iterator<Page<EnhancedDocument>> pages) {
        List<EnhancedDocument> items = new ArrayList<>(PAGE_SIZE);
        items.addAll(pages.next().items());

        while (items.size() < PAGE_SIZE && pages.hasNext()) {
            Page<EnhancedDocument> page = pages.next();
            items.addAll(page.items());
        }
        return new Pair<>(items, pages);
    }

    CompletableFuture<Pair<List<EnhancedDocument>, Iterator<Page<EnhancedDocument>>>> queryPageItems() {
        return executeQueryOrSearch()
                .thenApply(Iterable::iterator)
                .thenApply(TableGridController::iteratePage);
    }

    // analyze what kind of request should be executed - query or scan
    private CompletableFuture<SdkIterable<Page<EnhancedDocument>>> executeQueryOrSearch() {
        // query table if hash attribute has filter
        if (notBlankFilterValue(hash())) {
            SimpleStringProperty hashValueProperty = tableModel.getAttributeFilterMap().get(hash());
            // query if hash has eq operation only
            if (FilterExpressionBuilder.isEqualExpression(hashValueProperty.get())) {
                var queryRequest = buildQuerySpec(hash(), range(), tableModel.getAttributeFilterMap());
                return queryTableItems(queryRequest);
            }
        } else if (tableModel.getOriginalTableDescription().globalSecondaryIndexes() != null) {
            // find global indexes with ALL properties projection
            List<GlobalSecondaryIndexDescription> fullProjectionIndexes = tableModel.getOriginalTableDescription().globalSecondaryIndexes().stream()
                    .filter(__ -> __.projection().projectionType().equals(ProjectionType.ALL)).toList();

            // find the first index that has hash and range attributes in the filters map
            Optional<GlobalSecondaryIndexDescription> globalIndexOptional = fullProjectionIndexes.stream()
                    .filter(__ -> __.keySchema().stream()
                            .allMatch(key -> notBlankFilterValue(key.attributeName()))
                    ).findFirst();

            // or try to find at least global index with hash in the filters map
            globalIndexOptional = globalIndexOptional.or(() -> fullProjectionIndexes.stream()
                    .filter(__ -> lookUpKeyName(__.keySchema(), KeyType.HASH).map(this::notBlankFilterValue).orElse(false)).findFirst()
            );

            if (globalIndexOptional.isPresent()) {
                GlobalSecondaryIndexDescription indexDescription = globalIndexOptional.get();

                DynamoDbIndex<EnhancedDocument> index = table.index(indexDescription.indexName());
                Optional<String> indexHash = lookUpKeyName(indexDescription.keySchema(), KeyType.HASH);
                Optional<String> indexRange = lookUpKeyName(indexDescription.keySchema(), KeyType.RANGE);

                if (indexHash.isPresent()) {
                    QueryEnhancedRequest querySpec = buildQuerySpec(indexHash.get(), indexRange.orElse(null), tableModel.getAttributeFilterMap());
                    return queryIndexItems(querySpec, index);
                }
            }
        }
        return scanItems(tableModel.getAttributeFilterMap());
    }

    private CompletableFuture<Void> processItemAsync(String json, boolean isRaw, Consumer<EnhancedDocument> command) {
        try {
            EnhancedDocument item = isRaw ? rawJsonToItem(json) : EnhancedDocument.fromJson(json);
            return runAsync(() -> command.accept(item));
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<Void> createItem(String json, boolean isRaw) {
        return processItemAsync(json, isRaw, table::putItem);
    }

    private CompletableFuture<Void> updateItem(String json, boolean isRaw) {
        return processItemAsync(json, isRaw, table::putItem);
    }

    private CompletableFuture<Void> patchItems(List<EnhancedDocument> items, String jsonPatch, boolean isRaw) {
        if (items.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return processItemAsync(jsonPatch, isRaw, patch -> items.forEach(item -> {
            Map<String, AttributeValue> valuesMap = new HashMap<>(patch.toMap());
            valuesMap.put(hash(), item.toMap().get(hash()));
            if (range() != null) {
                valuesMap.put(hash(), item.toMap().get(range()));
            }

            table.updateItem(EnhancedDocument.fromAttributeValueMap(valuesMap));
        }));
    }

    private CompletableFuture<SdkIterable<Page<EnhancedDocument>>> scanItems(Map<String, SimpleStringProperty> attributeFilterMap) {
        return supplyAsync(() -> {
            ScanEnhancedRequest.Builder scanSpec = ScanEnhancedRequest.builder();

            Expression.Builder builder = Expression.builder();
//            builder.expression();

            List<ScanFilter> filters = attributeFilterMap.entrySet().stream()
                    .filter(entry -> Objects.nonNull(entry.getValue().get()) && !entry.getValue().get().trim().isEmpty())
                    .map(entry -> attributeValueToFilter(entry.getKey(), entry.getValue().get(), tableModel.getTableDef().getAttributeTypesMap().get(entry.getKey()), ScanFilter::new))
                    .toList();

            if (!filters.isEmpty()) {
                scanSpec.filterExpression(builder.build());
            }
            LOG.fine(() -> String.format("Scan %1s = %2s", table.tableName(), logAsJson(scanSpec)));
            return table.scan(scanSpec.limit(PAGE_SIZE).build());
        });
    }

    private QueryEnhancedRequest buildQuerySpec(String hashName, String rangeName, Map<String, SimpleStringProperty> attributeFilterMap) {
        String hashValue = attributeFilterMap.get(hashName).get();
        Key.Builder keyBuilder = Key.builder().partitionValue(hashValue);

        if (rangeName != null && !StringUtils.isNullOrEmpty(attributeFilterMap.get(rangeName).get())) {
            String sortValue = attributeFilterMap.get(rangeName).get();
            keyBuilder.sortValue(sortValue);
        }

        var attributesWithoutKeys = attributeFilterMap.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(hashName) && !entry.getKey().equals(rangeName))
                .filter(entry -> !StringUtils.isNullOrEmpty(entry.getValue().get()))
                .toList();

        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();

        attributesWithoutKeys.forEach(entry -> filterExpressionBuilder.addAttributeValue(entry.getKey(), entry.getValue().getValue()));

        QueryEnhancedRequest.Builder querySpec = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(keyBuilder.build()))
                .filterExpression(filterExpressionBuilder.build());

        return querySpec.limit(PAGE_SIZE).build();
    }

    private CompletableFuture<SdkIterable<Page<EnhancedDocument>>> queryTableItems(QueryEnhancedRequest querySpec) {
        return supplyAsync(() -> {
            LOG.fine(() -> String.format("Query %1s = %2s", table.tableName(), logAsJson(querySpec)));
            return table.query(querySpec);
        });
    }

    private CompletableFuture<SdkIterable<Page<EnhancedDocument>>> queryIndexItems(QueryEnhancedRequest querySpec, DynamoDbIndex<EnhancedDocument> index) {
        return supplyAsync(() -> {
            LOG.fine(() -> String.format("Query %1s = %2s", index.indexName(), logAsJson(querySpec)));
            return index.query(querySpec);
        });
    }


    private CompletableFuture<Void> delete(List<EnhancedDocument> items) {
        return runAsync(() -> Observable.fromIterable(items)
                .buffer(BATCH_SIZE)
                .map(list -> BatchWriteItemEnhancedRequest.builder().writeBatches(
                        list.stream().map(item -> {
                            Key.Builder keyBuilder = Key.builder().partitionValue(item.toMap().get(hash()));
                            if (range() != null) {
                                keyBuilder.sortValue(item.toMap().get(range()));
                            }
                            return WriteBatch.builder(EnhancedDocument.class).addDeleteItem(keyBuilder.build()).build();
                        }).toList()
                ).build())
                .subscribe(documentClient::batchWriteItem)
        );
    }

    /**
     * sort attributes before bindings
     */
    private void bindToModel(software.amazon.awssdk.services.dynamodb.model.TableDescription tableDescription) {
        tableModel.setOriginalTableDescription(tableDescription);

        getHashKey(tableDescription).ifPresent(tableModel.getTableDef()::setHashAttribute);
        getRangeKey(tableDescription).ifPresent(tableModel.getTableDef()::setRangeAttribute);

        Map<String, String> attributes = new TreeMap<>(KEYS_FIRST(hash(), range()));
        attributes.putAll(
                tableDescription.attributeDefinitions().stream()
                        .collect(Collectors.toMap(
                                AttributeDefinition::attributeName,
                                AttributeDefinition::attributeTypeAsString))
        );

        attributes.forEach((name, type) -> tableModel.getTableDef().getAttributeTypesMap().put(name, fromDynamoDBType(type)));

        tableModel.getTableDef().setTotalCount(tableDescription.itemCount());
    }

    private void bindToModel(Pair<List<EnhancedDocument>, Iterator<Page<EnhancedDocument>>> pair) {
        Map<String, Type> attributesTypes = new TreeMap<>(KEYS_FIRST(hash(), range()));
        attributesTypes.putAll(defineAttributesTypes(pair.getKey()));

        tableModel.getTableDef().getAttributeTypesMap().putAll(attributesTypes);
        tableModel.setPageIterator(pair.getValue());
        tableModel.getRows().addAll(pair.getKey());
    }

    private void bindToModel(TableDef tableDef) {
        tableDef.getAttributeTypesMap().keySet().forEach(attr -> tableModel.getAttributeFilterMap().computeIfAbsent(attr, __ -> new SimpleStringProperty()));
    }

    private void applyContext() {
        if (context.propertyName() != null && context.propertyValue() != null) {
            tableModel.getAttributeFilterMap().computeIfAbsent(context.propertyName(), __ -> new SimpleStringProperty()).set(context.propertyValue());
        }
    }

    /**
     * Check that filters map has not null or not empty value for the attribute
     *
     * @param attr attribute for checking
     * @return true if attribute has some value
     */
    private boolean notBlankFilterValue(String attr) {
        SimpleStringProperty property = tableModel.getAttributeFilterMap().get(attr);
        return property != null && !StringUtils.isNullOrEmpty(property.get());
    }

    private String hash() {
        return tableModel.getTableDef().getHashAttribute();
    }

    private String range() {
        return tableModel.getTableDef().getRangeAttribute();
    }

    public void openUrl(String url) {
        hostServices.showDocument(url);
    }
}
