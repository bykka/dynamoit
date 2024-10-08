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

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.reactfx.EventStream;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.internal.document.DefaultEnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import ua.org.java.dynamoit.components.tablegrid.highlight.Highlighter;
import ua.org.java.dynamoit.components.thememanager.ThemeManager;
import ua.org.java.dynamoit.utils.DX;
import ua.org.java.dynamoit.utils.Utils;
import ua.org.java.dynamoit.widgets.ClearableTextField;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static atlantafx.base.theme.Styles.BUTTON_ICON;
import static javafx.beans.binding.Bindings.*;
import static ua.org.java.dynamoit.utils.Utils.copyToClipboard;
import static ua.org.java.dynamoit.utils.Utils.uglyToPrettyJson;

public class TableGridView extends VBox {

    private static final int PADDING = 7 + 7; // left + right paddings
    private static final int FONT_SIZE = 10;

    private final TableGridModel tableModel;
    private final ThemeManager themeManager;

    private final TableGridController controller;
    private Button clearFilterButton;
    private final TableView<EnhancedDocument> tableView = new TableView<>();

    private final Highlighter highlighter = new Highlighter();

    public TableGridView(TableGridController controller, TableGridModel tableModel, ThemeManager themeManager) {
        this.controller = controller;
        this.tableModel = tableModel;
        this.themeManager = themeManager;

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
                            button.getStyleClass().addAll(BUTTON_ICON);
                            button.setOnAction(event -> showCreateItemDialog(DefaultEnhancedDocument.builder().build()));
                        }),
                        DX.create(Button::new, button -> {
                            button.setTooltip(new Tooltip("Delete selected rows"));
                            button.setGraphic(DX.icon("icons/delete.png"));
                            button.getStyleClass().addAll(BUTTON_ICON);
                            button.setOnAction(event -> deleteSelectedItems());
                            button.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());
                        }),
                        new Separator(),
                        DX.create(Button::new, button -> {
                            this.clearFilterButton = button;
                            button.setTooltip(new Tooltip("Clear filter"));
                            button.setGraphic(DX.icon("icons/filter_clear.png"));
                            button.getStyleClass().addAll(BUTTON_ICON);
                            button.setOnAction(event -> clearFilter());
                        }),
                        DX.create(Button::new, button -> {
                            button.setTooltip(new Tooltip("Clear highlighting"));
                            button.setGraphic(DX.icon("icons/color_swatches.png"));
                            button.getStyleClass().addAll(BUTTON_ICON);
                            button.setOnAction(event -> highlighter.clear());
                        }),
                        DX.create(Button::new, button -> {
                            button.setTooltip(new Tooltip("Refresh rows"));
                            button.setGraphic(DX.icon("icons/table_refresh.png"));
                            button.getStyleClass().addAll(BUTTON_ICON);
                            button.setOnAction(event -> reloadData());
                        }),
                        DX.create(Button::new, button -> {
                            button.setTooltip(new Tooltip("Compare documents"));
                            button.setGraphic(DX.icon("icons/edit_diff.png"));
                            button.getStyleClass().addAll(BUTTON_ICON);
                            button.disableProperty().bind(
                                    greaterThan(2, size(tableView.getSelectionModel().getSelectedItems()))
                            );
                            button.setOnAction(event -> showCompareDialog());
                        }),
                        DX.create(Button::new, button -> {
                            button.setTooltip(new Tooltip("Patch documents"));
                            button.setGraphic(DX.icon("icons/script.png"));
                            button.getStyleClass().addAll(BUTTON_ICON);
                            button.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());
                            button.setOnAction(event -> showPatchDialog());
                        }),
                        new Separator(),
                        DX.create(Button::new, button -> {
                            button.setTooltip(new Tooltip("Save table as json"));
                            button.setGraphic(DX.icon("icons/diskette.png"));
                            button.getStyleClass().addAll(BUTTON_ICON);
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
                            button.getStyleClass().addAll(BUTTON_ICON);
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
                            button.getStyleClass().addAll(BUTTON_ICON);
                            button.setOnAction(event -> createTableInfoDialog().show());
                        }),
                        DX.spacer(),
                        DX.create(Label::new, t -> {
                            t.textProperty().bind(concat("Count [", tableModel.rowsSizeProperty(), " of ~", tableModel.getTableDef().totalCountProperty(), "]"));
                        })
                )),
                DX.create(() -> this.tableView, tableView -> {
//                    tableView.getStyleClass().addAll(INTERACTIVE);
                    tableView.getColumns().add(DX.create((Supplier<TableColumn<EnhancedDocument, Number>>) TableColumn::new, column -> {
                        column.prefWidthProperty().bind(createIntegerBinding(() -> {
                            int charsNumber = String.valueOf(tableModel.rowsSizeProperty().get()).length();
                            return PADDING + charsNumber * FONT_SIZE;
                        }, tableModel.rowsSizeProperty()));
                        column.setResizable(true);
                        column.setSortable(false);
                        column.getStyleClass().add("column-index");
                        column.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(tableModel.getRows().indexOf(param.getValue()) + 1));
                    }));

                    VBox.setVgrow(tableView, Priority.ALWAYS);
                    tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                    tableView.setItems(tableModel.getRows());
                    tableView.setSkin(new MyTableViewSkin<>(tableView));
                    tableView.setRowFactory(param -> {
                        TableRow<EnhancedDocument> tableRow = new TableRow<>();
                        tableRow.setOnMouseClicked(event -> {
                            if (event.getClickCount() == 2 && tableRow.getItem() != null) {
                                showEditItemDialog(tableRow.getItem());
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
                                    showEditItemDialog(tableView.getSelectionModel().getSelectedItem());
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

        tableModel.getRows().addListener((ListChangeListener<EnhancedDocument>) c -> {
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
                .map(TableColumnBase::getId).toList();

        tableModel.getTableDef().getAttributeTypesMap().keySet().stream()
                .filter(attrName -> !availableAttributes.contains(attrName))
                .map(this::buildTableColumn)
                .forEach(tableView.getColumns()::add);

    }

    private TableColumn<EnhancedDocument, String> buildTableColumn(String attrName) {
        SimpleStringProperty filterProperty = tableModel.getAttributeFilterMap().computeIfAbsent(attrName, s -> new SimpleStringProperty());

        return DX.create(TableColumn::new, filter -> {
            filter.setId(attrName);
            filter.getStyleClass().add("table-column-filter");
            filter.setGraphic(DX.create(ClearableTextField::new, textField -> {
                textField.textProperty().bindBidirectional(filterProperty);
                textField.setOnAction(event -> reloadData());
                textField.setOnClear(event -> reloadData());
            }));
            filter.getColumns().add(DX.create((Supplier<TableColumn<EnhancedDocument, String>>) TableColumn::new, column -> {
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
//                    String value = param.getValue().getJson(attrName);
                    String value = toDisplayValue(param.getValue(), attrName);

                    return new SimpleStringProperty(value != null ? value : "");
                });
                column.setCellFactory(param -> {
                    TableCell<EnhancedDocument, String> cell = new TableCell<>();
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

    private void highlightCellValue(ObservableList<Highlighter.Criteria> criteriaList, TableCell<EnhancedDocument, String> cell) {
        criteriaList.stream()
                .filter(criteria -> criteria.match(cell.getText()))
                .findFirst()
                .ifPresentOrElse(criteria -> cell.setStyle(
                        String.format("-fx-background-color: %1s; -fx-text-fill: %2s", criteria.getBackgroundColor(), criteria.getTextColor())
                ), () -> cell.setStyle(null));
    }

    private void attachCellContextMenu(TableCell<EnhancedDocument, String> cell, String attrName) {
        cell.setOnContextMenuRequested(event -> {
            if (cell.getText() != null && !cell.getText().trim().isEmpty()) {
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
                                    reloadData();
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
                        DX.create(MenuItem::new, menuEdit -> {
                            menuEdit.setText("Edit document");
                            menuEdit.setGraphic(DX.icon("icons/page_edit.png"));
                            menuEdit.setOnAction(editEvent -> {
                                if (editEvent.getTarget().equals(editEvent.getSource())) {
                                    showEditItemDialog(cell.getTableRow().getItem());
                                }
                            });
                        }),
                        DX.create(MenuItem::new, menuEdit -> {
                            menuEdit.setText("Edit as new");
                            menuEdit.setGraphic(DX.icon("icons/page_add.png"));
                            menuEdit.setOnAction(editEvent -> {
                                if (editEvent.getTarget().equals(editEvent.getSource())) {
                                    showCreateItemDialog(cell.getTableRow().getItem());
                                }
                            });
                        })
                )).show(cell, event.getScreenX(), event.getScreenY());
            }
        });

    }

    private void clearFilter() {
        tableView.getSortOrder().clear();
        controller.onClearFilters();
    }

    private void reloadData() {
        tableView.getSortOrder().clear();
        controller.onRefreshData();
    }

    private void showEditItemDialog(EnhancedDocument document) {
        showItemDialog(String.format("[%1s] Edit the document", tableModel.getTableName()), document, controller::onUpdateItem, controller::validateItem);
    }

    private void showCreateItemDialog(EnhancedDocument document) {
        showItemDialog(String.format("[%1s] Create a new document", tableModel.getTableName()), document, controller::onCreateItem, controller::validateItem);
    }

    private void showPatchDialog() {
        if (!tableView.getSelectionModel().getSelectedItems().isEmpty()) {
            showItemDialog(String.format("[%1s] Patch selected documents", tableModel.getTableName()), DefaultEnhancedDocument.builder().build(),
                    (json, isRaw) -> controller.onPatchItems(tableView.getSelectionModel().getSelectedItems(), json, isRaw),
                    stringEventStream -> controller.validateItem(stringEventStream, true));
        }
    }

    private void showItemDialog(String title, EnhancedDocument document, BiConsumer<String, Boolean> onSaveConsumer, Function<EventStream<String>, EventStream<Boolean>> validator) {
        ItemDialog dialog = new ItemDialog(title, document, validator);
        themeManager.applyPseudoClasses(dialog.getDialogPane());
        dialog.showAndWait().ifPresent(result -> onSaveConsumer.accept(result, dialog.isEditAsRawJson()));
    }

    private void showCompareDialog() {
        if (tableView.getSelectionModel().getSelectedItems().size() >= 2) {
            EnhancedDocument item1 = tableView.getSelectionModel().getSelectedItems().get(0);
            EnhancedDocument item2 = tableView.getSelectionModel().getSelectedItems().get(1);

            CompareDialog dialog = new CompareDialog(uglyToPrettyJson(item1.toJson()), uglyToPrettyJson(item2.toJson()));
            dialog.showAndWait();
        }
    }

    private void deleteSelectedItems() {
        List<EnhancedDocument> items = tableView.getSelectionModel().getSelectedItems();
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

    private Dialog<?> createTableInfoDialog() {
        return new TableInfoDialog(tableModel, controller::openUrl);
    }

    private static String toDisplayValue(EnhancedDocument doc, String property) {
        if (doc.toMap().containsKey(property)) {
            AttributeValue attributeValue = doc.toMap().get(property);
            if (attributeValue.s() != null) {
                return attributeValue.s();
            } else {
                return doc.getJson(property);
            }
        }
        return "";
    }

}
