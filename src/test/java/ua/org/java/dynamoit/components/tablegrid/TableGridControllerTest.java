package ua.org.java.dynamoit.components.tablegrid;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Page;
import com.amazonaws.services.dynamodbv2.document.Table;
import javafx.util.Pair;
import org.junit.Test;
import ua.org.java.dynamoit.EventBus;
import ua.org.java.dynamoit.MainModel;
import ua.org.java.dynamoit.db.DynamoDBService;
import ua.org.java.dynamoit.model.TableDef;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class TableGridControllerTest {

    @Test
    public void onRefreshData() {
        TableGridContext context = new TableGridContext("profile1", "table1");
        EventBus eventBus = new EventBus(ForkJoinPool.commonPool());
        MainModel mainModel = new MainModel();
        TableDef tableDef = new TableDef("Table1");
        tableDef.setHashAttribute("hash_attr");
        TableGridModel model = new TableGridModel(mainModel);
        model.setTableDef(tableDef);
        model.getRows().add(new Item());

        Table table = mock(Table.class);
        AmazonDynamoDB amazonDynamoDB = mock(AmazonDynamoDB.class);
        DynamoDB dynamoDB = mock(DynamoDB.class);
        expect(dynamoDB.getTable(context.getTableName())).andReturn(table);
        DynamoDBService dynamoDBService = mock(DynamoDBService.class);
        expect(dynamoDBService.getOrCreateDynamoDBClient(context.getProfileName())).andReturn(amazonDynamoDB);
        expect(dynamoDBService.getOrCreateDocumentClient(context.getProfileName())).andReturn(dynamoDB);

        Page<Item, Object> page = mock(Page.class);

        replay(table, amazonDynamoDB, dynamoDB, dynamoDBService, page);

        TableGridController controller = partialMockBuilder(TableGridController.class)
                .withConstructor(context, model, dynamoDBService, eventBus, ForkJoinPool.commonPool())
                .addMockedMethod("queryPageItems")
                .createMock();

        expect(controller.queryPageItems()).andReturn(CompletableFuture.completedFuture(
                new Pair<>(List.of(new Item(), new Item()), page)
        ));

        replay(controller);

        controller.onRefreshData().join();

        verify(table, amazonDynamoDB, dynamoDB, dynamoDBService, page, controller);

        assertEquals(model.getRowsSize(), 2);
        assertEquals(model.getCurrentPage(), page);
    }
}
