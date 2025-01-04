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

import javafx.application.HostServices;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import ua.org.java.dynamoit.EventBus;
import ua.org.java.dynamoit.components.main.MainModel;
import ua.org.java.dynamoit.model.profile.LocalProfileDetails;
import ua.org.java.dynamoit.services.DynamoDbTableService;

import java.util.concurrent.ForkJoinPool;

import static org.mockito.Mockito.mock;


//@PrepareForTest(HostServices.class)
//@RunWith(PowerMockRunner.class)
public class TableGridControllerTest {

    @Test
    public void onRefreshData() {
        LocalProfileDetails profileDetails = new LocalProfileDetails("profile1", "region1");
        TableGridContext context = new TableGridContext(profileDetails, "table1");
        MainModel mainModel = new MainModel();
        mainModel.addProfile(profileDetails);

//        TableDef tableDef = new TableDef("Table1");
//        tableDef.setHashAttribute("hash_attr");
        TableGridModel model = new TableGridModel(mainModel.getAvailableProfiles().get("profile1"));
//        model.setTableDef(tableDef);
//        model.getRows().add(new Item());
//
//        Table table = mock(Table.class);
        DynamoDbClient amazonDynamoDB = mock(DynamoDbClient.class);
        DynamoDbEnhancedClient dynamoDB = mock(DynamoDbEnhancedClient.class);

//        expect(dynamoDB.getTable(context.tableName())).andReturn(table);
        DynamoDbTableService dynamoDbTableService = mock(DynamoDbTableService.class);
//        when(dynamoDBService.getOrCreateDynamoDBClient(context.profileDetails())).thenReturn(amazonDynamoDB);
//        when(dynamoDBService.getOrCreateDocumentClient(context.profileDetails())).thenReturn(dynamoDB);
//
//        Page<Item, Object> page = mock(Page.class);
        HostServices hostServices = mock(HostServices.class);
//
        EventBus eventBus = new EventBus(ForkJoinPool.commonPool());
//
//        replay(table, amazonDynamoDB, dynamoDB, dynamoDBService, page);
//cc
        TableGridController controller = new TableGridController(context, model, dynamoDbTableService, eventBus, ForkJoinPool.commonPool(), hostServices, dynamoDB);

//        expect(controller.queryPageItems()).andReturn(CompletableFuture.completedFuture(
//                new Pair<>(List.of(new Item(), new Item()), page)
//        ));
//
//        replay(controller);
//
//        controller.onRefreshData().join();
//
//        verify(table, amazonDynamoDB, dynamoDB, dynamoDBService, page, controller);
//
//        assertEquals(model.getRowsSize(), 2);
//        assertEquals(model.getPageIterator(), page);
    }
}
