package ua.org.java.dynamoit.components.tablegrid;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.internal.PageBasedCollection;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.Pair;
import ua.org.java.dynamoit.EventBus;
import ua.org.java.dynamoit.db.DynamoDBService;
import ua.org.java.dynamoit.utils.FXExecutor;
import ua.org.java.dynamoit.utils.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static ua.org.java.dynamoit.utils.Utils.asStream;

public class TableGridController {

    /**
     * Maximum number of items in one page
     */
    private static final int PAGE_SIZE = 100;

    private final AmazonDynamoDB dbClient;
    private final Table table;
    private final TableGridModel tableModel;
    private final EventBus eventBus;

    public TableGridController(TableGridContext context, TableGridModel tableModel, DynamoDBService dynamoDBService, EventBus eventBus) {
        this.tableModel = tableModel;
        this.eventBus = eventBus;

        tableModel.setTableName(context.getTableName());

        dbClient = dynamoDBService.getOrCreateDynamoDBClient(context.getProfileName());
        DynamoDB documentClient = dynamoDBService.getOrCreateDocumentClient(context.getProfileName());
        table = documentClient.getTable(context.getTableName());

        eventBus.activity(
                CompletableFuture
                        .supplyAsync(() -> dbClient.describeTable(context.getTableName()))
                        .thenAcceptAsync(describeTable -> {
                            Utils.getHashKey(describeTable).ifPresent(tableModel::setHashAttribute);
                            Utils.getRangeKey(describeTable).ifPresent(tableModel::setRangeAttribute);
                            tableModel.setDescribeTableResult(describeTable);

                            if (context.getPropertyName() != null && context.getPropertyValue() != null) {
                                tableModel.getAttributeFilterMap().put(tableModel.getHashAttribute(), new SimpleStringProperty(context.getPropertyValue()));
                            }
                        }, FXExecutor.getInstance())
                        .thenCompose(__ -> queryPageItems())
                        .thenAcceptAsync(pair -> {
                            tableModel.getRows().addAll(pair.getKey());
                            tableModel.setCurrentPage(pair.getValue());
                        }, FXExecutor.getInstance())
        );
    }


    public void onReachScrollEnd() {
        if (tableModel.getCurrentPage().hasNextPage()) {
            eventBus.activity(
                    CompletableFuture
                            .supplyAsync(() -> tableModel.getCurrentPage().nextPage())
                            .thenAcceptAsync(page -> {
                                Pair<List<Item>, Page<Item, ?>> pair = iteratePage(page);
                                tableModel.getRows().addAll(pair.getKey());
                                tableModel.setCurrentPage(pair.getValue());
                            }, FXExecutor.getInstance())
            );
        }
    }

    public void onRefreshData() {
        eventBus.activity(
                queryPageItems().thenAcceptAsync(pair -> {
                    tableModel.getRows().addAll(pair.getKey());
                    tableModel.setCurrentPage(pair.getValue());
                }, FXExecutor.getInstance())
        );
    }

    public void onCreateItem(String json) {
        createItem(json).thenRun(this::onRefreshData);
    }

    public void onUpdateItem(String json) {
        updateItem(json).thenRun(this::onRefreshData);
    }

    public void onDeleteItem(Item item) {
        delete(item).thenRun(this::onRefreshData);
    }

    public void onClearFilters() {
        tableModel.getAttributeFilterMap().values().forEach(simpleStringProperty -> simpleStringProperty.set(null));
        onRefreshData();
    }

    public void onTableSave(File file) {
        eventBus.activity(
                executeQueryOrSearch().thenAccept(items -> {
                    BufferedWriter writer = null;
                    try {
                        writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
                        JsonGenerator generator = new JsonFactory(new ObjectMapper()).createGenerator(writer);
                        generator.writeStartArray();
                        Utils.asStream(items).forEach(o -> {
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
    private CompletableFuture<Pair<List<Item>, Page<Item, ?>>> queryPageItems() {
        return executeQueryOrSearch()
                .thenApply(PageBasedCollection::firstPage)
                .thenApply(TableGridController::iteratePage);
    }

    private CompletableFuture<? extends ItemCollection<?>> executeQueryOrSearch() {
        SimpleStringProperty hashValueProperty = tableModel.getAttributeFilterMap().get(tableModel.getHashAttribute());
        if (hashValueProperty != null && !StringUtils.isNullOrEmpty(hashValueProperty.get())) {
            return queryItems(tableModel.getAttributeFilterMap());
        }
        return scanItems(tableModel.getAttributeFilterMap());
    }

    private CompletableFuture<Void> createItem(String json) {
        return CompletableFuture.runAsync(() -> table.putItem(Item.fromJSON(json)));
    }

    private CompletableFuture<Void> updateItem(String json) {
        return CompletableFuture.runAsync(() -> table.putItem(Item.fromJSON(json)));
    }

    private CompletableFuture<ItemCollection<ScanOutcome>> scanItems(Map<String, SimpleStringProperty> attributeFilterMap) {
        return CompletableFuture.supplyAsync(() -> {
            ScanSpec scanSpec = new ScanSpec();
            List<ScanFilter> filters = attributeFilterMap.entrySet().stream()
                    .filter(entry -> Objects.nonNull(entry.getValue().get()) && entry.getValue().get().trim().length() > 0)
                    .map(entry -> new ScanFilter(entry.getKey()).eq(entry.getValue().get()))
                    .collect(Collectors.toList());
            if (!filters.isEmpty()) {
                scanSpec.withScanFilters(filters.toArray(new ScanFilter[]{}));
            }
            return table.scan(scanSpec.withMaxPageSize(PAGE_SIZE));
        });
    }

    private CompletableFuture<ItemCollection<QueryOutcome>> queryItems(Map<String, SimpleStringProperty> attributeFilterMap) {
        return CompletableFuture.supplyAsync(() -> {
            QuerySpec querySpec = new QuerySpec();
            querySpec.withHashKey(tableModel.getHashAttribute(), attributeFilterMap.get(tableModel.getHashAttribute()).get());
            if (tableModel.getRangeAttribute() != null && !StringUtils.isNullOrEmpty(attributeFilterMap.get(tableModel.getRangeAttribute()).get())) {
                querySpec.withRangeKeyCondition(new RangeKeyCondition(tableModel.getRangeAttribute()).eq(attributeFilterMap.get(tableModel.getRangeAttribute()).get()));
            }
            List<QueryFilter> filters = attributeFilterMap.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(tableModel.getHashAttribute()) && !entry.getKey().equals(tableModel.getRangeAttribute()))
                    .filter(entry -> !StringUtils.isNullOrEmpty(entry.getValue().get()))
                    .map(entry -> new QueryFilter(entry.getKey()).eq(entry.getValue().get()))
                    .collect(Collectors.toList());
            if (!filters.isEmpty()) {
                querySpec.withQueryFilters(filters.toArray(new QueryFilter[]{}));
            }
            return table.query(querySpec.withMaxPageSize(PAGE_SIZE));
        });
    }

    private CompletableFuture<Void> delete(Item item) {
        return CompletableFuture.runAsync(() -> {
            if (tableModel.getRangeAttribute() == null) {
                table.deleteItem(tableModel.getHashAttribute(), item.get(tableModel.getHashAttribute()));
            } else {
                table.deleteItem(tableModel.getHashAttribute(), item.get(tableModel.getHashAttribute()), tableModel.getRangeAttribute(), item.get(tableModel.getRangeAttribute()));
            }
        });
    }

}
