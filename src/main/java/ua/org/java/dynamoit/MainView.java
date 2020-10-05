/*
 * This file is part of DynamoIt.
 *
 *     DynamoIt is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Foobar is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with DynamoIt.  If not, see <https://www.gnu.org/licenses/>.
 */

package ua.org.java.dynamoit;

import io.reactivex.rxjavafx.observables.JavaFxObservable;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import ua.org.java.dynamoit.components.activityindicator.ActivityIndicator;
import ua.org.java.dynamoit.components.profileviewer.ProfileView;
import ua.org.java.dynamoit.components.tablegrid.TableGridComponent;
import ua.org.java.dynamoit.components.tablegrid.TableGridContext;
import ua.org.java.dynamoit.components.tablegrid.TableGridView;
import ua.org.java.dynamoit.model.TableDef;
import ua.org.java.dynamoit.utils.DX;

import java.util.List;
import java.util.stream.Collectors;

public class MainView extends VBox {

    private final MainModel model;
    private final MainController controller;

    private final TreeItem<String> allTables;
    private TabPane tabPane;
    private final ToggleGroup profileToggleGroup = new ToggleGroup();

    public MainView(MainModel mainModel, MainController controller, ActivityIndicator activityIndicator) {
        this.model = mainModel;
        this.controller = controller;

        allTables = new AllTreeItem();

        this.getChildren().addAll(

                DX.create(HBox::new, (HBox hBox1) -> {
                    VBox.setVgrow(hBox1, Priority.ALWAYS);
                    hBox1.getChildren().addAll(
                            DX.toolBar(toolBar -> {
                                toolBar.setOrientation(Orientation.VERTICAL);
                                return List.of(
                                        new Group(DX.create(ToggleButton::new, (ToggleButton button) -> {
                                            button.setText("default");
                                            button.setRotate(-90);
                                            button.setToggleGroup(profileToggleGroup);
                                        })),
                                        new Group(DX.create(ToggleButton::new, (ToggleButton button) -> {
                                            button.setText("prod");
                                            button.setRotate(-90);
                                            button.setToggleGroup(profileToggleGroup);
                                        }))
                                );
                            }),

                            DX.splitPane(splitPane -> {
                                        HBox.setHgrow(splitPane, Priority.ALWAYS);
                                        splitPane.setDividerPositions(0.35);
                                        return List.of(
                                                DX.create(StackPane::new, stackPane -> {
                                                    SplitPane.setResizableWithParent(stackPane, false);
                                                    stackPane.visibleProperty().bind(Bindings.isNotNull(profileToggleGroup.selectedToggleProperty()));
                                                    stackPane.managedProperty().bind(Bindings.isNotNull(profileToggleGroup.selectedToggleProperty()));
                                                    return List.of(
                                                            new Button("1"),
                                                            new Button("2"),
                                                            new ProfileView(model, controller)
                                                    );
                                                }),
                                                DX.create(TabPane::new, tabPane -> {
                                                    this.tabPane = tabPane;
                                                })
                                        );
                                    }
                            )
                    );
                }),


                DX.create(HBox::new, hBox -> {
                    hBox.setPadding(new Insets(3, 3, 3, 3));
                    hBox.getChildren().addAll(
                            DX.create(Pane::new, pane -> {
                                HBox.setHgrow(pane, Priority.ALWAYS);
                            }),
                            activityIndicator
                    );
                })
        );

        mainModel.getFilteredTables().addListener((ListChangeListener<TableDef>) c -> {
            allTables.getChildren().setAll(mainModel.getFilteredTables().stream().map(TableDef::getName).map(TableTreeItem::new).collect(Collectors.toList()));
            allTables.setExpanded(true);
        });

        JavaFxObservable.additionsOf(mainModel.getSavedFilters())
                .map(filter -> {
                    FilterTreeItem filterTables = new FilterTreeItem(filter);
                    filterTables.getChildren().addAll(mainModel.getAvailableTables()
                            .stream()
                            .filter(tableDef -> tableDef.getName().contains(filter))
                            .map(TableDef::getName)
                            .map(TableTreeItem::new)
                            .collect(Collectors.toList()));
                    filterTables.setExpanded(true);
                    return filterTables;
                });//fixme
//                .subscribe(filterTreeItem -> this.treeView.getRoot().getChildren().add(filterTreeItem));
    }

    private void onTableSelect(MouseEvent event, TreeItem<String> selectedItem) {
        if (event.getClickCount() == 2 && selectedItem != null) {
            if (selectedItem instanceof AllTreeItem || selectedItem instanceof FilterTreeItem) {
                return;
            }

            createAndOpenTab(new TableGridContext(model.getSelectedProfile(), selectedItem.getValue()));
        }
    }

    private void createAndOpenTab(TableGridContext tableContext) {
        TableGridComponent tableComponent = controller.buildTableGridComponent(tableContext);

        TableGridView tableItemsView = tableComponent.view();
        tableItemsView.setOnSearchInTable(this::createAndOpenTab);

        Tab tab = new Tab(tableContext.getTableName(), tableItemsView);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    private class AllTreeItem extends TreeItem<String> {

        public AllTreeItem() {
            super("All tables", DX.icon("icons/database.png"));
            valueProperty().bind(Bindings.concat("All tables (", Bindings.size(model.getFilteredTables()), ")"));
        }

    }

    private static class FilterTreeItem extends TreeItem<String> {

        public FilterTreeItem(String filter) {
            super("Contains: " + filter, DX.icon("icons/folder_star.png"));
        }
    }

    private static class TableTreeItem extends TreeItem<String> {

        public TableTreeItem(String text) {
            super(text, DX.icon("icons/table.png"));
        }

    }

}
