package ua.org.java.dynamoit.table;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Page;
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
import ua.org.java.dynamoit.MainModel;
import ua.org.java.dynamoit.utils.DX;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static ua.org.java.dynamoit.utils.Utils.asStream;

public class TableItemsView extends VBox {

    private static final String HASH = "HASH";
    private static final String RANGE = "RANGE";

    private MainModel mainModel;

    private TableController controller;
    private Button deleteSelectedButton;
    private TableView<Item> tableView;
    private ObservableList<Item> rows = FXCollections.observableArrayList();
    private IntegerBinding rowsSize = Bindings.createIntegerBinding(() -> rows.size(), rows);
    private TableDescription tableDescription;
    private Map<String, String> keyTypeMap;
    private Page<Item, ?> currentPage;
    private SimpleStringProperty totalCount = new SimpleStringProperty();
    private Map<String, SimpleStringProperty> attributeFilterMap = new HashMap<>();

    private Consumer<TableContext> onSearchInTable;

    public TableItemsView(TableContext context, TableController controller, MainModel mainModel) {
        this.controller = controller;
        this.mainModel = mainModel;


        this.getChildren().addAll(
                List.of(
                        DX.toolBar(toolBar -> List.of(
                                DX.create(Button::new, button -> {
                                    button.setTooltip(new Tooltip("Create a new item"));
                                    button.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PLUS));
                                    button.setOnAction(event -> showItemDialog("Create a new item", "New document in JSON format", "", json -> controller.createItem(json).thenRun(() -> Platform.runLater(this::applyFilter))));
                                }),
                                DX.create(Button::new, button -> {
                                    button.setTooltip(new Tooltip("Clear filter"));
                                    button.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.FILTER));
                                    button.setOnAction(event -> clearFilter());
                                }),
                                DX.create(Button::new, button -> {
                                    deleteSelectedButton = button;
                                    button.setTooltip(new Tooltip("Delete selected rows"));
                                    button.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
                                    button.setOnAction(event -> deleteSelectedItem());
                                }),
                                DX.create(Button::new, button -> {
                                    button.setTooltip(new Tooltip("Refresh rows"));
                                    button.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.REFRESH));
                                    button.setOnAction(event -> applyFilter());
                                }),
                                DX.spacer(),
                                DX.create(Label::new, t -> {
                                    t.textProperty().bind(Bindings.concat("Count [", rowsSize, " of ~", totalCount, "]"));
                                })
                        )),
                        DX.create((Supplier<TableView<Item>>) TableView::new, tableView -> {
                            this.tableView = tableView;

                            tableView.getColumns().add(DX.create((Supplier<TableColumn<Item, Number>>) TableColumn::new, column -> {
                                column.setPrefWidth(35);
                                column.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(rows.indexOf(param.getValue())));
                            }));

                            VBox.setVgrow(tableView, Priority.ALWAYS);
                            tableView.setItems(rows);
                            tableView.setSkin(new MyTableViewSkin<>(tableView));
                            tableView.setRowFactory(param -> {
                                TableRow<Item> tableRow = new TableRow<>();
                                tableRow.setOnMouseClicked(event -> {
                                    if (event.getClickCount() == 2) {
                                        showItemDialog("Edit the item", "Document in JSON format", tableRow.getItem().toJSONPretty(), json -> controller.updateItem(json).thenRun(() -> Platform.runLater(this::applyFilter)));
                                    }
                                });
                                return tableRow;
                            });

                            deleteSelectedButton.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());
                        })
                )
        );

        controller.describeTable().thenAccept(describeTableResult -> Platform.runLater(() -> {
            tableDescription = describeTableResult.getTable();
            totalCount.set(tableDescription.getItemCount().toString());
            keyTypeMap = tableDescription.getKeySchema().stream().collect(Collectors.toMap(KeySchemaElement::getAttributeName, KeySchemaElement::getKeyType));
        })).thenRunAsync(() -> controller.queryPageItems(attributeFilterMap)
                .thenAccept(page -> {
                    currentPage = page;
                    Platform.runLater(() -> {
                        buildTableHeaders(currentPage);
                        showPage(currentPage);
                    });
                }));
    }

    private void buildTableHeaders(Page<Item, ?> page) {
        List<String> availableAttributes = tableView.getColumns().stream().map(TableColumnBase::getColumns).filter(Objects::nonNull).flatMap(Collection::stream).map(TableColumnBase::getId).collect(Collectors.toList());

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
                .filter(attrName -> !availableAttributes.contains(attrName))
                .map(attrName -> {
                    SimpleStringProperty filterProperty = new SimpleStringProperty();
                    attributeFilterMap.put(attrName, filterProperty);

                    return DX.create((Supplier<TableColumn<Item, String>>) TableColumn::new, filter -> {
                        TextField textField = new TextField();
                        textField.textProperty().bindBidirectional(filterProperty);
                        textField.setOnAction(event -> applyFilter());
                        filter.setGraphic(textField);
                        filter.getColumns().add(DX.create((Supplier<TableColumn<Item, String>>) TableColumn::new, column -> {
                            String text = attrName;
                            if (HASH.equalsIgnoreCase(keyTypeMap.get(attrName))) {
                                text = "#" + attrName;
                            }
                            if (RANGE.equalsIgnoreCase(keyTypeMap.get(attrName))) {
                                text = "$" + attrName;
                            }
                            column.setText(text);
                            column.setId(attrName);
                            column.setPrefWidth(200);
                            column.setCellValueFactory(param -> {
                                Object value = param.getValue().get(attrName);
                                return new SimpleStringProperty(value != null ? value.toString() : "");
                            });
                            buildCellContextMenu(column);
                        }));
                    });
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
                                    DX.create(MenuItem::new, menuCopy -> {
                                        menuCopy.textProperty().bind(Bindings.concat("Copy '", cell.textProperty(), "'"));
                                        menuCopy.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.COPY));
                                        menuCopy.disableProperty().bind(Bindings.isEmpty(cell.textProperty()));
                                        menuCopy.setOnAction(event -> {
                                            ClipboardContent content = new ClipboardContent();
                                            content.putString(cell.textProperty().get());
                                            Clipboard clipboard = Clipboard.getSystemClipboard();
                                            clipboard.setContent(content);
                                        });
                                    }),
                                    DX.create(MenuItem::new, menuFilter -> {
                                        menuFilter.textProperty().bind(Bindings.concat("Filter '", cell.textProperty(), "'"));
                                        menuFilter.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.FILTER));
                                        menuFilter.disableProperty().bind(Bindings.isEmpty(cell.textProperty()));
                                        menuFilter.setOnAction(event -> {
                                            SimpleStringProperty property = this.attributeFilterMap.get(column.getId());
                                            if (property != null) {
                                                property.set(cell.getText());
                                                applyFilter();
                                            }
                                        });
                                    }),
                                    DX.create(Menu::new, menuSearch -> {
                                        menuSearch.textProperty().bind(Bindings.concat("Search '", cell.textProperty(), "' in"));
                                        menuSearch.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.SEARCH));
                                        menuSearch.disableProperty().bind(Bindings.isEmpty(cell.textProperty()));
                                        menuSearch.getItems().add(
                                                DX.create(Menu::new, allTablesMenuItem -> {
                                                    allTablesMenuItem.textProperty().bind(Bindings.concat("All tables"));
                                                    allTablesMenuItem.getItems().addAll(
                                                            mainModel.getAvailableTables().stream().map(tableName ->
                                                                    DX.create(MenuItem::new, menuItem -> {
                                                                        menuItem.setText(tableName);
                                                                        menuItem.setOnAction(event -> {
                                                                            if (onSearchInTable != null) {
                                                                                onSearchInTable.accept(new TableContext(mainModel.getSelectedProfile(), tableName, column.getId(), cell.getText()));
                                                                            }
                                                                        });
                                                                    })
                                                            ).collect(Collectors.toList())
                                                    );
                                                })
                                        );
                                        menuSearch.getItems().addAll(
                                                mainModel.getSavedFilters().stream().map(filter ->
                                                        DX.create(Menu::new, filterMenuItem -> {
                                                            filterMenuItem.textProperty().bind(Bindings.concat("Contains: " + filter));
                                                            filterMenuItem.getItems().addAll(
                                                                    mainModel.getAvailableTables().stream()
                                                                            .filter(tableName -> tableName.contains(filter))
                                                                            .map(tableName ->
                                                                                    DX.create(MenuItem::new, menuItem -> {
                                                                                        menuItem.setText(tableName);
                                                                                        menuItem.setOnAction(event -> {
                                                                                            if (onSearchInTable != null) {
                                                                                                onSearchInTable.accept(new TableContext(mainModel.getSelectedProfile(), tableName, column.getId(), cell.getText()));
                                                                                            }
                                                                                        });
                                                                                    })
                                                                            ).collect(Collectors.toList())
                                                            );
                                                        })
                                                ).collect(Collectors.toList())
                                        );
                                    })
                            ))
                    );
                }
            });

            return cell;
        });
    }

    private void showPage(Page<Item, ?> page) {
        int count = rows.size();
        asStream(page).forEach(item -> rows.add(item));
        tableView.scrollTo(count);
    }


    private void clearFilter() {
        attributeFilterMap.values().forEach(simpleStringProperty -> simpleStringProperty.set(null));
        applyFilter();
    }

    private void applyFilter() {
        rows.clear();
        controller.queryPageItems(attributeFilterMap).thenAccept(page -> {

            currentPage = page;
            Platform.runLater(() -> {
                buildTableHeaders(currentPage);
                showPage(currentPage);
            });
        });
    }

    private void showItemDialog(String title, String promptText, String json, Consumer<String> onSaveConsumer) {
        TextArea textArea = new TextArea(json);
        textArea.setPromptText(promptText);
        textArea.setPrefWidth(800);
        textArea.setPrefHeight(800);
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CLOSE);
        dialog.getDialogPane().setContent(textArea);
        dialog.initModality(Modality.NONE);
        dialog.setResizable(true);
        dialog.setResultConverter(param -> {
            if (param == saveButton) {
                return textArea.getText();
            }
            return null;
        });
        dialog.showAndWait().ifPresent(onSaveConsumer);
    }

    private void deleteSelectedItem() {
        Item item = tableView.getSelectionModel().getSelectedItem();
        controller.delete(item).thenRun(() -> Platform.runLater(this::applyFilter));
    }

    private class MyTableViewSkin<T> extends TableViewSkin<T> {

        public MyTableViewSkin(TableView<T> control) {
            super(control);

            getVirtualFlow().positionProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.doubleValue() == 1.0) {
                    if (currentPage.hasNextPage()) {
                        CompletableFuture
                                .runAsync(() -> currentPage = currentPage.nextPage())
                                .thenRun(() -> Platform.runLater(() -> showPage(currentPage)));
                    }
                }
            });
        }
    }

    public void setOnSearchInTable(Consumer<TableContext> onSearchInTable) {
        this.onSearchInTable = onSearchInTable;
    }
}
