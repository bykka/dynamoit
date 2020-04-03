package ua.org.java.dynamoit;

import com.amazonaws.util.StringUtils;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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
import ua.org.java.dynamoit.table.DaggerTableComponent;
import ua.org.java.dynamoit.table.TableComponent;
import ua.org.java.dynamoit.table.TableContext;
import ua.org.java.dynamoit.table.TableModule;
import ua.org.java.dynamoit.utils.DX;

import javax.inject.Inject;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MainView extends VBox {

    private DynamoDBService dynamoDBService;

    private MainModel mainModel;

    private ObservableList<String> availableProfiles = FXCollections.observableArrayList();
    private SimpleStringProperty selectedProfile = new SimpleStringProperty();
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
                                                                    textField.disableProperty().bind(selectedProfile.isEmpty());
                                                                }),
                                                                DX.create(Button::new, button -> {
                                                                    button.setTooltip(new Tooltip("Save current filter"));
                                                                    button.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.SAVE));
                                                                    button.disableProperty().bind(selectedProfile.isEmpty());
                                                                    button.setOnAction(this::onFilterSave);
                                                                }),
                                                                DX.create((Supplier<ComboBox<String>>) ComboBox::new, comboBox -> {
                                                                    comboBox.setItems(availableProfiles);
                                                                    comboBox.valueProperty().bindBidirectional(selectedProfile);
                                                                }),
                                                                DX.create(Button::new, button -> {
                                                                    button.setTooltip(new Tooltip("Reload list of tables"));
                                                                    button.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.REFRESH));
                                                                    button.disableProperty().bind(selectedProfile.isEmpty());
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

        this.selectedProfile.addListener((observable, oldValue, newValue) -> {
            this.mainModel.getAvailableTables().clear();
            if (!StringUtils.isNullOrEmpty(newValue)) {
                this.dynamoDBService.getListOfTables(newValue).thenAccept(tables -> Platform.runLater(() -> {
                    this.mainModel.getAvailableTables().addAll(tables);
                    buildTablesTree();
                }));
            }
        });

        mainModel.getFilteredTables().addListener((ListChangeListener<String>) c -> {
            allTables.getChildren().setAll(mainModel.getFilteredTables().stream().map(TreeItem::new).collect(Collectors.toList()));
        });

        mainModel.getSavedFilters().addListener((ListChangeListener<String>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    this.rootTreeItem.get().getChildren().addAll(
                            c.getAddedSubList()
                                    .stream()
                                    .map(filter -> {
                                        FilterTreeItem filterTables = new FilterTreeItem(filter);
                                        filterTables.getChildren().addAll(mainModel.getAvailableTables().stream().filter(tableName -> tableName.contains(filter)).map(TreeItem::new).collect(Collectors.toList()));
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

    private void buildTablesTree() {
        allTables.getChildren().addAll(mainModel.getAvailableTables().stream().map(TreeItem::new).collect(Collectors.toList()));
        allTables.setExpanded(true);

        this.rootTreeItem.set(DX.create(TreeItem::new, root -> {
            root.getChildren().addAll(allTables);
        }));
    }

    private void onTableSelect(MouseEvent event, TreeItem<String> selectedItem) {
        if (event.getClickCount() == 2 && selectedItem != null) {
            if (selectedItem instanceof AllTreeItem || selectedItem instanceof FilterTreeItem) {
                return;
            }

            TableComponent tableComponent = DaggerTableComponent.builder()
                    .tableModule(new TableModule(new TableContext(selectedProfile.get(), selectedItem.getValue()), mainModel))
                    .build();

            Tab tab = new Tab(selectedItem.getValue(), tableComponent.view());
            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tab);
        }
    }

    private static class AllTreeItem extends TreeItem<String> {

        public AllTreeItem() {
            super("All tables");
        }

    }

    private static class FilterTreeItem extends TreeItem<String> {

        public FilterTreeItem(String filter) {
            super("Contains: " + filter);
        }
    }

}
