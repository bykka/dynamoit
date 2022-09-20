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

package ua.org.java.dynamoit.components.profileviewer;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import ua.org.java.dynamoit.utils.DX;

import java.util.function.Consumer;

public class NewProfileDialog extends Dialog<Void> {

    private static final int LABEL_SIZE = 90;

    public NewProfileDialog() {
        setTitle("Create a new Profile");
        getDialogPane().setMinWidth(500);

        getDialogPane().setContent(DX.create(TabPane::new, tabPane -> {
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

            Consumer<GridPane> defaultSettings = gridPane -> {
                gridPane.setHgap(10);
                gridPane.setVgap(10);
                gridPane.getColumnConstraints().addAll(
                        new ColumnConstraints(LABEL_SIZE),
                        DX.create(ColumnConstraints::new, c -> {
                            c.setHgrow(Priority.ALWAYS);
                        })
                );
                gridPane.setPadding(new Insets(14, 0, 0, 14));
                gridPane.addRow(0, DX.boldLabel("Profile:"), new TextField());
            };

            tabPane.getTabs().add(DX.create(Tab::new, tab -> {
                tab.setText("Remote");
                tab.setContent(DX.create(GridPane::new, gridPane -> {
                    defaultSettings.accept(gridPane);

                    gridPane.addRow(1, DX.boldLabel("Access key:"), new TextField());
                    gridPane.addRow(2, DX.boldLabel("Security key:"), new TextField());
                    gridPane.addRow(3, DX.boldLabel("Region:"), new ChoiceBox<String>());
                }));
            }));
            tabPane.getTabs().add(DX.create(Tab::new, tab -> {
                tab.setText("Local");
                tab.setContent(DX.create(GridPane::new, gridPane -> {
                    defaultSettings.accept(gridPane);

                    gridPane.addRow(1, DX.boldLabel("Endpoint url:"), new TextField());
                }));
            }));
        }));

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    }
}
