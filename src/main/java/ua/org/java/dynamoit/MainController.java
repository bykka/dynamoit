package ua.org.java.dynamoit;

import com.amazonaws.util.StringUtils;
import ua.org.java.dynamoit.db.DynamoDBService;
import ua.org.java.dynamoit.utils.FXExecutor;

import java.util.concurrent.CompletableFuture;

public class MainController {

    private final DynamoDBService dynamoDBService;
    private final MainModel model;

    public MainController(DynamoDBService dynamoDBService, MainModel model) {
        this.dynamoDBService = dynamoDBService;
        this.model = model;

        CompletableFuture
                .supplyAsync(this.dynamoDBService::getAvailableProfiles)
                .thenAcceptAsync(profiles -> model.getAvailableProfiles().addAll(profiles), FXExecutor.getInstance());

        this.model.selectedProfileProperty().addListener((observable, oldValue, newValue) -> {
            this.model.getAvailableTables().clear();
            if (!StringUtils.isNullOrEmpty(newValue)) {
                this.dynamoDBService.getListOfTables(newValue)
                        .thenAcceptAsync(tables -> this.model.getAvailableTables().addAll(tables), FXExecutor.getInstance());
            }
        });
    }

    public void onSaveFilter() {
        this.model.getSavedFilters().add(model.getFilter());
    }

}
