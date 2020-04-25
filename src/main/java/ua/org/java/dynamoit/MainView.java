package ua.org.java.dynamoit;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import ua.org.java.dynamoit.components.tablegrid.DaggerTableGridComponent;
import ua.org.java.dynamoit.components.tablegrid.TableGridComponent;
import ua.org.java.dynamoit.components.tablegrid.TableGridContext;
import ua.org.java.dynamoit.components.tablegrid.TableGridView;
import ua.org.java.dynamoit.utils.DX;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MainView extends VBox {

    private final MainModel model;

    private final ObjectProperty<TreeItem<String>> rootTreeItem = new SimpleObjectProperty<>();
    private final TreeItem<String> allTables = new AllTreeItem();
    private TabPane tabPane;

    public MainView(MainModel mainModel, MainController controller, ActivityIndicator activityIndicator) {
        this.model = mainModel;
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
                                                            treeView.rootProperty().bind(this.rootTreeItem);
                                                            treeView.setShowRoot(false);
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
                            hBox.setPadding(new Insets(3,3,3,3));
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

        this.model.getAvailableTables().addListener((ListChangeListener<String>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    this.rootTreeItem.set(DX.create(TreeItem::new, root -> {
                        root.getChildren().add(allTables);
                    }));
                }
            }
        });

        mainModel.getFilteredTables().addListener((ListChangeListener<String>) c -> {
            allTables.getChildren().setAll(mainModel.getFilteredTables().stream().map(TableTreeItem::new).collect(Collectors.toList()));
            allTables.setExpanded(true);
        });

        mainModel.getSavedFilters().addListener((ListChangeListener<String>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    this.rootTreeItem.get().getChildren().addAll(
                            c.getAddedSubList()
                                    .stream()
                                    .map(filter -> {
                                        FilterTreeItem filterTables = new FilterTreeItem(filter);
                                        filterTables.getChildren().addAll(mainModel.getAvailableTables().stream().filter(tableName -> tableName.contains(filter)).map(TableTreeItem::new).collect(Collectors.toList()));
                                        filterTables.setExpanded(true);
                                        return filterTables;
                                    }).collect(Collectors.toList())
                    );
                }
            }
        });
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
        TableGridComponent tableComponent = DaggerTableGridComponent.builder()
                .mainModel(model)
                .tableContext(tableContext)
                .build();

        TableGridView tableItemsView = tableComponent.view();
        tableItemsView.setOnSearchInTable(this::createAndOpenTab);

        Tab tab = new Tab(tableContext.getTableName(), tableItemsView);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    private static class AllTreeItem extends TreeItem<String> {

        public AllTreeItem() {
            super("All tables", DX.icon("icons/database.png"));
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
