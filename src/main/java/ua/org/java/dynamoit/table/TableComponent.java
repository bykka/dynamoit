package ua.org.java.dynamoit.table;

import dagger.Component;

@Component(modules = {TableModule.class})
public interface TableComponent {

    TableItemsView view();

}
