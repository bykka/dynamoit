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

package ua.org.java.dynamoit.components.tablegrid;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.controlsfx.control.textfield.TextFields;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.reactfx.EventStream;
import org.reactfx.Subscription;
import ua.org.java.dynamoit.components.jsoneditor.JsonEditor;
import ua.org.java.dynamoit.utils.DX;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class ItemDialog extends Dialog<String> {

    private final VBox mainBox;
    private final ToolBar toolBar;
    private Subscription validationSubscribe;
    private final JsonEditor textArea;
    private TextField searchField;
    private Disposable focusDisposable;

    public ItemDialog(String title, String json, Function<EventStream<String>, EventStream<Boolean>> validator) {
        this.setTitle(title);

        this.toolBar = DX.toolBar(toolBar -> List.of(
                DX.create(TextFields::createClearableTextField, textField -> {
                    this.searchField = textField;
                    HBox.setHgrow(textField, Priority.ALWAYS);
                    textField.requestFocus();
                }),
                DX.create(Button::new, button -> {
                    button.setTooltip(new Tooltip("Hide toolbar"));
                    button.setGraphic(DX.icon("icons/cross.png"));
                    button.setOnAction(event -> hideToolbar());
                })
        ));

        textArea = new JsonEditor();
        textArea.setPrefWidth(800);
        textArea.setPrefHeight(800);
        textArea.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode().equals(KeyCode.F)) {
                this.showToolbar();
            }
        });

        this.mainBox = DX.create(VBox::new, vBox -> {
            vBox.setPadding(new Insets(0, 0, 0, 0));
            vBox.getChildren().addAll(
                    new VirtualizedScrollPane<>(textArea)
            );
        });

        ((Stage) this.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/page.png"));
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        this.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CLOSE);

        this.getDialogPane().setContent(this.mainBox);
        this.initModality(Modality.NONE);
        this.setResizable(true);
        this.setResultConverter(param -> {
            if (param == saveButton) {
                return textArea.getText();
            }
            return null;
        });

        Consumer<Boolean> saveButtonDisable = disable -> {
            Node button = this.getDialogPane().lookupButton(saveButton);
            if (button != null) {
                button.setDisable(disable);
            }
        };

        saveButtonDisable.accept(true);

        this.onShowingProperty().set(event -> {
            validationSubscribe = validator.apply(textArea.multiPlainChanges()
                    .map(__ -> textArea.getText()))
                    .subscribe(valid -> saveButtonDisable.accept(!valid));

            textArea.replaceText(json);
            textArea.requestFocus();
        });

        this.onCloseRequestProperty().set(event -> {
            if (validationSubscribe != null) {
                validationSubscribe.unsubscribe();
            }
            if (focusDisposable != null) {
                focusDisposable.dispose();
            }
        });
    }


    private void showToolbar() {
        if (!this.mainBox.getChildren().contains(this.toolBar)) {
            this.mainBox.getChildren().add(0, this.toolBar);

            focusDisposable = Observable.timer(10, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(JavaFxScheduler.platform())
                    .subscribe(aLong -> this.searchField.requestFocus());
        }
    }

    private void hideToolbar() {
        this.mainBox.getChildren().remove(this.toolBar);
        this.textArea.requestFocus();
        this.focusDisposable.dispose();
    }
}
