package ua.org.java.dynamoit.components.tablegrid;

import com.amazonaws.services.dynamodbv2.document.Item;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.scene.control.*;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.controlsfx.control.textfield.TextFields;
import org.fxmisc.flowless.VirtualizedScrollPane;
import ua.org.java.dynamoit.components.jsoneditor.JsonEditor;
import ua.org.java.dynamoit.utils.DX;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TableGridView extends VBox {

    private final TableGridModel tableModel;

    private final TableGridController controller;
    private Button deleteSelectedButton;
    private javafx.scene.control.TableView<Item> tableView;

    private Consumer<TableGridContext> onSearchInTable;

    public TableGridView(TableGridController controller, TableGridModel tableModel) {
        this.controller = controller;
        this.tableModel = tableModel;

        buildUI();
        addModelListeners();
    }

    private void buildUI() {
        this.getChildren().addAll(
                List.of(
                        DX.toolBar(toolBar -> List.of(
                                DX.create(Button::new, button -> {
                                    button.setTooltip(new Tooltip("Create a new item"));
                                    button.setGraphic(DX.icon("icons/table_row_insert.png"));
                                    button.setOnAction(event -> showItemDialog("Create a new item", "New document in JSON format", "", controller::onCreateItem));
                                }),
                                DX.create(Button::new, button -> {
                                    deleteSelectedButton = button;
                                    button.setTooltip(new Tooltip("Delete selected rows"));
                                    button.setGraphic(DX.icon("icons/table_row_delete.png"));
                                    button.setOnAction(event -> deleteSelectedItem());
                                }),
                                DX.create(Button::new, button -> {
                                    button.setTooltip(new Tooltip("Clear filter"));
                                    button.setGraphic(DX.icon("icons/filter_clear.png"));
                                    button.setOnAction(event -> clearFilter());
                                }),
                                DX.create(Button::new, button -> {
                                    button.setTooltip(new Tooltip("Refresh rows"));
                                    button.setGraphic(DX.icon("icons/table_refresh.png"));
                                    button.setOnAction(event -> controller.onRefreshData());
                                }),
                                DX.create(Button::new, button -> {
                                    button.setTooltip(new Tooltip("Save table as json"));
                                    button.setGraphic(DX.icon("icons/table_save.png"));
                                    button.setOnAction(event -> {
                                        FileChooser fileChooser = new FileChooser();
                                        fileChooser.setInitialFileName(tableModel.getTableName() + ".json");
                                        FileChooser.ExtensionFilter jsonFiles = new FileChooser.ExtensionFilter("Json files", "*.json");
                                        fileChooser.getExtensionFilters().addAll(
                                                new FileChooser.ExtensionFilter("All files", "*.*"),
                                                jsonFiles
                                        );
                                        fileChooser.setSelectedExtensionFilter(jsonFiles);
                                        File file = fileChooser.showSaveDialog(this.getScene().getWindow());
                                        if (file != null) {
                                            controller.onSaveToFile(file);
                                        }
                                    });
                                }),
                                DX.spacer(),
                                DX.create(Label::new, t -> {
                                    t.textProperty().bind(Bindings.concat("Count [", tableModel.rowsSizeProperty(), " of ~", tableModel.totalCountProperty(), "]"));
                                })
                        )),
                        DX.create((Supplier<javafx.scene.control.TableView<Item>>) javafx.scene.control.TableView::new, tableView -> {
                            this.tableView = tableView;

                            tableView.getColumns().add(DX.create((Supplier<TableColumn<Item, Number>>) TableColumn::new, column -> {
                                column.setPrefWidth(35);
                                column.setResizable(false);
                                column.setStyle( "-fx-alignment: CENTER-RIGHT;");
                                column.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(tableModel.getRows().indexOf(param.getValue()) + 1));
                            }));

                            VBox.setVgrow(tableView, Priority.ALWAYS);
                            tableView.setItems(tableModel.getRows());
                            tableView.setSkin(new MyTableViewSkin<>(tableView));
                            tableView.setRowFactory(param -> {
                                TableRow<Item> tableRow = new TableRow<>();
                                tableRow.setOnMouseClicked(event -> {
                                    if (event.getClickCount() == 2 && tableRow.getItem() != null) {
                                        showItemDialog("Edit the item", "Document in JSON format", tableRow.getItem().toJSONPretty(), controller::onUpdateItem);
                                    }
                                });
                                return tableRow;
                            });

                            deleteSelectedButton.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());
                        })
                )
        );
    }

    private void addModelListeners() {
        tableModel.getAttributeTypesMap().addListener((MapChangeListener<String, Attributes.Type>) c -> {
            if (c.wasAdded()) {
                buildTableHeaders();
            }
        });

        tableModel.getRows().addListener((ListChangeListener<Item>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    tableView.scrollTo(c.getFrom());
                }
            }
        });
    }

    private void buildTableHeaders() {
        List<String> availableAttributes = tableView.getColumns().stream()
                .map(TableColumnBase::getId)
                .collect(Collectors.toList());

        tableModel.getAttributeTypesMap().keySet().stream()
                .filter(attrName -> !availableAttributes.contains(attrName))
                .map(this::buildTableColumn)
                .forEach(tableView.getColumns()::add);

    }

    private TableColumn<Item, String> buildTableColumn(String attrName) {
        SimpleStringProperty filterProperty = tableModel.getAttributeFilterMap().computeIfAbsent(attrName, s -> new SimpleStringProperty());

        return DX.create(TableColumn::new, filter -> {
            filter.setId(attrName);
            filter.setGraphic(DX.create(TextFields::createClearableTextField, textField -> {
                textField.textProperty().bindBidirectional(filterProperty);
                textField.setOnAction(event -> controller.onRefreshData());
            }));
            filter.getColumns().add(DX.create((Supplier<TableColumn<Item, String>>) TableColumn::new, column -> {
                if (attrName.equals(tableModel.getHashAttribute())) {
                    column.setGraphic(DX.icon("icons/key.png"));
                }
                if (attrName.equals(tableModel.getRangeAttribute())) {
                    column.setGraphic(DX.icon("icons/sort_columns.png"));
                }
                column.setText(attrName);
                column.setId(attrName);
                column.setPrefWidth(200);
                column.setCellValueFactory(param -> {
                    Object value = param.getValue().get(attrName);
                    return new SimpleStringProperty(value != null ? value.toString() : "");
                });
                buildCellContextMenu(column);
            }));
        });
    }

    private void buildCellContextMenu(TableColumn<Item, String> column) {
        column.setCellFactory(param -> {
            TableCell<Item, String> cell = new TableCell<>();
            cell.textProperty().bind(cell.itemProperty());

            cell.setOnContextMenuRequested(event -> {
                if (cell.getText() != null && cell.getText().trim().length() != 0) {
                    DX.contextMenu(contextMenu -> List.of(
                            DX.create(MenuItem::new, menuCopy -> {
                                menuCopy.textProperty().bind(Bindings.concat("Copy '", cell.textProperty(), "'"));
                                menuCopy.setGraphic(DX.icon("icons/page_copy.png"));
                                menuCopy.disableProperty().bind(Bindings.isEmpty(cell.textProperty()));
                                menuCopy.setOnAction(__ -> {
                                    ClipboardContent content = new ClipboardContent();
                                    content.putString(cell.textProperty().get());
                                    Clipboard clipboard = Clipboard.getSystemClipboard();
                                    clipboard.setContent(content);
                                });
                            }),
                            DX.create(MenuItem::new, menuFilter -> {
                                menuFilter.textProperty().bind(Bindings.concat("Filter '", cell.textProperty(), "'"));
                                menuFilter.setGraphic(DX.icon("icons/filter_add.png"));
                                menuFilter.disableProperty().bind(Bindings.isEmpty(cell.textProperty()));
                                menuFilter.setOnAction(__ -> {
                                    SimpleStringProperty property = this.tableModel.getAttributeFilterMap().get(column.getId());
                                    if (property != null) {
                                        property.set(cell.getText());
                                        controller.onRefreshData();
                                    }
                                });
                            }),
                            DX.create(Menu::new, menuSearch -> {
                                menuSearch.textProperty().bind(Bindings.concat("Search '", cell.textProperty(), "' in"));
                                menuSearch.setGraphic(DX.icon("icons/table_tab_search.png"));
                                menuSearch.disableProperty().bind(Bindings.isEmpty(cell.textProperty()));
                                menuSearch.getItems().add(
                                        DX.create(Menu::new, allTablesMenuItem -> {
                                            allTablesMenuItem.textProperty().bind(Bindings.concat("All tables"));
                                            allTablesMenuItem.setGraphic(DX.icon("icons/database.png"));
                                            allTablesMenuItem.getItems().addAll(
                                                    tableModel.getMainModel().getAvailableTables().stream().map(tableName ->
                                                            DX.create(MenuItem::new, menuItem -> {
                                                                menuItem.setText(tableName);
                                                                menuItem.setOnAction(__ -> {
                                                                    if (onSearchInTable != null) {
                                                                        onSearchInTable.accept(new TableGridContext(tableModel.getMainModel().getSelectedProfile(), tableName, column.getId(), cell.getText()));
                                                                    }
                                                                });
                                                            })
                                                    ).collect(Collectors.toList())
                                            );
                                        })
                                );
                                menuSearch.getItems().addAll(
                                        tableModel.getMainModel().getSavedFilters().stream().map(filter ->
                                                DX.create(Menu::new, filterMenuItem -> {
                                                    filterMenuItem.textProperty().bind(Bindings.concat("Contains: " + filter));
                                                    filterMenuItem.setGraphic(DX.icon("icons/folder_star.png"));
                                                    filterMenuItem.getItems().addAll(
                                                            tableModel.getMainModel().getAvailableTables().stream()
                                                                    .filter(tableName -> tableName.contains(filter))
                                                                    .map(tableName ->
                                                                            DX.create(MenuItem::new, menuItem -> {
                                                                                menuItem.setText(tableName);
                                                                                menuItem.setOnAction(__ -> {
                                                                                    if (onSearchInTable != null) {
                                                                                        onSearchInTable.accept(new TableGridContext(tableModel.getMainModel().getSelectedProfile(), tableName, column.getId(), cell.getText()));
                                                                                    }
                                                                                });
                                                                            })
                                                                    ).collect(Collectors.toList())
                                                    );
                                                })
                                        ).collect(Collectors.toList())
                                );
                            })
                    )).show(cell, event.getScreenX(), event.getScreenY());
                }
            });

            return cell;
        });
    }

    private void clearFilter() {
        controller.onClearFilters();
    }

    private void showItemDialog(String title, String promptText, String json, Consumer<String> onSaveConsumer) {
        JsonEditor textArea = new JsonEditor(json);
//        textArea.setPromptText(promptText);
        textArea.setPrefWidth(800);
        textArea.setPrefHeight(800);
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        ((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/page.png"));
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CLOSE);
        dialog.getDialogPane().setContent(new VirtualizedScrollPane<>(textArea));
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
        controller.onDeleteItem(item);
    }

    private class MyTableViewSkin<T> extends TableViewSkin<T> {

        public MyTableViewSkin(javafx.scene.control.TableView<T> control) {
            super(control);

            getVirtualFlow().positionProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.doubleValue() == 1.0) {
                    controller.onReachScrollEnd();
                }
            });
        }
    }

    public void setOnSearchInTable(Consumer<TableGridContext> onSearchInTable) {
        this.onSearchInTable = onSearchInTable;
    }
}
