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

package ua.org.java.dynamoit.components.profileviewer;

import ua.org.java.dynamoit.EventBus;
import ua.org.java.dynamoit.components.main.MainModel;
import ua.org.java.dynamoit.components.tablegrid.TableGridContext;
import ua.org.java.dynamoit.db.DynamoDBService;
import ua.org.java.dynamoit.model.TableDef;
import ua.org.java.dynamoit.utils.FXExecutor;

import java.util.stream.Collectors;

public class ProfileController {

    private final DynamoDBService dynamoDBService;
    private final MainModel.ProfileModel model;
    private final EventBus eventBus;

    public ProfileController(MainModel.ProfileModel model, DynamoDBService dynamoDBService, EventBus eventBus) {
        this.dynamoDBService = dynamoDBService;
        this.model = model;
        this.eventBus = eventBus;

        this.getListOfTables();
    }

    public void onSaveFilter() {
        if (model.getFilter() != null && !model.getFilter().isBlank()) {
            this.model.getSavedFilters().add(model.getFilter());
        }
    }

    public void onTablesRefresh() {
        getListOfTables();
    }

    public void onTableSelect(String tableName) {
        eventBus.setSelectedTable(new TableGridContext(model.getProfile(), tableName));
    }

    private void getListOfTables() {
        eventBus.activity(
                this.dynamoDBService.getListOfTables(model.getProfile())
                        .thenApply(tables -> tables.stream().map(TableDef::new).collect(Collectors.toList()))
                        .thenAcceptAsync(tables -> this.model.getAvailableTables().setAll(tables), FXExecutor.getInstance())
        );
    }


}
