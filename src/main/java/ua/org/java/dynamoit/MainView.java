package ua.org.java.dynamoit;

import com.amazonaws.util.StringUtils;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ua.org.java.dynamoit.table.DaggerTableComponent;
import ua.org.java.dynamoit.table.TableComponent;
import ua.org.java.dynamoit.table.TableContext;
import ua.org.java.dynamoit.table.TableModule;
import ua.org.java.dynamoit.utils.DX;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MainView extends VBox {

    private MainController mainController;

    private ObservableList<String> availableProfiles = FXCollections.observableArrayList();
    private List<String> availableTables = new ArrayList<>();
    private SimpleStringProperty selectedProfile = new SimpleStringProperty();
    private SimpleStringProperty filter = new SimpleStringProperty("");
    private ObjectProperty<TreeItem<String>> rootTreeItem = new SimpleObjectProperty<>();

    private TabPane tabPane;

    @Inject
    public MainView(MainController mainController) {
        this.mainController = mainController;
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
                                                                    textField.textProperty().bindBidirectional(filter);
                                                                    textField.disableProperty().bind(selectedProfile.isEmpty());
                                                                }),
                                                                DX.create(Button::new, button -> {
                                                                    button.setTooltip(new Tooltip("Save current filter"));
                                                                    button.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.SAVE));
                                                                    button.disableProperty().bind(selectedProfile.isEmpty());
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

        this.mainController.getAvailableProfiles().thenAccept(profiles -> Platform.runLater(() -> this.availableProfiles.addAll(profiles)));

        this.selectedProfile.addListener((observable, oldValue, newValue) -> {
            this.availableTables.clear();
            if (!StringUtils.isNullOrEmpty(newValue)) {
                this.mainController.getListOfTables(newValue).thenAccept(tables -> Platform.runLater(() -> {
                    this.availableTables.addAll(tables);
                    buildTablesTree();
                }));
            }
        });

        this.filter.addListener((observable, oldValue, newValue) -> {
            buildTablesTree();
        });

    }

    private void buildTablesTree() {
        TreeItem<String> rootItem = new TreeItem<>("All tables");
        rootItem.getChildren().addAll(availableTables.stream().filter(tableName -> tableName.contains(filter.get())).map(TreeItem::new).collect(Collectors.toList()));
        this.rootTreeItem.set(rootItem);
    }

    private void onTableSelect(MouseEvent event, TreeItem<String> selectedItem) {
        if (event.getClickCount() == 2) {

            TableComponent tableComponent = DaggerTableComponent.builder()
                    .tableModule(new TableModule(new TableContext(selectedProfile.get(), selectedItem.getValue())))
                    .build();

            Tab tab = new Tab(selectedItem.getValue(), tableComponent.view());
            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tab);
        }
    }

}
