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

import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import ua.org.java.dynamoit.utils.DX;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilterTextField extends ClearableTextField {

    private final Label label = new Label(Operation.EQUALS.label);

    public FilterTextField() {
        ToggleGroup toggleGroup = new ToggleGroup();
        ContextMenu contextMenu = DX.contextMenu(__ ->
                Stream.of(Operation.values())
                        .map(operation -> DX.create(RadioMenuItem::new, menu -> {
                            menu.setText(operation.getText());
                            menu.setGraphic(new Label(operation.getLabel()));
                            menu.setOnAction(___ -> label.setText(operation.getLabel()));
                            menu.setToggleGroup(toggleGroup);
                            menu.setUserData(operation);
                            menu.setSelected(operation == Operation.EQUALS);
                        }))
                        .collect(Collectors.toList())
        );

        StackPane pane = new StackPane(label);
        pane.setCursor(Cursor.DEFAULT);
        pane.setOnMouseClicked(event -> {
            Bounds localBounds = this.getBoundsInLocal();
            Bounds screenBounds = this.localToScreen(localBounds);
            contextMenu.show(pane, screenBounds.getMinX(), screenBounds.getMinY() + getHeight());
        });

        setLeft(pane);
    }

    private enum Operation {
        EQUALS("==", "equals"),
        NOT_EQUALS("!=", "not equals"),
        LESS_THAN("<", "less than"),
        LESS_THAN_OR_EQUALS("<=", "less than or equals"),
        MORE_THAN(">", "more than"),
        MORE_THAN_OR_EQUALS("=>", "more than or equals"),
        BETWEEN("..", "between"),
        EXISTS("$", "exists"),
        NOT_EXISTS("!$", "not exists"),
        CONTAINS("~", "contains"),
        NOT_CONTAINS("!~", "not contains"),
        BEGINS_WITH("^", "begins with");

        private final String label;
        private final String text;

        Operation(String label, String text) {
            this.label = label;
            this.text = text;
        }

        public String getLabel() {
            return label;
        }

        public String getText() {
            return text;
        }
    }
}
