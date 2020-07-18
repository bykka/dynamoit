/*
 * This file is part of DynamoIt.
 *
 *     DynamoIt is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Foobar is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with DynamoIt.  If not, see <https://www.gnu.org/licenses/>.
 */

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
