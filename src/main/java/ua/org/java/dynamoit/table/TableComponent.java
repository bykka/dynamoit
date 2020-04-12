package ua.org.java.dynamoit.table;

import dagger.BindsInstance;
import dagger.Component;
import ua.org.java.dynamoit.MainModel;
import ua.org.java.dynamoit.db.DynamoDBModule;

import javax.inject.Singleton;

@Component(modules = {TableModule.class, DynamoDBModule.class})
@Singleton
public interface TableComponent {

    TableView view();

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder mainModel(MainModel mainModel);
        @BindsInstance
        Builder tableContext(TableContext context);
        TableComponent build();
    }

}
