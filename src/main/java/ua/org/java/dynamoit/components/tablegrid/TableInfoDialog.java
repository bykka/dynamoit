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
import javafx.scene.image.Image;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndexDescription;
import software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndexDescription;
import ua.org.java.dynamoit.utils.DX;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static atlantafx.base.theme.Styles.FLAT;
import static ua.org.java.dynamoit.utils.Utils.copyToClipboard;

public class TableInfoDialog extends Dialog<Void> {

    private final TableGridModel tableModel;
    private final Consumer<String> openUrl;

    public TableInfoDialog(TableGridModel tableModel, Consumer<String> openUrl) {
        this.tableModel = tableModel;
        this.openUrl = openUrl;

        setTitle(tableModel.getTableName());
        ((Stage) this.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/information.png"));
        getDialogPane().getButtonTypes().add(ButtonType.OK);
        setResizable(true);

        getDialogPane().setContent(DX.create(TabPane::new, tabPane -> {
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

            tabPane.getTabs().add(DX.create(Tab::new, tab -> {
                tab.setText("Overview");
                tab.setContent(buildTableOverview());
            }));

            if (this.tableModel.getOriginalTableDescription().hasLocalSecondaryIndexes()) {
                tabPane.getTabs().add(DX.create(Tab::new, tab -> {
                    tab.setText("Local indexes");
                    tab.setContent(buildLocalIndexes());
                }));
            }

            if (this.tableModel.getOriginalTableDescription().hasGlobalSecondaryIndexes()) {
                tabPane.getTabs().add(DX.create(Tab::new, tab -> {
                    tab.setText("Global indexes");
                    tab.setContent(buildGlobalIndexes());
                }));
            }

        }));
    }

    private Node buildTableOverview() {
        return DX.create(TableInfoDialog::buildEmptyGridPane, gridPane -> {
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
                        link.setText(tableModel.getOriginalTableDescription().tableName().trim());
                        link.setOnMouseClicked(event -> openUrl.accept(tableLink));
                    }),
                    new Label(tableModel.getOriginalTableDescription().tableArn()),
                    new Label(DateFormat.getInstance().format(Date.from(tableModel.getOriginalTableDescription().creationDateTime()))),
                    new Label(tableModel.getOriginalTableDescription().tableSizeBytes() + " bytes"),
                    new Label(tableModel.getProfileModel().getRegion())
            );

            gridPane.addColumn(2,
                    copyClipboardWidget(() -> tableModel.getOriginalTableDescription().tableName()),
                    copyClipboardWidget(() -> tableModel.getOriginalTableDescription().tableArn()),
                    copyClipboardWidget(() -> DateFormat.getInstance().format(Date.from(tableModel.getOriginalTableDescription().creationDateTime()))),
                    copyClipboardWidget(() -> "" + tableModel.getOriginalTableDescription().tableSizeBytes()),
                    copyClipboardWidget(() -> tableModel.getProfileModel().getRegion())
            );

            String streamArn = tableModel.getOriginalTableDescription().latestStreamArn();
            if (streamArn != null && !streamArn.isBlank()) {
                gridPane.addRow(gridPane.getRowCount(), DX.boldLabel("Stream:"), new Label(streamArn), copyClipboardWidget(() -> streamArn));
            }
        });
    }

    private Node buildLocalIndexes() {
        return DX.create(TableInfoDialog::buildEmptyGridPane, gridPane -> {
            List<LocalSecondaryIndexDescription> indexes = this.tableModel.getOriginalTableDescription().localSecondaryIndexes();
            if (indexes != null) {

                gridPane.getColumnConstraints().clear();
                gridPane.getColumnConstraints().addAll(
                        new ColumnConstraints(10), // padding for details
                        new ColumnConstraints(140), // details label
                        DX.create(ColumnConstraints::new, (ColumnConstraints c) -> c.setHgrow(Priority.ALWAYS)) // details value
                );

                AtomicInteger startRow = new AtomicInteger();
                indexes.forEach(indexDescription -> {
                    gridPane.add(DX.boldLabel(indexDescription.indexName()), 0, startRow.get(), 3, 1);
                    gridPane.add(copyClipboardWidget(indexDescription::indexName), 3, startRow.get());

                    startRow.incrementAndGet();

                    indexDescription.keySchema().forEach(keySchemaElement -> {
                        gridPane.add(DX.boldLabel(keySchemaElement.keyType() + ":"), 1, startRow.get());
                        gridPane.add(new Label(keySchemaElement.attributeName()), 2, startRow.getAndIncrement());
                    });

                    gridPane.add(DX.boldLabel("Projection type:"), 1, startRow.get());
                    gridPane.add(new Label(indexDescription.projection().projectionType().toString()), 2, startRow.getAndIncrement());

                    if (indexDescription.projection().nonKeyAttributes() != null) {
                        gridPane.add(DX.boldLabel("Projection attributes:"), 1, startRow.get());
                        gridPane.add(new Label(String.join(",", indexDescription.projection().nonKeyAttributes())), 2, startRow.getAndIncrement());
                    }
                });

            }
        });
    }

    private Node buildGlobalIndexes() {
        return DX.create(TableInfoDialog::buildEmptyGridPane, gridPane -> {
            List<GlobalSecondaryIndexDescription> globalSecondaryIndexes = this.tableModel.getOriginalTableDescription().globalSecondaryIndexes();
            if (globalSecondaryIndexes != null) {

                gridPane.getColumnConstraints().clear();
                gridPane.getColumnConstraints().addAll(
                        new ColumnConstraints(10), // padding for details
                        new ColumnConstraints(140), // details label
                        DX.create(ColumnConstraints::new, (ColumnConstraints c) -> c.setHgrow(Priority.ALWAYS)) // details value
                );

                AtomicInteger startRow = new AtomicInteger();
                globalSecondaryIndexes.forEach(indexDescription -> {
                    gridPane.add(DX.boldLabel(indexDescription.indexName()), 0, startRow.get(), 3, 1);
                    gridPane.add(copyClipboardWidget(indexDescription::indexName), 3, startRow.get());

                    startRow.incrementAndGet();

                    indexDescription.keySchema().forEach(keySchemaElement -> {
                        gridPane.add(DX.boldLabel(keySchemaElement.keyType() + ":"), 1, startRow.get());
                        gridPane.add(new Label(keySchemaElement.attributeName()), 2, startRow.getAndIncrement());
                    });

                    gridPane.add(DX.boldLabel("Projection type:"), 1, startRow.get());
                    gridPane.add(new Label(indexDescription.projection().projectionType().toString()), 2, startRow.getAndIncrement());

                    if (indexDescription.projection().nonKeyAttributes() != null) {
                        gridPane.add(DX.boldLabel("Projection attributes:"), 1, startRow.get());
                        gridPane.add(new Label(String.join(",", indexDescription.projection().nonKeyAttributes())), 2, startRow.getAndIncrement());
                    }
                });

            }
        });
    }

    private static GridPane buildEmptyGridPane() {
        return DX.create(GridPane::new, gridPane -> {
            gridPane.setHgap(10);
            gridPane.setVgap(10);
            gridPane.setPadding(new Insets(14, 14, 0, 14));

            gridPane.getColumnConstraints().addAll(
                    new ColumnConstraints(110),
                    DX.create(ColumnConstraints::new, c -> {
                        c.setHgrow(Priority.ALWAYS);
                    })
            );
        });
    }

    private static Node copyClipboardWidget(Supplier<String> stringSupplier) {
        return DX.create(Button::new, button -> {
            button.setGraphic(DX.icon("icons/page_copy.png"));
            button.setOnAction(__ -> copyToClipboard(stringSupplier.get()));
            button.setTooltip(new Tooltip("Copy to clipboard"));
            button.getStyleClass().addAll(FLAT);
        });
    }
}
