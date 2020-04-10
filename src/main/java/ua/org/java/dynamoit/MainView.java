package ua.org.java.dynamoit;

import com.amazonaws.util.StringUtils;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ua.org.java.dynamoit.db.DynamoDBService;
import ua.org.java.dynamoit.table.TableView;
import ua.org.java.dynamoit.table.*;
import ua.org.java.dynamoit.utils.DX;

import javax.inject.Inject;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MainView extends VBox {

    private DynamoDBService dynamoDBService;

    private MainModel mainModel;

    private ObservableList<String> availableProfiles = FXCollections.observableArrayList();
    private ObjectProperty<TreeItem<String>> rootTreeItem = new SimpleObjectProperty<>();

    private TreeItem<String> allTables = new AllTreeItem();
    private TabPane tabPane;

    @Inject
    public MainView(DynamoDBService dynamoDBService, MainModel mainModel) {
        this.dynamoDBService = dynamoDBService;
        this.mainModel = mainModel;
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
                                                                DX.create(TextField::new, textField -> {
                                                                    HBox.setHgrow(textField, Priority.ALWAYS);
                                                                    textField.setPromptText("Table name contains");
                                                                    textField.textProperty().bindBidirectional(mainModel.filterProperty());
                                                                    textField.disableProperty().bind(mainModel.selectedProfileProperty().isEmpty());
                                                                }),
                                                                DX.create(Button::new, button -> {
                                                                    button.setTooltip(new Tooltip("Save current filter"));
                                                                    button.setGraphic(DX.icon("icons/star.png"));
                                                                    button.disableProperty().bind(mainModel.selectedProfileProperty().isEmpty());
                                                                    button.setOnAction(this::onFilterSave);
                                                                }),
                                                                DX.create((Supplier<ComboBox<String>>) ComboBox::new, comboBox -> {
                                                                    comboBox.setItems(availableProfiles);
                                                                    comboBox.valueProperty().bindBidirectional(mainModel.selectedProfileProperty());
                                                                }),
                                                                DX.create(Button::new, button -> {
                                                                    button.setTooltip(new Tooltip("Reload list of tables"));
                                                                    button.setGraphic(DX.icon("icons/arrow_refresh.png"));
                                                                    button.disableProperty().bind(mainModel.selectedProfileProperty().isEmpty());
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
                        )
                )
        );

        this.dynamoDBService.getAvailableProfiles().thenAccept(profiles -> Platform.runLater(() -> this.availableProfiles.addAll(profiles)));

        this.mainModel.selectedProfileProperty().addListener((observable, oldValue, newValue) -> {
            this.mainModel.getAvailableTables().clear();
            if (!StringUtils.isNullOrEmpty(newValue)) {
                this.dynamoDBService.getListOfTables(newValue).thenAccept(tables -> Platform.runLater(() -> {
                    this.mainModel.getAvailableTables().addAll(tables);
                    this.rootTreeItem.set(DX.create(TreeItem::new, root -> {
                        root.getChildren().add(allTables);
                    }));
                }));
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

    private void onFilterSave(ActionEvent actionEvent) {
        this.mainModel.getSavedFilters().add(mainModel.getFilter());
    }

    private void onTableSelect(MouseEvent event, TreeItem<String> selectedItem) {
        if (event.getClickCount() == 2 && selectedItem != null) {
            if (selectedItem instanceof AllTreeItem || selectedItem instanceof FilterTreeItem) {
                return;
            }

            createAndOpenTab(new TableContext(mainModel.getSelectedProfile(), selectedItem.getValue()));
        }
    }

    private void createAndOpenTab(TableContext tableContext){
        TableComponent tableComponent = DaggerTableComponent.builder()
                .tableModule(new TableModule(tableContext, mainModel))
                .build();

        TableView tableItemsView = tableComponent.view();
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
