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

import dagger.Module;
import dagger.Provides;
import javafx.application.HostServices;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import ua.org.java.dynamoit.EventBus;
import ua.org.java.dynamoit.components.main.MainModel;
import ua.org.java.dynamoit.components.thememanager.ThemeManager;
import ua.org.java.dynamoit.model.profile.ProfileDetails;
import ua.org.java.dynamoit.services.DynamoDbClientRegistry;
import ua.org.java.dynamoit.services.DynamoDbTableService;
import ua.org.java.dynamoit.utils.FXExecutor;

import javax.inject.Singleton;
import java.util.concurrent.Executor;

@Module
public class TableGridModule {

    @Provides
    @Singleton
    public TableGridModel model(MainModel.ProfileModel profileModel) {
        return new TableGridModel(profileModel);
    }

    @Provides
    public TableGridView view(TableGridController controller, TableGridModel tableModel, ThemeManager themeManager) {
        return new TableGridView(controller, tableModel, themeManager);
    }

    @Provides
    public TableGridController controller(
            TableGridContext tableContext,
            TableGridModel tableModel,
            DynamoDbClientRegistry dbClientRegistry,
            EventBus eventBus,
            HostServices hostServices
    ) {
        ProfileDetails profileDetails = tableModel.getProfileModel().getProfileDetails();

        DynamoDbEnhancedClient enhancedClient = dbClientRegistry.getOrCreateDocumentClient(profileDetails);
        TableGridController controller = new TableGridController(
                tableContext,
                tableModel,
                DynamoDbTableService.getOrCreate(profileDetails, tableContext.tableName(), dbClientRegistry),
                eventBus,
                getUIExecutor(),
                hostServices,
                enhancedClient
        );
        controller.init();
        return controller;
    }

    protected Executor getUIExecutor(){
        return FXExecutor.getInstance();
    }

}
