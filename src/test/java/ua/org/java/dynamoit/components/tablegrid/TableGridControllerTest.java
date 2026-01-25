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

import org.junit.jupiter.api.Test;
import ua.org.java.dynamoit.DynamoDBTest;
import ua.org.java.dynamoit.EventBus;
import ua.org.java.dynamoit.components.main.MainModel;
import ua.org.java.dynamoit.model.TableDef;
import ua.org.java.dynamoit.services.DynamoDbService;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TableGridControllerTest extends DynamoDBTest {

    @Test
    public void onRefreshData() {
        MainModel mainModel = new MainModel();
        mainModel.addProfile(localProfileDetails);

        DynamoDbService dynamoDbService = new DynamoDbService(dynamoDbClientRegistry);

        dynamoDbService.getListOfTables(localProfileDetails)
                .thenApply(tables -> tables.stream().map(TableDef::new).collect(Collectors.toList()))
                .thenAcceptAsync(tables -> mainModel.getAvailableProfiles().get("local").getAvailableTables().setAll(tables))
                .join();

        TableGridModel model = new TableGridModel(mainModel.getAvailableProfiles().get("local"));


        TableGridContext context = new TableGridContext(localProfileDetails, "Users");

        EventBus eventBus = new EventBus(ForkJoinPool.commonPool());

        TableGridController controller = new TableGridModule() {
            @Override
            protected Executor getUIExecutor() {
                return ForkJoinPool.commonPool();
            }
        }.controller(context, model, dynamoDbClientRegistry, eventBus, null);

        controller.onRefreshData().join();

        assertEquals(2, model.getRowsSize());
    }
}
