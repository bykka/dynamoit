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

package ua.org.java.dynamoit.components.profileviewer;

import io.reactivex.rxjavafx.observables.JavaFxObservable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ua.org.java.dynamoit.components.main.MainModel;
import ua.org.java.dynamoit.model.TableDef;
import ua.org.java.dynamoit.utils.DX;
import ua.org.java.dynamoit.widgets.ClearableTextField;

import javax.inject.Inject;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static atlantafx.base.theme.Styles.BUTTON_ICON;

public class ProfileView extends VBox {

    private static final List<String> REGIONS = Stream.of(
            "us-east-1",
            "us-east-2",
            "us-west-1",
            "us-west-2",
            "af-south-1",
            "ap-east-1",
            "ap-south-1",
            "ap-northeast-3",
            "ap-northeast-2",
            "ap-southeast-1",
            "ap-southeast-2",
            "ap-northeast-1",
            "ca-central-1",
            "eu-central-1",
            "eu-west-1",
            "eu-west-2",
            "eu-south-1",
            "eu-west-3",
            "eu-north-1",
            "me-south-1",
            "sa-east-1",
            "us-gov-east-1",
            "us-gov-west-1"
    ).sorted().toList();

    private final TreeView<String> treeView = new TreeView<>();
    private final TreeItem<String> allTables;
    private final MainModel.ProfileModel model;
    private final ProfileController controller;

    @Inject
    public ProfileView(ProfileController controller, MainModel.ProfileModel model) {
        this.model = model;
        this.controller = controller;
        allTables = new AllTreeItem(model.regionProperty());

        this.getChildren().addAll(
                DX.toolBar(toolBar -> List.of(
                        DX.create(ClearableTextField::new, textField -> {
                            HBox.setHgrow(textField, Priority.ALWAYS);
                            textField.setPromptText("Table name contains");
                            textField.textProperty().bindBidirectional(model.filterProperty());
                            textField.setOnAction(event -> controller.onSaveFilter());
                        }),
                        DX.create(Button::new, button -> {
                            button.setTooltip(new Tooltip("Save current filter"));
                            button.setGraphic(DX.icon("icons/star.png"));
                            button.getStyleClass().addAll(BUTTON_ICON);
                            button.setOnAction(event -> controller.onSaveFilter());
                        }),
                        DX.create(Button::new, button -> {
                            button.setTooltip(new Tooltip("Reload list of tables"));
                            button.setGraphic(DX.icon("icons/arrow_refresh.png"));
                            button.getStyleClass().addAll(BUTTON_ICON);
                            button.setOnAction(__ -> controller.onTablesRefresh());
                        })
                )),
                DX.create(() -> this.treeView, treeView -> {
                    VBox.setVgrow(treeView, Priority.ALWAYS);
                    treeView.setRoot(new TreeItem<>());
                    treeView.setShowRoot(false);
                    treeView.getRoot().getChildren().add(allTables);
                    treeView.setOnMouseClicked(event -> this.onTableSelect(event, treeView.getSelectionModel().getSelectedItem()));
                    treeView.setOnKeyPressed(event -> this.onTableSelect(event, treeView.getSelectionModel().getSelectedItem()));
                    treeView.setOnContextMenuRequested(event -> {
                        if (event.getTarget() != null) {
                            TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();
                            ContextMenu contextMenu = null;

                            if (selectedItem instanceof AllTreeItem allTreeItem) {
                                contextMenu = DX.contextMenu(cm -> REGIONS.stream()
                                        .map(region -> DX.create(MenuItem::new, menu -> {
                                            menu.setText(region);
                                            menu.setOnAction(__ -> controller.onChangeRegion(region));
                                        })).collect(Collectors.toList()));
                            } else if (selectedItem instanceof FilterTreeItem filterTreeItem) {
                                contextMenu = DX.contextMenu(cm -> List.of(
                                        DX.create((Supplier<MenuItem>) MenuItem::new, menu -> {
                                            menu.setText("Delete");
                                            menu.setOnAction(__ -> {
                                                controller.onDeleteFilter(filterTreeItem.getFilter());
                                                treeView.getRoot().getChildren().remove(filterTreeItem);
                                            });
                                        })
                                ));
                            }

                            if (contextMenu != null) {
                                contextMenu.show(selectedItem.getGraphic(), event.getScreenX(), event.getScreenY());
                            }
                        }
                    });
                })
        );

        model.getFilteredTables().addListener((ListChangeListener<TableDef>) c -> {
            allTables.getChildren().setAll(model.getFilteredTables().stream().map(TableDef::getName).map(TableTreeItem::new).collect(Collectors.toList()));
            allTables.setExpanded(true);
        });

        JavaFxObservable.additionsOf(model.getSavedFilters())
                .map(filter -> {
                    FilterTreeItem filterTables = new FilterTreeItem(filter);

                    ObjectBinding<List<TableTreeItem>> tableItems = Bindings.createObjectBinding(() -> model.getAvailableTables().stream()
                            .map(TableDef::getName)
                            .filter(name -> name.contains(filter))
                            .map(TableTreeItem::new)
                            .collect(Collectors.toList()), model.getAvailableTables());

                    filterTables.getChildren().addAll(tableItems.get());

                    tableItems.addListener(observable -> {
                        filterTables.getChildren().clear();
                        filterTables.getChildren().addAll(tableItems.get());
                    });

                    filterTables.setExpanded(true);
                    return filterTables;
                })
                .subscribe(filterTreeItem -> this.treeView.getRoot().getChildren().add(filterTreeItem));
    }

    private void onTableSelect(MouseEvent event, TreeItem<String> selectedItem) {
        if (event.getClickCount() == 2 && selectedItem != null) {
            if (selectedItem instanceof AllTreeItem || selectedItem instanceof FilterTreeItem) {
                return;
            }

            controller.onTableSelect(selectedItem.getValue());
        }
    }

    private void onTableSelect(KeyEvent event, TreeItem<String> selectedItem) {
        if (event.getCode() == KeyCode.ENTER && selectedItem != null) {
            if (selectedItem instanceof AllTreeItem || selectedItem instanceof FilterTreeItem) {
                return;
            }

            controller.onTableSelect(selectedItem.getValue());
        }
    }

    private class AllTreeItem extends TreeItem<String> {

        public AllTreeItem(SimpleStringProperty region) {
            super("All tables: " + region, DX.icon("icons/database.png"));
            valueProperty().bind(Bindings.concat("All tables: ", region, " (", Bindings.size(model.getFilteredTables()), ")"));
        }

    }

    private static class FilterTreeItem extends TreeItem<String> {

        private final String filter;

        public FilterTreeItem(String filter) {
            this.filter = filter;
            valueProperty().bind(Bindings.concat("Contains: ", filter, " (", Bindings.size(getChildren()), ")"));
            setGraphic(DX.icon("icons/folder_star.png"));
        }

        public String getFilter() {
            return filter;
        }
    }

    private static class TableTreeItem extends TreeItem<String> {

        public TableTreeItem(String text) {
            super(text, DX.icon("icons/table.png"));
        }

    }
}
