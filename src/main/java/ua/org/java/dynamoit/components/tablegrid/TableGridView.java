/*
 * This file is part of DynamoIt.
 *
 *     DynamoIt is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     DynamoIt is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with DynamoIt.  If not, see <https://www.gnu.org/licenses/>.
 */

package ua.org.java.dynamoit.components.tablegrid;

import com.amazonaws.services.dynamodbv2.document.Item;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.reactfx.EventStream;
import ua.org.java.dynamoit.components.tablegrid.highlight.Highlighter;
import ua.org.java.dynamoit.model.TableDef;
import ua.org.java.dynamoit.utils.DX;
import ua.org.java.dynamoit.utils.Utils;
import ua.org.java.dynamoit.widgets.ClearableTextField;

import java.io.File;
import java.text.DateFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static javafx.beans.binding.Bindings.*;

public class TableGridView extends VBox {

    private final TableGridModel tableModel;

    private final TableGridController controller;
    private Button clearFilterButton;
    private final TableView<Item> tableView = new TableView<>();

    private Consumer<TableGridContext> onSearchInTable;

    private final Highlighter highlighter = new Highlighter();

    public TableGridView(TableGridController controller, TableGridModel tableModel) {
        this.controller = controller;
        this.tableModel = tableModel;

        buildUI();
        addModelListeners();

        buildTableHeaders();
    }

    private void buildUI() {
        this.getChildren().addAll(
                DX.toolBar(toolBar -> List.of(
                        DX.create(Button::new, button -> {
                            button.setTooltip(new Tooltip("Create a new document"));
                            button.setGraphic(DX.icon("icons/add.png"));
                            button.setOnAction(event -> showCreateItemDialog(""));
                        }),
                        DX.create(Button::new, button -> {
                            button.setTooltip(new Tooltip("Delete selected rows"));
                            button.setGraphic(DX.icon("icons/delete.png"));
                            button.setOnAction(event -> deleteSelectedItems());
                            button.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());
                        }),
                        new Separator(),
                        DX.create(Button::new, button -> {
                            this.clearFilterButton = button;
                            button.setTooltip(new Tooltip("Clear filter"));
                            button.setGraphic(DX.icon("icons/filter_clear.png"));
                            button.setOnAction(event -> clearFilter());
                        }),
                        DX.create(Button::new, button -> {
                            button.setTooltip(new Tooltip("Clear highlighting"));
                            button.setGraphic(DX.icon("icons/color_swatches.png"));
                            button.setOnAction(event -> highlighter.clear());
                        }),
                        DX.create(Button::new, button -> {
                            button.setTooltip(new Tooltip("Refresh rows"));
                            button.setGraphic(DX.icon("icons/table_refresh.png"));
                            button.setOnAction(event -> controller.onRefreshData());
                        }),
                        DX.create(Button::new, button -> {
                            button.setTooltip(new Tooltip("Compare documents"));
                            button.setGraphic(DX.icon("icons/edit_diff.png"));
                            button.disableProperty().bind(
                                    greaterThan(2, size(tableView.getSelectionModel().getSelectedItems()))
                            );
                            button.setOnAction(event -> showCompareDialog());
                        }),
                        DX.create(Button::new, button -> {
                            button.setTooltip(new Tooltip("Patch documents"));
                            button.setGraphic(DX.icon("icons/script.png"));
                            button.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());
                            button.setOnAction(event -> showPatchDialog());
                        }),
                        new Separator(),
                        DX.create(Button::new, button -> {
                            button.setTooltip(new Tooltip("Save table as json"));
                            button.setGraphic(DX.icon("icons/diskette.png"));
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
                        DX.create(Button::new, button -> {
                            button.setTooltip(new Tooltip("Load json into the table"));
                            button.setGraphic(DX.icon("icons/folder_go.png"));
                            button.setOnAction(event -> {
                                FileChooser fileChooser = new FileChooser();
                                FileChooser.ExtensionFilter jsonFiles = new FileChooser.ExtensionFilter("Json files", "*.json");
                                fileChooser.getExtensionFilters().addAll(
                                        new FileChooser.ExtensionFilter("All files", "*.*"),
                                        jsonFiles
                                );
                                fileChooser.setSelectedExtensionFilter(jsonFiles);
                                File file = fileChooser.showOpenDialog(this.getScene().getWindow());
                                if (file != null) {
                                    controller.onLoadFromFile(file);
                                }
                            });
                        }),
                        new Separator(),
                        DX.create(Button::new, button -> {
                            button.setTooltip(new Tooltip("Show table information"));
                            button.setGraphic(DX.icon("icons/information.png"));
                            button.setOnAction(event -> createTableInfoDialog().show());
                        }),
                        DX.spacer(),
                        DX.create(Label::new, t -> {
                            t.textProperty().bind(concat("Count [", tableModel.rowsSizeProperty(), " of ~", tableModel.getTableDef().totalCountProperty(), "]"));
                        })
                )),
                DX.create(() -> this.tableView, tableView -> {
                    tableView.getColumns().add(DX.create((Supplier<TableColumn<Item, Number>>) TableColumn::new, column -> {
                        column.setPrefWidth(35);
                        column.setResizable(false);
                        column.setSortable(false);
                        column.getStyleClass().add("column-index");
                        column.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(tableModel.getRows().indexOf(param.getValue()) + 1));
                    }));

                    VBox.setVgrow(tableView, Priority.ALWAYS);
                    tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                    tableView.setItems(tableModel.getRows());
                    tableView.setSkin(new MyTableViewSkin<>(tableView));
                    tableView.setRowFactory(param -> {
                        TableRow<Item> tableRow = new TableRow<>();
                        tableRow.setOnMouseClicked(event -> {
                            if (event.getClickCount() == 2 && tableRow.getItem() != null) {
                                showEditItemDialog(tableRow.getItem().toJSONPretty());
                            }
                        });
                        return tableRow;
                    });

                    tableView.setOnKeyPressed(event -> {
                        if (!tableView.getSelectionModel().isEmpty()) {
                            if (KeyCode.ENTER == event.getCode()) {
                                if (Utils.isKeyModifierDown(event)) {
                                    if (event.isControlDown() && tableView.getSelectionModel().getSelectedItems().size() > 1) {
                                        showCompareDialog();
                                    }
                                } else {
                                    showEditItemDialog(tableView.getSelectionModel().getSelectedItem().toJSONPretty());
                                }
                            }
                            if (KeyCode.DELETE == event.getCode()) {
                                deleteSelectedItems();
                            }
                        }
                    });
                })
        );
    }

    private void addModelListeners() {
        tableModel.getTableDef().getAttributeTypesMap().addListener((MapChangeListener<String, Attributes.Type>) c -> {
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

        Supplier<Boolean> isFilterClean = () -> tableModel.getAttributeFilterMap().values().stream()
                .map(prop -> prop.get() == null || prop.get().isBlank())
                .reduce(true, (a, b) -> a && b);

        tableModel.getAttributeFilterMap().addListener((MapChangeListener<String, SimpleStringProperty>) change -> {
            SimpleStringProperty valueAdded = change.getValueAdded();
            valueAdded.addListener((observable, oldValue, newValue) -> this.clearFilterButton.setDisable(isFilterClean.get()));
            this.clearFilterButton.setDisable(isFilterClean.get());
        });
    }

    private void buildTableHeaders() {
        List<String> availableAttributes = tableView.getColumns().stream()
                .map(TableColumnBase::getId)
                .collect(Collectors.toList());

        tableModel.getTableDef().getAttributeTypesMap().keySet().stream()
                .filter(attrName -> !availableAttributes.contains(attrName))
                .map(this::buildTableColumn)
                .forEach(tableView.getColumns()::add);

    }

    private TableColumn<Item, String> buildTableColumn(String attrName) {
        SimpleStringProperty filterProperty = tableModel.getAttributeFilterMap().computeIfAbsent(attrName, s -> new SimpleStringProperty());

        return DX.create(TableColumn::new, filter -> {
            filter.setId(attrName);
            filter.getStyleClass().add("table-column-filter");
            filter.setGraphic(DX.create(ClearableTextField::new, textField -> {
                textField.textProperty().bindBidirectional(filterProperty);
                textField.setOnAction(event -> controller.onRefreshData());
                textField.setOnClear(event -> controller.onRefreshData());
            }));
            filter.getColumns().add(DX.create((Supplier<TableColumn<Item, String>>) TableColumn::new, column -> {
                if (attrName.equals(tableModel.getTableDef().getHashAttribute())) {
                    column.setGraphic(DX.icon("icons/key.png"));
                }
                if (attrName.equals(tableModel.getTableDef().getRangeAttribute())) {
                    column.setGraphic(DX.icon("icons/sort_columns.png"));
                }
                column.setText(attrName);
                column.setId(attrName);
                column.setPrefWidth(200);
                column.setCellValueFactory(param -> {
                    Object value = param.getValue().get(attrName);
                    return new SimpleStringProperty(value != null ? value.toString() : "");
                });
                column.setCellFactory(param -> {
                    TableCell<Item, String> cell = new TableCell<>();
                    if (Attributes.Type.NUMBER == tableModel.getTableDef().getAttributeTypesMap().get(attrName)) {
                        cell.setAlignment(Pos.CENTER_RIGHT);
                    }
                    cell.textProperty().bind(cell.itemProperty());
                    attachCellContextMenu(cell, attrName);

                    highlighter.getCriteria(attrName).addListener((ListChangeListener<Highlighter.Criteria>) c -> highlightCellValue(highlighter.getCriteria(attrName), cell));
                    cell.textProperty().addListener(observable -> highlightCellValue(highlighter.getCriteria(attrName), cell));

                    return cell;
                });
            }));
        });
    }

    private void highlightCellValue(ObservableList<Highlighter.Criteria> criteriaList, TableCell<Item, String> cell) {
        criteriaList.stream()
                .filter(criteria -> criteria.match(cell.getText()))
                .findFirst()
                .ifPresentOrElse(criteria -> cell.setStyle(
                        String.format("-fx-background-color: %1s; -fx-text-fill: %2s", criteria.getBackgroundColor(), criteria.getTextColor())
                ), () -> cell.setStyle(null));
    }

    private void attachCellContextMenu(TableCell<Item, String> cell, String attrName) {
        cell.setOnContextMenuRequested(event -> {
            if (cell.getText() != null && cell.getText().trim().length() != 0) {
                String value = Utils.truncateWithDots(cell.textProperty().get());
                DX.contextMenu(contextMenu -> List.of(
                        DX.create(MenuItem::new, menuCopy -> {
                            menuCopy.textProperty().set("Copy      '" + value + "'");
                            menuCopy.setGraphic(DX.icon("icons/page_copy.png"));
                            menuCopy.disableProperty().bind(isEmpty(cell.textProperty()));
                            menuCopy.setOnAction(__ -> copyToClipboard(cell.textProperty().get()));
                        }),
                        DX.create(MenuItem::new, menuFilter -> {
                            menuFilter.textProperty().set("Filter    '" + value + "'");
                            menuFilter.setGraphic(DX.icon("icons/filter_add.png"));
                            menuFilter.disableProperty().bind(isEmpty(cell.textProperty()));
                            menuFilter.setOnAction(__ -> {
                                SimpleStringProperty property = this.tableModel.getAttributeFilterMap().get(attrName);
                                if (property != null) {
                                    property.set(cell.getText());
                                    controller.onRefreshData();
                                }
                            });
                        }),
                        DX.create(MenuItem::new, menuHighlight -> {
                            menuHighlight.textProperty().set("Highlight '" + value + "'");
                            menuHighlight.setGraphic(DX.icon("icons/select_by_color.png"));
                            menuHighlight.disableProperty().bind(isEmpty(cell.textProperty()));
                            menuHighlight.setOnAction(__ -> {
                                SimpleStringProperty property = this.tableModel.getAttributeFilterMap().get(attrName);
                                if (property != null) {
                                    highlighter.addEqHighlighting(attrName, cell.getText());
                                }
                            });
                        }),
                        DX.create(Menu::new, menuSearch -> {
                            menuSearch.textProperty().set("Search    '" + value + "' in");
                            menuSearch.setGraphic(DX.icon("icons/table_tab_search.png"));
                            menuSearch.disableProperty().bind(isEmpty(cell.textProperty()));
                            menuSearch.getItems().add(
                                    DX.create(Menu::new, allTablesMenuItem -> {
                                        allTablesMenuItem.textProperty().bind(concat("All tables"));
                                        allTablesMenuItem.setGraphic(DX.icon("icons/database.png"));
                                        allTablesMenuItem.getItems().addAll(
                                                tableModel.getProfileModel().getAvailableTables().stream().map(tableDef ->
                                                        buildContextMenuByTableDef(tableDef, cell.getText())
                                                ).collect(Collectors.toList())
                                        );
                                    })
                            );
                            menuSearch.getItems().addAll(
                                    tableModel.getProfileModel().getSavedFilters().stream().map(filter ->
                                            DX.create(Menu::new, filterMenuItem -> {
                                                filterMenuItem.textProperty().bind(concat("Contains: " + filter));
                                                filterMenuItem.setGraphic(DX.icon("icons/folder_star.png"));
                                                filterMenuItem.getItems().addAll(
                                                        tableModel.getProfileModel().getAvailableTables().stream()
                                                                .filter(tableDef -> tableDef.getName().contains(filter))
                                                                .map(tableDef ->
                                                                        buildContextMenuByTableDef(tableDef, cell.getText())
                                                                ).collect(Collectors.toList())
                                                );
                                            })
                                    ).collect(Collectors.toList())
                            );
                        }),
                        DX.create(Menu::new, menuEdit -> {
                            menuEdit.setText("Edit document");
                            menuEdit.setGraphic(DX.icon("icons/page_edit.png"));
                            menuEdit.setOnAction(editEvent -> {
                                if (editEvent.getTarget().equals(editEvent.getSource())) {
                                    showEditItemDialog(cell.getTableRow().getItem().toJSONPretty());
                                }
                            });
                            menuEdit.getItems().add(
                                    DX.create(MenuItem::new, menuEditAsNew -> {
                                        menuEditAsNew.setText("Edit as new document");
                                        menuEditAsNew.setOnAction(__ -> showCreateItemDialog(cell.getTableRow().getItem().toJSONPretty()));
                                    })
                            );
                        })
                )).show(cell, event.getScreenX(), event.getScreenY());
            }
        });

    }

    private void clearFilter() {
        controller.onClearFilters();
    }

    private void showEditItemDialog(String json) {
        showItemDialog(String.format("[%1s] Edit the document", tableModel.getTableName()), json, controller::onUpdateItem, controller::validateItem);
    }

    private void showCreateItemDialog(String json) {
        showItemDialog(String.format("[%1s] Create a new document", tableModel.getTableName()), json, controller::onCreateItem, controller::validateItem);
    }

    private void showPatchDialog() {
        if (!tableView.getSelectionModel().getSelectedItems().isEmpty()) {
            showItemDialog(String.format("[%1s] Patch selected documents", tableModel.getTableName()), "{\n\n}",
                    json -> controller.onPatchItems(tableView.getSelectionModel().getSelectedItems(), json),
                    stringEventStream -> controller.validateItem(stringEventStream, true));
        }
    }

    private void showItemDialog(String title, String json, Consumer<String> onSaveConsumer, Function<EventStream<String>, EventStream<Boolean>> validator) {
        ItemDialog dialog = new ItemDialog(title, json, validator);

        dialog.showAndWait().ifPresent(onSaveConsumer);
    }

    private void showCompareDialog() {
        if (tableView.getSelectionModel().getSelectedItems().size() >= 2) {
            Item item1 = tableView.getSelectionModel().getSelectedItems().get(0);
            Item item2 = tableView.getSelectionModel().getSelectedItems().get(1);

            CompareDialog dialog = new CompareDialog(item1.toJSONPretty(), item2.toJSONPretty());
            dialog.showAndWait();
        }
    }

    private void deleteSelectedItems() {
        List<Item> items = tableView.getSelectionModel().getSelectedItems();
        Alert deleteConfirmation = new Alert(Alert.AlertType.CONFIRMATION, "Do you really want to delete " + items.size() + " item(s)?");
        Optional<ButtonType> pressedButton = deleteConfirmation.showAndWait();
        pressedButton.ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                controller.onDeleteItems(items);
            }
        });
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

    private MenuItem buildContextMenuByTableDef(TableDef tableDef, String value) {
        return DX.create(Menu::new, menu -> {
            menu.setText(tableDef.getName());
            menu.setOnAction(event -> {
                //issue#1
                if (onSearchInTable != null && event.getTarget().equals(event.getSource())) {
                    onSearchInTable.accept(new TableGridContext(tableModel.getProfile(), tableDef.getName(), tableDef.getHashAttribute(), value));
                }
            });
            menu.getItems().addAll(
                    tableDef.getAttributeTypesMap().keySet().stream()
                            .map(attr -> DX.create(MenuItem::new, menuItem -> {
                                menuItem.setText(attr);
                                menuItem.setOnAction(__ -> {
                                    if (onSearchInTable != null) {
                                        onSearchInTable.accept(new TableGridContext(tableModel.getProfile(), tableDef.getName(), attr, value));
                                    }
                                });
                            }))
                            .collect(Collectors.toList())
            );
        });
    }

    private Dialog<?> createTableInfoDialog() {
        return DX.create(Dialog::new, dialog -> {
            dialog.setTitle(tableModel.getTableName());
            dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
            dialog.setResizable(true);
            dialog.getDialogPane().setContent(DX.create(GridPane::new, gridPane -> {
                gridPane.setHgap(10);
                gridPane.setVgap(10);

                gridPane.getColumnConstraints().addAll(
                        new ColumnConstraints(),
                        DX.create(ColumnConstraints::new, c -> {
                            c.setHgrow(Priority.ALWAYS);
                        })
                );

                Function<Supplier<String>, Node> copyClipboardImage = stringSupplier -> DX.create(() -> DX.icon("icons/page_copy.png"), icon -> {
                    icon.setOnMouseClicked(__ -> copyToClipboard(stringSupplier.get()));
                    icon.setStyle("-fx-cursor: hand");
                });

                gridPane.addColumn(0,
                        DX.boldLabel("Name:"),
                        DX.boldLabel("Arn:"),
                        DX.boldLabel("Creation date:"),
                        DX.boldLabel("Size:"),
                        DX.boldLabel("Region:")
                );

                gridPane.addColumn(1,
                        DX.create(Hyperlink::new, link -> {
                            String tableLink = String.format(
                                    "https://%1$s.console.aws.amazon.com/dynamodb/home?region=%1$s#tables:selected=%2$s;tab=overview",
                                    tableModel.getProfileModel().getRegion(),
                                    tableModel.getTableName()
                            );
                            link.setText(tableModel.getOriginalTableDescription().getTableName().trim());
                            link.setOnMouseClicked(event -> controller.openUrl(tableLink));
                        }),
                        new Label(tableModel.getOriginalTableDescription().getTableArn()),
                        new Label(DateFormat.getInstance().format(tableModel.getOriginalTableDescription().getCreationDateTime())),
                        new Label(tableModel.getOriginalTableDescription().getTableSizeBytes() + " bytes"),
                        new Label(tableModel.getProfileModel().getRegion())
                );

                gridPane.addColumn(2,
                        copyClipboardImage.apply(() -> tableModel.getOriginalTableDescription().getTableName()),
                        copyClipboardImage.apply(() -> tableModel.getOriginalTableDescription().getTableArn()),
                        copyClipboardImage.apply(() -> DateFormat.getInstance().format(tableModel.getOriginalTableDescription().getCreationDateTime())),
                        copyClipboardImage.apply(() -> "" + tableModel.getOriginalTableDescription().getTableSizeBytes()),
                        copyClipboardImage.apply(() -> tableModel.getProfileModel().getRegion())
                );

                String streamArn = tableModel.getOriginalTableDescription().getLatestStreamArn();
                if (streamArn != null && !streamArn.isBlank()) {
                    gridPane.addRow(gridPane.getRowCount(), DX.boldLabel("Stream:"), new Label(streamArn), copyClipboardImage.apply(() -> streamArn));
                }
            }));
        });
    }

    private static void copyToClipboard(String value) {
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard clipboard = Clipboard.getSystemClipboard();
        clipboard.setContent(content);
    }
}
