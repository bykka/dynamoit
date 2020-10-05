package ua.org.java.dynamoit.components.profileviewer;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.control.textfield.TextFields;
import ua.org.java.dynamoit.MainController;
import ua.org.java.dynamoit.MainModel;
import ua.org.java.dynamoit.utils.DX;

import java.util.List;
import java.util.function.Supplier;

public class ProfileView extends VBox {

    private TreeView<String> treeView;

    public ProfileView(MainModel mainModel, MainController controller) {
        this.getChildren().addAll(
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
//                    treeView.getRoot().getChildren().add(allTables);
//                    treeView.setOnMouseClicked(event -> this.onTableSelect(event, treeView.getSelectionModel().getSelectedItem()));
                })
        );
    }
}
