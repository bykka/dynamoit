package ua.org.java.dynamoit.components.tablegrid;

import dagger.Module;
import dagger.Provides;
import ua.org.java.dynamoit.EventBus;
import ua.org.java.dynamoit.MainModel;
import ua.org.java.dynamoit.db.DynamoDBService;

import javax.inject.Singleton;

@Module
public class TableGridModule {

    @Provides
    @Singleton
    public TableGridModel model(MainModel mainModel) {
        return new TableGridModel(mainModel);
    }

    @Provides
    public TableGridView view(TableGridController controller, TableGridModel tableModel) {
        return new TableGridView(controller, tableModel);
    }

    @Provides
    public TableGridController controller(TableGridContext tableContext, TableGridModel tableModel, DynamoDBService dynamoDBService, EventBus eventBus) {
        return new TableGridController(tableContext, tableModel, dynamoDBService, eventBus);
    }

}
