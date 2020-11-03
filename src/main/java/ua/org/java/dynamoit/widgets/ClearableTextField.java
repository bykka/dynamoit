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

package ua.org.java.dynamoit.widgets;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.controlsfx.control.textfield.CustomTextField;

public class ClearableTextField extends CustomTextField {

    public ClearableTextField() {
        setRight(buildClearNode());
    }

    protected Node buildClearNode() {
        BooleanBinding visibleIconProperty = Bindings.and(this.editableProperty(), Bindings.isNotEmpty(this.textProperty()));

        double size = 14;
        Image grey = new Image("icons/cross_grey.png", size, size, true, false);
        Image red = new Image("icons/cross.png", size, size, true, false);

        ImageView icon = new ImageView(grey);
        StackPane pane = new StackPane(icon);
        pane.setCursor(Cursor.DEFAULT);
        pane.managedProperty().bind(visibleIconProperty);
        pane.visibleProperty().bind(visibleIconProperty);
        pane.setOnMouseClicked(event -> {
            this.clear();
            this.fireEvent(new ClearEvent(ClearEvent.CLEAR));
        });
        pane.setOnMouseEntered(event -> {
            icon.setImage(red);
        });
        pane.setOnMouseExited(event -> {
            icon.setImage(grey);
        });


        return pane;
    }

    public static class ClearEvent extends Event {

        public static EventType<ClearEvent> CLEAR = new EventType<>(Event.ANY, "CLEAR");

        public ClearEvent(EventType<? extends Event> eventType) {
            super(eventType);
        }
    }
}
