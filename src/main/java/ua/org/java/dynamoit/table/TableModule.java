package ua.org.java.dynamoit.table;

import dagger.Module;
import dagger.Provides;
import ua.org.java.dynamoit.MainModel;
import ua.org.java.dynamoit.db.DynamoDBService;

import javax.inject.Singleton;

@Module
public class TableModule {

    @Provides
    @Singleton
    public TableModel model(MainModel mainModel) {
        return new TableModel(mainModel);
    }

    @Provides
    public TableView view(TableController controller, TableModel tableModel) {
        return new TableView(controller, tableModel);
    }

    @Provides
    public TableController controller(TableContext tableContext, TableModel tableModel, DynamoDBService dynamoDBService) {
        return new TableController(tableContext, tableModel, dynamoDBService);
    }

}
