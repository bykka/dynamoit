package ua.org.java.dynamoit;

import io.reactivex.rxjavafx.observables.JavaFxObservable;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.control.textfield.TextFields;
import ua.org.java.dynamoit.components.activityindicator.ActivityIndicator;
import ua.org.java.dynamoit.components.tablegrid.TableGridComponent;
import ua.org.java.dynamoit.components.tablegrid.TableGridContext;
import ua.org.java.dynamoit.components.tablegrid.TableGridView;
import ua.org.java.dynamoit.utils.DX;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MainView extends VBox {

    private final MainModel model;
    private final MainController controller;

    private final TreeItem<String> allTables;
    private TabPane tabPane;
    private TreeView<String> treeView;

    public MainView(MainModel mainModel, MainController controller, ActivityIndicator activityIndicator) {
        this.model = mainModel;
        this.controller = controller;

        allTables = new AllTreeItem();

        this.getChildren().addAll(
                List.of(
                        DX.splitPane(splitPane -> {
                                    VBox.setVgrow(splitPane, Priority.ALWAYS);
                                    splitPane.setDividerPositions(0.35);
                                    return List.of(
                                            DX.create(VBox::new, vBox1 -> {
                                                SplitPane.setResizableWithParent(vBox1, false);
                                                return List.of(
                                                        DX.toolBar(toolBar -> List.of(
                                                                DX.create(TextFields::createClearableTextField, textField -> {
                                                                    HBox.setHgrow(textField, Priority.ALWAYS);
                                                                    textField.setPromptText("Table name contains");
                                                                    textField.textProperty().bindBidirectional(mainModel.filterProperty());
                                                                    textField.disableProperty().bind(mainModel.selectedProfileProperty().isEmpty());
                                                                }),
                                                                DX.create(Button::new, button -> {
                                                                    button.setTooltip(new Tooltip("Save current filter"));
                                                                    button.setGraphic(DX.icon("icons/star.png"));
                                                                    button.disableProperty().bind(mainModel.selectedProfileProperty().isEmpty());
                                                                    button.setOnAction(event -> controller.onSaveFilter());
                                                                }),
                                                                DX.create((Supplier<ComboBox<String>>) ComboBox::new, comboBox -> {
                                                                    comboBox.setItems(mainModel.getAvailableProfiles());
                                                                    comboBox.valueProperty().bindBidirectional(mainModel.selectedProfileProperty());
                                                                }),
                                                                DX.create(Button::new, button -> {
                                                                    button.setTooltip(new Tooltip("Reload list of tables"));
                                                                    button.setGraphic(DX.icon("icons/arrow_refresh.png"));
                                                                    button.disableProperty().bind(mainModel.selectedProfileProperty().isEmpty());
                                                                    button.setOnAction(__ -> controller.onTablesRefresh());
                                                                })
                                                        )),
                                                        DX.create((Supplier<TreeView<String>>) TreeView::new, treeView -> {
                                                            VBox.setVgrow(treeView, Priority.ALWAYS);
                                                            this.treeView = treeView;
                                                            treeView.setRoot(new TreeItem<>());
                                                            treeView.setShowRoot(false);
                                                            treeView.getRoot().getChildren().add(allTables);
                                                            treeView.setOnMouseClicked(event -> this.onTableSelect(event, treeView.getSelectionModel().getSelectedItem()));
                                                        })
                                                );
                                            }),
                                            DX.create(TabPane::new, tabPane -> {
                                                this.tabPane = tabPane;
                                            })
                                    );
                                }
                        ),
                        DX.create(HBox::new, hBox -> {
                            hBox.setPadding(new Insets(3, 3, 3, 3));
                            hBox.getChildren().addAll(
                                    List.of(
                                            DX.create(Pane::new, pane -> {
                                                HBox.setHgrow(pane, Priority.ALWAYS);
                                            }),
                                            activityIndicator
                                    )
                            );
                        })
                )
        );

        mainModel.getFilteredTables().addListener((ListChangeListener<MainModel.TableDef>) c -> {
            allTables.getChildren().setAll(mainModel.getFilteredTables().stream().map(MainModel.TableDef::getName).map(TableTreeItem::new).collect(Collectors.toList()));
            allTables.setExpanded(true);
        });

        JavaFxObservable.additionsOf(mainModel.getSavedFilters())
                .map(filter -> {
                    FilterTreeItem filterTables = new FilterTreeItem(filter);
                    filterTables.getChildren().addAll(mainModel.getAvailableTables()
                            .stream()
                            .filter(tableDef -> tableDef.getName().contains(filter))
                            .map(MainModel.TableDef::getName)
                            .map(TableTreeItem::new)
                            .collect(Collectors.toList()));
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
