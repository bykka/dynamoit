package ua.org.java.dynamoit.table;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Page;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ua.org.java.dynamoit.utils.DX;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

public class TableItemsView extends VBox {

    private TableController controller;
    private TableView<Map<String, Object>> tableView;
    private ObservableList<Map<String, Object>> rows = FXCollections.observableArrayList();

    public TableItemsView(TableController controller) {
        this.controller = controller;

        this.getChildren().addAll(
                List.of(
                        DX.toolBar(toolBar -> List.of(DX.create(Button::new, t -> {
                        }))),
                        DX.create((Supplier<TableView<Map<String, Object>>>) TableView::new, tableView -> {
                            this.tableView = tableView;
                            VBox.setVgrow(tableView, Priority.ALWAYS);
                            tableView.setItems(rows);
                        })
                )
        );

        controller.describeTable().thenAccept(describeTableResult -> Platform.runLater(() -> {

        }));

        controller.scanItems().thenAccept(items -> {
            Iterator<Page<Item, ScanOutcome>> pageIterator = items.pages().iterator();
            if (pageIterator.hasNext()) {
                Page<Item, ScanOutcome> page = pageIterator.next();
                Platform.runLater(() -> {
                    StreamSupport.stream(page.spliterator(), false)
                            .flatMap(item -> StreamSupport.stream(item.attributes().spliterator(), false).map(Map.Entry::getKey))
                            .sorted()
                            .distinct()
                            .map(attrName -> {
                                TableColumn<Map<String, Object>, String> column = new TableColumn<>(attrName);
                                column.setCellValueFactory(param -> {
                                    Object value = param.getValue().get(attrName);
                                    return new SimpleStringProperty(value != null ? value.toString() : "");
                                });
                                return column;
                            })
                            .forEach(tableColumn -> tableView.getColumns().add(tableColumn));

                    StreamSupport.stream(page.spliterator(), false).map(Item::asMap).forEach(map -> rows.add(map));
                });
            }
        });
    }


}
