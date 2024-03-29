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

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.internal.PageBasedCollection;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.model.*;
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
import ua.org.java.dynamoit.EventBus;
import ua.org.java.dynamoit.db.DynamoDBService;
import ua.org.java.dynamoit.db.KeySchemaType;
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

    private final AmazonDynamoDB dbClient;
    private final Table table;
    private final TableGridContext context;
    private final TableGridModel tableModel;
    private final EventBus eventBus;
    private final Executor uiExecutor;
    private final HostServices hostServices;
    private final DynamoDB documentClient;

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
        table = documentClient.getTable(context.tableName());
    }

    public void init() {
        eventBus.activity(
                supplyAsync(() -> {
                    if (tableModel.getOriginalTableDescription() == null) {
                        return supplyAsync(() -> dbClient.describeTable(context.tableName()))
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
        if (tableModel.getCurrentPage().hasNextPage()) {
            eventBus.activity(
                    supplyAsync(() -> tableModel.getCurrentPage().nextPage())
                            .thenApply(TableGridController::iteratePage)
                            .thenAcceptAsync(pair -> {
                                tableModel.getTableDef().getAttributeTypesMap().putAll(defineAttributesTypes(pair.getKey()));
                                tableModel.setCurrentPage(pair.getValue());
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
                        Item item = Item.fromJSON(text);

                        if (jsonOnly) {
                            return true;
                        }
                        return item.get(hash()) != null && (range() == null || item.get(range()) != null);
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

    public void onDeleteItems(List<Item> items) {
        eventBus.activity(
                delete(items).thenRun(this::onRefreshData)
        );
    }

    public void onPatchItems(List<Item> items, String jsonPatch, boolean isRaw) {
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
                        asStream(items).forEach(o -> {
                            try {
                                generator.writeRawValue(o.toJSON());
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
                                .map(jsonNode -> Item.fromJSON(jsonNode.toString()))
                                .buffer(25)
                                .map(list -> {
                                    TableWriteItems addItems = new TableWriteItems(table.getTableName());
                                    list.forEach(addItems::addItemToPut);
                                    return addItems;
                                })
                                .subscribe(documentClient::batchWriteItem);
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
     * Iterate page to get around PAGE_SIZE number of items if exist
     *
     * @param page current page
     * @return list of items and next page
     */
    private static Pair<List<Item>, Page<Item, ?>> iteratePage(Page<Item, ?> page) {
        List<Item> items = new ArrayList<>(PAGE_SIZE);
        items.addAll(asStream(page).toList());

        while (items.size() < PAGE_SIZE && page.hasNextPage()) {
            page = page.nextPage();
            items.addAll(asStream(page).toList());
        }
        return new Pair<>(items, page);
    }

    CompletableFuture<Pair<List<Item>, Page<Item, ?>>> queryPageItems() {
        return executeQueryOrSearch()
                .thenApply(PageBasedCollection::firstPage)
                .thenApply(TableGridController::iteratePage);
    }

    private CompletableFuture<? extends ItemCollection<?>> executeQueryOrSearch() {
        // query table if hash attribute has filter
        if (notBlankFilterValue(hash())) {
            SimpleStringProperty hashValueProperty = tableModel.getAttributeFilterMap().get(hash());
            QueryFilter filter = attributeValueToFilter(hash(), hashValueProperty.get(), Type.STRING, QueryFilter::new);
            // query if hash has eq operation only
            if (filter.getComparisonOperator() == ComparisonOperator.EQ) {
                QuerySpec querySpec = buildQuerySpec(hash(), range(), tableModel.getAttributeFilterMap());
                return queryTableItems(querySpec);
            }
        } else if (tableModel.getOriginalTableDescription().getGlobalSecondaryIndexes() != null) {
            // find global indexes with ALL properties projection
            List<GlobalSecondaryIndexDescription> fullProjectionIndexes = tableModel.getOriginalTableDescription().getGlobalSecondaryIndexes().stream()
                    .filter(__ -> __.getProjection().getProjectionType().equals("ALL")).toList();

            // find the first index that has hash and range attributes in the filters map
            Optional<GlobalSecondaryIndexDescription> globalIndexOptional = fullProjectionIndexes.stream()
                    .filter(__ -> __.getKeySchema().stream()
                            .allMatch(key -> notBlankFilterValue(key.getAttributeName()))
                    ).findFirst();

            // or try to find at least global index with hash in the filters map
            globalIndexOptional = globalIndexOptional.or(() -> fullProjectionIndexes.stream()
                    .filter(__ -> lookUpKeyName(__.getKeySchema(), KeySchemaType.HASH).map(this::notBlankFilterValue).orElse(false)).findFirst()
            );

            if (globalIndexOptional.isPresent()) {
                GlobalSecondaryIndexDescription indexDescription = globalIndexOptional.get();

                Index index = table.getIndex(indexDescription.getIndexName());
                Optional<String> indexHash = lookUpKeyName(indexDescription.getKeySchema(), KeySchemaType.HASH);
                Optional<String> indexRange = lookUpKeyName(indexDescription.getKeySchema(), KeySchemaType.RANGE);

                if (indexHash.isPresent()) {
                    QuerySpec querySpec = buildQuerySpec(indexHash.get(), indexRange.orElse(null), tableModel.getAttributeFilterMap());
                    return queryIndexItems(querySpec, index);
                }
            }
        }
        return scanItems(tableModel.getAttributeFilterMap());
    }

    private CompletableFuture<Void> processItemAsync(String json, boolean isRaw, Consumer<Item> command) {
        try {
            Item item = isRaw ? rawJsonToItem(json) : Item.fromJSON(json);
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

    private CompletableFuture<Void> patchItems(List<Item> items, String jsonPatch, boolean isRaw) {
        if (items.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return processItemAsync(jsonPatch, isRaw, patch -> items.forEach(item -> {
            UpdateItemSpec updateItemSpec = new UpdateItemSpec();
            if (range() == null) {
                updateItemSpec.withPrimaryKey(hash(), item.get(hash()));
            } else {
                updateItemSpec.withPrimaryKey(hash(), item.get(hash()), range(), item.get(range()));
            }

            updateItemSpec.withAttributeUpdate(
                    asStream(patch.attributes())
                            .map(entry -> new AttributeUpdate(entry.getKey()).put(entry.getValue()))
                            .collect(Collectors.toList())
            );

            table.updateItem(updateItemSpec);
        }));
    }

    private CompletableFuture<ItemCollection<ScanOutcome>> scanItems(Map<String, SimpleStringProperty> attributeFilterMap) {
        return supplyAsync(() -> {
            ScanSpec scanSpec = new ScanSpec();
            List<ScanFilter> filters = attributeFilterMap.entrySet().stream()
                    .filter(entry -> Objects.nonNull(entry.getValue().get()) && entry.getValue().get().trim().length() > 0)
                    .map(entry -> attributeValueToFilter(entry.getKey(), entry.getValue().get(), tableModel.getTableDef().getAttributeTypesMap().get(entry.getKey()), ScanFilter::new))
                    .toList();
            if (!filters.isEmpty()) {
                scanSpec.withScanFilters(filters.toArray(new ScanFilter[]{}));
            }
            LOG.fine(() -> String.format("Scan %1s = %2s", table.getTableName(), logAsJson(scanSpec)));
            return table.scan(scanSpec.withMaxPageSize(PAGE_SIZE));
        });
    }

    private QuerySpec buildQuerySpec(String hashName, String rangeName, Map<String, SimpleStringProperty> attributeFilterMap) {
        QuerySpec querySpec = new QuerySpec();
        querySpec.withHashKey(hashName, attributeFilterMap.get(hashName).get());
        if (rangeName != null && !StringUtils.isNullOrEmpty(attributeFilterMap.get(rangeName).get())) {
            querySpec.withRangeKeyCondition(new RangeKeyCondition(rangeName).eq(attributeFilterMap.get(rangeName).get()));
        }
        List<QueryFilter> filters = attributeFilterMap.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(hashName) && !entry.getKey().equals(rangeName))
                .filter(entry -> !StringUtils.isNullOrEmpty(entry.getValue().get()))
                .map(entry -> attributeValueToFilter(entry.getKey(), entry.getValue().get(), tableModel.getTableDef().getAttributeTypesMap().get(entry.getKey()), QueryFilter::new))
                .toList();
        if (!filters.isEmpty()) {
            querySpec.withQueryFilters(filters.toArray(new QueryFilter[]{}));
        }

        return querySpec.withMaxPageSize(PAGE_SIZE);
    }

    private CompletableFuture<ItemCollection<QueryOutcome>> queryTableItems(QuerySpec querySpec) {
        return supplyAsync(() -> {
            LOG.fine(() -> String.format("Query %1s = %2s", table.getTableName(), logAsJson(querySpec)));
            return table.query(querySpec);
        });
    }

    private CompletableFuture<ItemCollection<QueryOutcome>> queryIndexItems(QuerySpec querySpec, Index index) {
        return supplyAsync(() -> {
            LOG.fine(() -> String.format("Query %1s = %2s", index.getIndexName(), logAsJson(querySpec)));
            return index.query(querySpec);
        });
    }


    private CompletableFuture<Void> delete(List<Item> items) {
        return runAsync(() -> Observable.fromIterable(items)
                .buffer(25)
                .map(list -> {
                    TableWriteItems deleteItems = new TableWriteItems(table.getTableName());
                    list.forEach(item -> {
                        if (range() == null) {
                            deleteItems.addHashOnlyPrimaryKeyToDelete(hash(), item.get(hash()));
                        } else {
                            deleteItems.addHashAndRangePrimaryKeyToDelete(hash(), item.get(hash()), range(), item.get(range()));
                        }
                    });
                    return deleteItems;
                })
                .subscribe(documentClient::batchWriteItem)
        );
    }

    /**
     * sort attributes before bindings
     */
    private void bindToModel(DescribeTableResult describeTable) {
        TableDescription originalTableDescription = describeTable.getTable();
        tableModel.setOriginalTableDescription(originalTableDescription);

        getHashKey(describeTable).ifPresent(tableModel.getTableDef()::setHashAttribute);
        getRangeKey(describeTable).ifPresent(tableModel.getTableDef()::setRangeAttribute);

        Map<String, String> attributes = new TreeMap<>(KEYS_FIRST(hash(), range()));
        attributes.putAll(
                originalTableDescription.getAttributeDefinitions().stream()
                        .collect(Collectors.toMap(
                                AttributeDefinition::getAttributeName,
                                AttributeDefinition::getAttributeType))
        );

        attributes.forEach((name, type) -> tableModel.getTableDef().getAttributeTypesMap().put(name, fromDynamoDBType(type)));

        tableModel.getTableDef().setTotalCount(originalTableDescription.getItemCount());
    }

    private void bindToModel(Pair<List<Item>, Page<Item, ?>> pair) {
        Map<String, Type> attributesTypes = new TreeMap<>(KEYS_FIRST(hash(), range()));
        attributesTypes.putAll(defineAttributesTypes(pair.getKey()));

        tableModel.getTableDef().getAttributeTypesMap().putAll(attributesTypes);
        tableModel.setCurrentPage(pair.getValue());
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
