package ua.org.java.dynamoit;

import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class TableItemsView extends VBox {

    public TableItemsView() {
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
