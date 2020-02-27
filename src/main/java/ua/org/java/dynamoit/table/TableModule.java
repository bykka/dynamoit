package ua.org.java.dynamoit.table;

import dagger.Module;
import dagger.Provides;
import ua.org.java.dynamoit.db.DynamoDBService;

@Module
public class TableModule {

    private TableContext context;

    public TableModule(TableContext context) {
        this.context = context;
    }

    @Provides
    public TableContext context(){
        return this.context;
    }

    @Provides
    public TableItemsView view(TableController controller){
        return new TableItemsView(controller);
    }

    @Provides
    public TableController controller(TableContext tableContext, DynamoDBService dynamoDBService){
        return new TableController(tableContext, dynamoDBService);
    }

}
