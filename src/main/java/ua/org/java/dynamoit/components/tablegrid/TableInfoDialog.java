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

package ua.org.java.dynamoit.components.tablegrid;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import ua.org.java.dynamoit.utils.DX;

import java.text.DateFormat;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static atlantafx.base.theme.Styles.FLAT;
import static ua.org.java.dynamoit.utils.Utils.copyToClipboard;

public class TableInfoDialog extends Dialog<Void> {

    public TableInfoDialog(TableGridModel tableModel, Consumer<String> openUrl) {
        setTitle(tableModel.getTableName());
        getDialogPane().getButtonTypes().add(ButtonType.OK);
        setResizable(true);
        getDialogPane().setContent(DX.create(GridPane::new, gridPane -> {
            gridPane.setHgap(10);
            gridPane.setVgap(10);
            gridPane.setPadding(new Insets(14, 14, 0, 14));

            gridPane.getColumnConstraints().addAll(
                    new ColumnConstraints(110),
                    DX.create(ColumnConstraints::new, c -> {
                        c.setHgrow(Priority.ALWAYS);
                    })
            );

            Function<Supplier<String>, Node> copyClipboardWidget = stringSupplier -> DX.create(Button::new, button -> {
                button.setGraphic(DX.icon("icons/page_copy.png"));
                button.setOnAction(__ -> copyToClipboard(stringSupplier.get()));
                button.setTooltip(new Tooltip("Copy to clipboard"));
                button.getStyleClass().addAll(FLAT);
            });

            gridPane.addColumn(0,
                    DX.boldLabel("Name:"),
                    DX.boldLabel("Arn:"),
                    DX.boldLabel("Creation date:"),
                    DX.boldLabel("Size:"),
                    DX.boldLabel("Region:")
            );

            gridPane.addColumn(1,
                    DX.create(Hyperlink::new, link -> {
                        String tableLink = String.format(
                                "https://%1$s.console.aws.amazon.com/dynamodb/home?region=%1$s#tables:selected=%2$s;tab=overview",
                                tableModel.getProfileModel().getRegion(),
                                tableModel.getTableName()
                        );
                        link.setText(tableModel.getOriginalTableDescription().getTableName().trim());
                        link.setOnMouseClicked(event -> openUrl.accept(tableLink));
                    }),
                    new Label(tableModel.getOriginalTableDescription().getTableArn()),
                    new Label(DateFormat.getInstance().format(tableModel.getOriginalTableDescription().getCreationDateTime())),
                    new Label(tableModel.getOriginalTableDescription().getTableSizeBytes() + " bytes"),
                    new Label(tableModel.getProfileModel().getRegion())
            );

            gridPane.addColumn(2,
                    copyClipboardWidget.apply(() -> tableModel.getOriginalTableDescription().getTableName()),
                    copyClipboardWidget.apply(() -> tableModel.getOriginalTableDescription().getTableArn()),
                    copyClipboardWidget.apply(() -> DateFormat.getInstance().format(tableModel.getOriginalTableDescription().getCreationDateTime())),
                    copyClipboardWidget.apply(() -> "" + tableModel.getOriginalTableDescription().getTableSizeBytes()),
                    copyClipboardWidget.apply(() -> tableModel.getProfileModel().getRegion())
            );

            String streamArn = tableModel.getOriginalTableDescription().getLatestStreamArn();
            if (streamArn != null && !streamArn.isBlank()) {
                gridPane.addRow(gridPane.getRowCount(), DX.boldLabel("Stream:"), new Label(streamArn), copyClipboardWidget.apply(() -> streamArn));
            }
        }));

    }
}
