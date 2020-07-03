package ua.org.java.dynamoit.components.tablegrid;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.internal.PageBasedCollection;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.Pair;
import ua.org.java.dynamoit.EventBus;
import ua.org.java.dynamoit.db.DynamoDBService;
import ua.org.java.dynamoit.model.TableDef;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static ua.org.java.dynamoit.components.tablegrid.Attributes.*;
import static ua.org.java.dynamoit.utils.Utils.*;

public class TableGridController {

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

    public TableGridController(TableGridContext context,
                               TableGridModel tableModel,
                               DynamoDBService dynamoDBService,
                               EventBus eventBus,
                               Executor uiExecutor
    ) {
        this.context = context;
        this.tableModel = tableModel;
        this.eventBus = eventBus;
        this.uiExecutor = uiExecutor;

        tableModel.getMainModel().getAvailableTables().stream()
                .filter(tableDef -> tableDef.getName().equals(context.getTableName()))
                .findFirst()
                .ifPresent(tableModel::setTableDef);

        tableModel.setTableName(context.getTableName());

        dbClient = dynamoDBService.getOrCreateDynamoDBClient(context.getProfileName());
        DynamoDB documentClient = dynamoDBService.getOrCreateDocumentClient(context.getProfileName());
        table = documentClient.getTable(context.getTableName());
    }

    public void init() {
        eventBus.activity(
                supplyAsync(() -> {
                    if (tableModel.getTableDef().getHashAttribute() == null) {
                        return supplyAsync(() -> dbClient.describeTable(context.getTableName()))
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

    public void onCreateItem(String json) {
        eventBus.activity(
                createItem(json).thenRun(this::onRefreshData)
        );
    }

    public void onUpdateItem(String json) {
        eventBus.activity(
                updateItem(json).thenRun(this::onRefreshData)
        );
    }

    public void onDeleteItems(List<Item> items) {
        eventBus.activity(
                delete(items).thenRun(this::onRefreshData)
        );
    }

    public void onClearFilters() {
        tableModel.getAttributeFilterMap().values().forEach(simpleStringProperty -> simpleStringProperty.set(null));
        onRefreshData();
    }

    public void onSaveToFile(File file) {
        eventBus.activity(
                executeQueryOrSearch().thenAccept(items -> {
                    BufferedWriter writer = null;
                    try {
                        writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
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
                        e.printStackTrace();
                    } finally {
                        if (writer != null) {
                            try {
                                writer.flush();
                                writer.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                })
        );
    }

    /**
     * Iterate page to get around PAGE_SIZE number of items if exist
     *
     * @param page current page
     * @return list of items and next page
     */
    private static Pair<List<Item>, Page<Item, ?>> iteratePage(Page<Item, ?> page) {
        List<Item> items = new ArrayList<>(PAGE_SIZE);
        items.addAll(asStream(page).collect(Collectors.toList()));

        while (items.size() < PAGE_SIZE && page.hasNextPage()) {
            page = page.nextPage();
            items.addAll(asStream(page).collect(Collectors.toList()));
        }
        return new Pair<>(items, page);
    }

    // fixme DynamoDB methods
    CompletableFuture<Pair<List<Item>, Page<Item, ?>>> queryPageItems() {
        return executeQueryOrSearch()
                .thenApply(PageBasedCollection::firstPage)
                .thenApply(TableGridController::iteratePage);
    }

    private CompletableFuture<? extends ItemCollection<?>> executeQueryOrSearch() {
        SimpleStringProperty hashValueProperty = tableModel.getAttributeFilterMap().get(hash());
        if (hashValueProperty != null && !StringUtils.isNullOrEmpty(hashValueProperty.get()) && !hashValueProperty.get().contains(ASTERISK)) {
            return queryItems(tableModel.getAttributeFilterMap());
        }
        return scanItems(tableModel.getAttributeFilterMap());
    }

    private CompletableFuture<Void> createItem(String json) {
        return runAsync(() -> table.putItem(Item.fromJSON(json)));
    }

    private CompletableFuture<Void> updateItem(String json) {
        return runAsync(() -> table.putItem(Item.fromJSON(json)));
    }

    private CompletableFuture<ItemCollection<ScanOutcome>> scanItems(Map<String, SimpleStringProperty> attributeFilterMap) {
        return supplyAsync(() -> {
            ScanSpec scanSpec = new ScanSpec();
            List<ScanFilter> filters = attributeFilterMap.entrySet().stream()
                    .filter(entry -> Objects.nonNull(entry.getValue().get()) && entry.getValue().get().trim().length() > 0)
                    .map(entry -> attributeValueToFilter(entry.getKey(), entry.getValue().get(), tableModel.getTableDef().getAttributeTypesMap().get(entry.getKey()), ScanFilter::new))
                    .collect(Collectors.toList());
            if (!filters.isEmpty()) {
                scanSpec.withScanFilters(filters.toArray(new ScanFilter[]{}));
            }
            return table.scan(scanSpec.withMaxPageSize(PAGE_SIZE));
        });
    }

    private CompletableFuture<ItemCollection<QueryOutcome>> queryItems(Map<String, SimpleStringProperty> attributeFilterMap) {
        return supplyAsync(() -> {
            QuerySpec querySpec = new QuerySpec();
            querySpec.withHashKey(hash(), attributeFilterMap.get(hash()).get());
            if (range() != null && !StringUtils.isNullOrEmpty(attributeFilterMap.get(range()).get())) {
                querySpec.withRangeKeyCondition(new RangeKeyCondition(range()).eq(attributeFilterMap.get(range()).get()));
            }
            List<QueryFilter> filters = attributeFilterMap.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(hash()) && !entry.getKey().equals(range()))
                    .filter(entry -> !StringUtils.isNullOrEmpty(entry.getValue().get()))
                    .map(entry -> attributeValueToFilter(entry.getKey(), entry.getValue().get(), tableModel.getTableDef().getAttributeTypesMap().get(entry.getKey()), QueryFilter::new))
                    .collect(Collectors.toList());
            if (!filters.isEmpty()) {
                querySpec.withQueryFilters(filters.toArray(new QueryFilter[]{}));
            }
            return table.query(querySpec.withMaxPageSize(PAGE_SIZE));
        });
    }

    private CompletableFuture<Void> delete(List<Item> items) {
        return runAsync(() -> items.forEach(item -> {
            if (range() == null) {
                table.deleteItem(hash(), item.get(hash()));
            } else {
                table.deleteItem(hash(), item.get(hash()), range(), item.get(range()));
            }
        }));
    }

    /**
     * sort attributes before bindings
     */
    private void bindToModel(DescribeTableResult describeTable) {
        getHashKey(describeTable).ifPresent(tableModel.getTableDef()::setHashAttribute);
        getRangeKey(describeTable).ifPresent(tableModel.getTableDef()::setRangeAttribute);

        Map<String, String> attributes = new TreeMap<>(KEYS_FIRST(hash(), range()));
        attributes.putAll(
                describeTable.getTable().getAttributeDefinitions().stream()
                        .collect(Collectors.toMap(
                                AttributeDefinition::getAttributeName,
                                AttributeDefinition::getAttributeType))
        );

        attributes.forEach((name, type) -> tableModel.getTableDef().getAttributeTypesMap().put(name, fromDynamoDBType(type)));

        tableModel.getTableDef().setTotalCount(describeTable.getTable().getItemCount());
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
        if (context.getPropertyName() != null && context.getPropertyValue() != null) {
            tableModel.getAttributeFilterMap().computeIfAbsent(context.getPropertyName(), __ -> new SimpleStringProperty()).set(context.getPropertyValue());
        }
    }

    private String hash() {
        return tableModel.getTableDef().getHashAttribute();
    }

    private String range() {
        return tableModel.getTableDef().getRangeAttribute();
    }

}
