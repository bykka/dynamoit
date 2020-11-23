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
import javafx.collections.ListChangeListener;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ua.org.java.dynamoit.components.main.MainModel;
import ua.org.java.dynamoit.model.TableDef;
import ua.org.java.dynamoit.utils.DX;
import ua.org.java.dynamoit.widgets.ClearableTextField;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ProfileView extends VBox {

    private final TreeView<String> treeView = new TreeView<>();

    private final TreeItem<String> allTables;
    private final MainModel.ProfileModel model;
    private final ProfileController controller;

    public ProfileView(ProfileController controller, MainModel.ProfileModel model) {
        this.model = model;
        this.controller = controller;
        allTables = new AllTreeItem();

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
                            button.setOnAction(event -> controller.onSaveFilter());
                        }),
                        DX.create(Button::new, button -> {
                            button.setTooltip(new Tooltip("Reload list of tables"));
                            button.setGraphic(DX.icon("icons/arrow_refresh.png"));
                            button.setOnAction(__ -> controller.onTablesRefresh());
                        })
                )),
                DX.create(() -> this.treeView, treeView -> {
                    VBox.setVgrow(treeView, Priority.ALWAYS);
                    treeView.setRoot(new TreeItem<>());
                    treeView.setShowRoot(false);
                    treeView.getRoot().getChildren().add(allTables);
                    treeView.setOnMouseClicked(event -> this.onTableSelect(event, treeView.getSelectionModel().getSelectedItem()));
                    treeView.setOnContextMenuRequested(event -> {
                        if (event.getTarget() != null && treeView.getSelectionModel().getSelectedItem() instanceof FilterTreeItem) {
                            FilterTreeItem filterTreeItem = (FilterTreeItem) treeView.getSelectionModel().getSelectedItem();
                            DX.contextMenu(contextMenu -> List.of(
                                    DX.create((Supplier<MenuItem>) MenuItem::new, menu -> {
                                        menu.setText("Delete");
                                        menu.setOnAction(__ -> {
                                            controller.onDeleteFilter(filterTreeItem.getFilter());
                                            treeView.getRoot().getChildren().remove(filterTreeItem);
                                        });
                                    })
                            )).show(treeView.getSelectionModel().getSelectedItem().getGraphic(), event.getScreenX(), event.getScreenY());
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
                            .filter(tableDef -> tableDef.getName().contains(filter))
                            .map(TableDef::getName)
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

    private class AllTreeItem extends TreeItem<String> {

        public AllTreeItem() {
            super("All tables", DX.icon("icons/database.png"));
            valueProperty().bind(Bindings.concat("All tables (", Bindings.size(model.getFilteredTables()), ")"));
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
