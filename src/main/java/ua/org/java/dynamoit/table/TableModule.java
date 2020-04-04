package ua.org.java.dynamoit.table;

import dagger.Module;
import dagger.Provides;
import ua.org.java.dynamoit.MainModel;
import ua.org.java.dynamoit.db.DynamoDBService;

@Module
public class TableModule {

    private TableContext context;
    private MainModel mainModel;

    public TableModule(TableContext context, MainModel mainModel) {
        this.context = context;
        this.mainModel = mainModel;
    }

    @Provides
    public TableContext context(){
        return this.context;
    }

    @Provides
    public MainModel mainModel(){
        return this.mainModel;
    }

    @Provides
    public TableItemsView view(TableContext context, TableController controller, MainModel mainModel){
        return new TableItemsView(context, controller, mainModel);
    }

    @Provides
    public TableController controller(TableContext tableContext, DynamoDBService dynamoDBService){
        return new TableController(tableContext, dynamoDBService);
    }

}
