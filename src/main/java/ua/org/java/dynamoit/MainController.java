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

package ua.org.java.dynamoit;

import ua.org.java.dynamoit.components.profileviewer.DaggerProfileComponent;
import ua.org.java.dynamoit.components.profileviewer.ProfileComponent;
import ua.org.java.dynamoit.components.tablegrid.DaggerTableGridComponent;
import ua.org.java.dynamoit.components.tablegrid.TableGridComponent;
import ua.org.java.dynamoit.components.tablegrid.TableGridContext;
import ua.org.java.dynamoit.db.DynamoDBService;
import ua.org.java.dynamoit.utils.FXExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class MainController {

    private final DynamoDBService dynamoDBService;
    private final MainModel model;
    private final EventBus eventBus;
    private Consumer<TableGridContext> selectedTableConsumer;

    public MainController(DynamoDBService dynamoDBService, MainModel model, EventBus eventBus) {
        this.dynamoDBService = dynamoDBService;
        this.model = model;
        this.eventBus = eventBus;

        eventBus.activity(
                CompletableFuture
                        .supplyAsync(this.dynamoDBService::getAvailableProfiles)
                        .thenAcceptAsync(profiles -> profiles.forEach(profile -> model.addProfile(profile.getName(), profile.getRegion())), FXExecutor.getInstance())
        );

        eventBus.selectedTableProperty().addListener((observable, oldValue, newValue) -> {
            if (selectedTableConsumer != null) {
                selectedTableConsumer.accept(newValue);
            }
        });
    }

    public TableGridComponent buildTableGridComponent(TableGridContext tableContext) {
        return DaggerTableGridComponent.builder()
                .mainModel(model)
                .eventBus(eventBus)
                .tableContext(tableContext)
                .build();
    }

    public ProfileComponent buildProfileComponent(String profile) {
        return DaggerProfileComponent.builder()
                .mainModel(model)
                .eventBus(eventBus)
                .profile(profile)
                .build();
    }

    public void setSelectedTableConsumer(Consumer<TableGridContext> selectedTableConsumer) {
        this.selectedTableConsumer = selectedTableConsumer;
    }
}
