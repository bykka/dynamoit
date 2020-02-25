package ua.org.java.dynamoit.table;

import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ua.org.java.dynamoit.utils.DX;

import java.util.List;

public class TableItemsView extends VBox {

    private TableContext context;

    public TableItemsView(TableContext context) {
        this.context = context;

        this.getChildren().addAll(
                List.of(
                        DX.toolBar(toolBar -> List.of(DX.create(Button::new, t -> {
                        }))),
                        DX.create(TableView::new, tableView -> {
                            VBox.setVgrow(tableView, Priority.ALWAYS);
                        })
                )
        );

    }
}
