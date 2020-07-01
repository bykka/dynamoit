package ua.org.java.dynamoit;

import com.amazonaws.util.StringUtils;
import ua.org.java.dynamoit.components.tablegrid.DaggerTableGridComponent;
import ua.org.java.dynamoit.components.tablegrid.TableGridComponent;
import ua.org.java.dynamoit.components.tablegrid.TableGridContext;
import ua.org.java.dynamoit.db.DynamoDBService;
import ua.org.java.dynamoit.model.TableDef;
import ua.org.java.dynamoit.utils.FXExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MainController {

    private final DynamoDBService dynamoDBService;
    private final MainModel model;
    private final EventBus eventBus;

    public MainController(DynamoDBService dynamoDBService, MainModel model, EventBus eventBus) {
        this.dynamoDBService = dynamoDBService;
        this.model = model;
        this.eventBus = eventBus;

        eventBus.activity(
                CompletableFuture
                        .supplyAsync(this.dynamoDBService::getAvailableProfiles)
                        .thenAcceptAsync(profiles -> model.getAvailableProfiles().addAll(profiles), FXExecutor.getInstance())
        );

        this.model.selectedProfileProperty().addListener((observable, oldValue, newValue) -> {
            this.model.getAvailableTables().clear();
            if (!StringUtils.isNullOrEmpty(newValue)) {
                getListOfTables(newValue);
            }
        });
    }

    public void onSaveFilter() {
        this.model.getSavedFilters().add(model.getFilter());
    }

    public void onTablesRefresh() {
        getListOfTables(model.getSelectedProfile());
    }

    public TableGridComponent buildTableGridComponent(TableGridContext tableContext){
        return DaggerTableGridComponent.builder()
                .mainModel(model)
                .eventBus(eventBus)
                .tableContext(tableContext)
                .build();
    }

    private void getListOfTables(String profile) {
        eventBus.activity(
                this.dynamoDBService.getListOfTables(profile)
                        .thenApply(tables -> tables.stream().map(TableDef::new).collect(Collectors.toList()))
                        .thenAcceptAsync(tables -> this.model.getAvailableTables().setAll(tables), FXExecutor.getInstance())
        );
    }

}
