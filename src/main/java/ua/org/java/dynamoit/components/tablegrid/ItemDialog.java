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

import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.reactfx.EventStream;
import org.reactfx.Subscription;
import ua.org.java.dynamoit.components.jsoneditor.JsonEditor;

import java.util.function.Consumer;
import java.util.function.Function;

public class ItemDialog extends Dialog<String> {

    private Subscription subscribe = null;

    public ItemDialog(String title, String json, Function<EventStream<String>, EventStream<Boolean>> validator) {
        JsonEditor textArea = new JsonEditor();
        textArea.setPrefWidth(800);
        textArea.setPrefHeight(800);

        this.setTitle(title);
        ((Stage) this.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/page.png"));
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        this.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CLOSE);
        this.getDialogPane().setContent(new VirtualizedScrollPane<>(textArea));
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
            subscribe = validator.apply(textArea.multiPlainChanges()
                    .map(__ -> textArea.getText()))
                    .subscribe(valid -> saveButtonDisable.accept(!valid));

            textArea.replaceText(json);
        });

        this.onCloseRequestProperty().set(event -> {
            if (subscribe != null) {
                subscribe.unsubscribe();
            }
        });
    }


}
