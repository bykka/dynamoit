package ua.org.java.dynamoit.table;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Page;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
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
import java.util.stream.Collectors;

import static ua.org.java.dynamoit.utils.Utils.asStream;

public class TableItemsView extends VBox {

    private static final String HASH = "HASH";
    private static final String RANGE = "RANGE";

    private TableController controller;
    private TableView<Map<String, Object>> tableView;
    private ObservableList<Map<String, Object>> rows = FXCollections.observableArrayList();
    private TableDescription tableDescription;
    private Map<String, String> keyTypeMap;

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
            tableDescription = describeTableResult.getTable();
            keyTypeMap = tableDescription.getKeySchema().stream().collect(Collectors.toMap(KeySchemaElement::getAttributeName, KeySchemaElement::getKeyType));
        })).thenRunAsync(() -> {
            controller.scanItems().thenAccept(items -> {
                Iterator<Page<Item, ScanOutcome>> pageIterator = items.pages().iterator();
                if (pageIterator.hasNext()) {
                    Page<Item, ScanOutcome> page = pageIterator.next();
                    showPage(page);
                }
            });
        });
    }

    private void showPage(Page<Item, ScanOutcome> page) {
        Platform.runLater(() -> {
            asStream(page)
                    .flatMap(item -> asStream(item.attributes()).map(Map.Entry::getKey))
                    .distinct()
                    .sorted((o1, o2) -> {
                        if (HASH.equalsIgnoreCase(keyTypeMap.get(o1))) {
                            return -1;
                        }
                        if (HASH.equalsIgnoreCase(keyTypeMap.get(o2))) {
                            return 1;
                        }
                        if (RANGE.equalsIgnoreCase(keyTypeMap.get(o1))) {
                            return -1;
                        }
                        if (RANGE.equalsIgnoreCase(keyTypeMap.get(o2))) {
                            return 1;
                        }
                        return o1.compareTo(o2);
                    })
                    .map(attrName -> {
                        String text = attrName;
                        if (HASH.equalsIgnoreCase(keyTypeMap.get(attrName))) {
                            text = "#" + attrName;
                        }
                        if (RANGE.equalsIgnoreCase(keyTypeMap.get(attrName))) {
                            text = "$" + attrName;
                        }
                        TableColumn<Map<String, Object>, String> column = new TableColumn<>(text);
                        column.setPrefWidth(200);
                        column.setCellValueFactory(param -> {
                            Object value = param.getValue().get(attrName);
                            return new SimpleStringProperty(value != null ? value.toString() : "");
                        });
                        return column;
                    })
                    .forEach(tableColumn -> tableView.getColumns().add(tableColumn));

            asStream(page).map(Item::asMap).forEach(map -> rows.add(map));
        });
    }

}
