package ua.org.java.dynamoit.table;

import dagger.Component;
import ua.org.java.dynamoit.db.DynamoDBModule;

import javax.inject.Singleton;

@Component(modules = {TableModule.class, DynamoDBModule.class})
@Singleton
public interface TableComponent {

    TableView view();

}
