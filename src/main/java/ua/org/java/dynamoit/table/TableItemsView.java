package ua.org.java.dynamoit.table;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Page;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import ua.org.java.dynamoit.utils.DX;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static ua.org.java.dynamoit.utils.Utils.asStream;

public class TableItemsView extends VBox {

    private static final String HASH = "HASH";
    private static final String RANGE = "RANGE";

    private TableController controller;
    private TableView<Item> tableView;
    private ObservableList<Item> rows = FXCollections.observableArrayList();
    private IntegerBinding rowsSize = Bindings.createIntegerBinding(() -> rows.size(), rows);
    private TableDescription tableDescription;
    private Map<String, String> keyTypeMap;
    private Page<Item, ScanOutcome> currentPage;
    private SimpleStringProperty totalCount = new SimpleStringProperty();
    private Map<String, SimpleStringProperty> attributeFilterMap = new HashMap<>();

    public TableItemsView(TableController controller) {
        this.controller = controller;

        this.getChildren().addAll(
                List.of(
                        DX.toolBar(toolBar -> List.of(
                                DX.create(Button::new, button -> {
                                    button.setTooltip(new Tooltip("Clear filter"));
                                    button.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.FILTER));
                                    button.setOnAction(event -> clearFilter());
                                }),
                                DX.create(Button::new, button -> {
                                    button.setTooltip(new Tooltip("Delete selected rows"));
                                    button.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
                                }),
                                DX.spacer(),
                                DX.create(Label::new, t -> {
                                    t.textProperty().bind(Bindings.concat("Count [", rowsSize, " of ~", totalCount, "]"));
                                })
                        )),
                        DX.create((Supplier<TableView<Item>>) TableView::new, tableView -> {
                            this.tableView = tableView;
                            VBox.setVgrow(tableView, Priority.ALWAYS);
                            tableView.setItems(rows);
                            tableView.setSkin(new MyTableViewSkin<>(tableView));
                            tableView.setRowFactory(param -> {
                                TableRow<Item> tableRow = new TableRow<>();
                                tableRow.setOnMouseClicked(event -> {
                                    if (event.getClickCount() == 2) {
                                        Item item = tableRow.getItem();
                                        Dialog<String> dialog = new Dialog<>();
                                        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
                                        dialog.getDialogPane().setContent(new TextArea(item.toJSONPretty()));
                                        dialog.initModality(Modality.NONE);
                                        dialog.setResizable(true);
                                        dialog.show();
                                    }
                                });
                                return tableRow;
                            });
                        })
                )
        );

        controller.describeTable().thenAccept(describeTableResult -> Platform.runLater(() -> {
            tableDescription = describeTableResult.getTable();
            totalCount.set(tableDescription.getItemCount().toString());
            keyTypeMap = tableDescription.getKeySchema().stream().collect(Collectors.toMap(KeySchemaElement::getAttributeName, KeySchemaElement::getKeyType));
        })).thenRunAsync(() -> {
            controller.scanItems().thenAccept(items -> {
                Iterator<Page<Item, ScanOutcome>> pageIterator = items.pages().iterator();
                if (pageIterator.hasNext()) {
                    currentPage = pageIterator.next();
                    Platform.runLater(() -> {
                        buildTableHeaders(currentPage);
                        showPage(currentPage);
                    });
                }
            });
        });
    }

    private void buildTableHeaders(Page<Item, ScanOutcome> page) {
        TableColumn<Item, Number> indexColumn = new TableColumn<>();
        indexColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(rows.indexOf(param.getValue())));

        tableView.getColumns().add(indexColumn);

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
                    TableColumn<Item, String> column = new TableColumn<>(text);
                    column.setPrefWidth(200);
                    column.setCellValueFactory(param -> {
                        Object value = param.getValue().get(attrName);
                        return new SimpleStringProperty(value != null ? value.toString() : "");
                    });
                    buildCellContextMenu(column);


                    SimpleStringProperty filterProperty = new SimpleStringProperty();
                    attributeFilterMap.put(attrName, filterProperty);
                    TableColumn<Item, String> filter = new TableColumn<>();
                    TextField textField = new TextField();
                    textField.textProperty().bindBidirectional(filterProperty);
                    filter.setGraphic(textField);
                    filter.getColumns().add(column);
                    return filter;
                })
                .forEach(tableColumn -> tableView.getColumns().add(tableColumn));
    }

    private void buildCellContextMenu(TableColumn<Item, String> column) {
        column.setCellFactory(param -> {
            TableCell<Item, String> cell = new TableCell<>();
            cell.textProperty().bind(cell.itemProperty());

            cell.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null || newValue.trim().length() == 0) {
                    cell.setContextMenu(null);
                } else {
                    cell.setContextMenu(
                            DX.contextMenu(contextMenu -> List.of(
                                    DX.create((Supplier<MenuItem>) MenuItem::new, menuItem -> {
                                        menuItem.textProperty().bind(Bindings.concat("Copy '", cell.textProperty(), "'"));
                                        menuItem.disableProperty().bind(Bindings.isEmpty(cell.textProperty()));
                                        menuItem.setOnAction(event -> {
                                            ClipboardContent content = new ClipboardContent();
                                            content.putString(cell.textProperty().get());
                                            Clipboard clipboard = Clipboard.getSystemClipboard();
                                            clipboard.setContent(content);
                                        });
                                    })
                            ))
                    );
                }
            });

            return cell;
        });
    }

    private void showPage(Page<Item, ScanOutcome> page) {
        int count = rows.size();
        asStream(page).forEach(item -> rows.add(item));
        tableView.scrollTo(count);
    }

    private void clearFilter(){
        attributeFilterMap.values().forEach(simpleStringProperty -> simpleStringProperty.set(null));
    }

    private class MyTableViewSkin<T> extends TableViewSkin<T> {

        public MyTableViewSkin(TableView<T> control) {
            super(control);

            getVirtualFlow().positionProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.doubleValue() == 1.0) {
                    if (currentPage.hasNextPage()) {
                        CompletableFuture.runAsync(() -> {
                            currentPage = currentPage.nextPage();
                        }).thenRun(() -> Platform.runLater(() -> {
                            showPage(currentPage);
                        }));
                    }
                }
            });
        }
    }

}
