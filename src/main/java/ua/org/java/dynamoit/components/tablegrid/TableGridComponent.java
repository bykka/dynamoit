package ua.org.java.dynamoit.components.tablegrid;

import dagger.BindsInstance;
import dagger.Component;
import ua.org.java.dynamoit.MainModel;
import ua.org.java.dynamoit.db.DynamoDBModule;

import javax.inject.Singleton;

@Component(modules = {TableGridModule.class, DynamoDBModule.class})
@Singleton
public interface TableGridComponent {

    TableGridView view();

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder mainModel(MainModel mainModel);
        @BindsInstance
        Builder tableContext(TableGridContext context);
        TableGridComponent build();
    }

}
