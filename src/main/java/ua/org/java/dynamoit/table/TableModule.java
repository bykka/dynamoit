package ua.org.java.dynamoit.table;

import dagger.Module;
import dagger.Provides;

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
    public TableItemsView view(TableContext tableContext){
        return new TableItemsView(tableContext);
    }

}
