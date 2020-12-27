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


import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.Selection;
import org.fxmisc.richtext.SelectionImpl;
import ua.org.java.dynamoit.utils.DX;
import ua.org.java.dynamoit.utils.FXExecutor;
import ua.org.java.dynamoit.utils.ObservableListIterator;
import ua.org.java.dynamoit.utils.Utils;
import ua.org.java.dynamoit.widgets.JsonEditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class CompareDialog extends Dialog<Void> {

    private final JsonEditor doc1Area = new JsonEditor();
    private final JsonEditor doc2Area = new JsonEditor();
    private final ObservableListIterator<Integer> diffIterator = new ObservableListIterator<>();
    private final IntegerProperty diffCount = new SimpleIntegerProperty(0);

    public CompareDialog(String text1, String text2) {
        ((Stage) this.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/edit_diff.png"));
        this.setTitle("Compare documents");
        this.setResizable(true);
        this.initModality(Modality.NONE);
        this.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        this.setHeight(800);
        doc1Area.setPrefWidth(500);
        doc1Area.getStylesheets().add(getClass().getResource("/css/toggle-buttons.css").toExternalForm());
        doc2Area.setPrefWidth(500);

        VirtualizedScrollPane<JsonEditor> pane1 = new VirtualizedScrollPane<>(doc1Area);
        VirtualizedScrollPane<JsonEditor> pane2 = new VirtualizedScrollPane<>(doc2Area);
        this.getDialogPane().setContent(
                DX.create(VBox::new, vBox -> {
                    vBox.getChildren().addAll(
                            DX.toolBar(toolBar -> List.of(
                                    DX.create(Button::new, button -> {
                                        button.setGraphic(DX.icon("icons/arrow_up.png"));
                                        button.setTooltip(new Tooltip("Previous difference"));
                                        button.setDisable(true);
                                        button.setOnAction(event -> scrollTo(diffIterator.previous()));
                                        button.disableProperty().bind(Bindings.not(diffIterator.hasPreviousProperty()));
                                    }),
                                    DX.create(Button::new, button -> {
                                        button.setGraphic(DX.icon("icons/arrow_down.png"));
                                        button.setTooltip(new Tooltip("Next difference"));
                                        button.setDisable(true);
                                        button.setOnAction(event -> scrollTo(diffIterator.next()));
                                        button.disableProperty().bind(Bindings.not(diffIterator.hasNextProperty()));
                                    }),
                                    DX.spacer(),
                                    DX.create(Label::new, label -> {
                                        label.textProperty().bind(
                                                Bindings.createStringBinding(() -> diffCount.get() + " difference" + (diffCount.get() == 1 ? "" : "s"),
                                                        diffCount
                                                ));
                                    })
                            )),
                            DX.create(SplitPane::new, pane -> {
                                pane.getItems().addAll(pane1, pane2);
                                VBox.setVgrow(pane, Priority.ALWAYS);
                            })
                    );
                })
        );

        pane1.estimatedScrollYProperty().bindBidirectional(pane2.estimatedScrollYProperty());

        CompletableFuture<List<DiffRow>> diffFuture = CompletableFuture.supplyAsync(() -> {
            DiffRowGenerator generator = DiffRowGenerator.create()
                    .ignoreWhiteSpaces(true)
                    .build();
            return generator.generateDiffRows(
                    Arrays.asList(text1.split("\\R")),
                    Arrays.asList(text2.split("\\R")));
        });

        this.setOnShowing(event -> {
            diffFuture.thenAcceptAsync(rows -> {
                List<Integer> diffIndexes = new ArrayList<>();
                AtomicInteger index = new AtomicInteger(0);
                rows.forEach(diffRow -> {
                    doc1Area.appendText(diffRow.getOldLine());
                    doc1Area.appendText("\n");
                    doc2Area.appendText(diffRow.getNewLine());
                    doc2Area.appendText("\n");

                    switch (diffRow.getTag()) {
                        case CHANGE:
                        case DELETE:
                        case INSERT:
                            if (!diffRow.getOldLine().isBlank() && !diffRow.getNewLine().isBlank()) {
                                doc1Area.addSelection(createSelection(doc1Area, diffRow.getOldLine(), index.get(), Color.LIGHTBLUE));
                                doc2Area.addSelection(createSelection(doc2Area, diffRow.getNewLine(), index.get(), Color.LIGHTBLUE));
                            }
                            if (diffRow.getOldLine().isBlank()) {
                                doc2Area.addSelection(createSelection(doc2Area, diffRow.getNewLine(), index.get(), Color.LIGHTGREEN));
                            }
                            if (diffRow.getNewLine().isBlank()) {
                                doc1Area.addSelection(createSelection(doc1Area, diffRow.getOldLine(), index.get(), Color.LIGHTGREEN));
                            }
                            diffIndexes.add(index.get());
                    }
                    index.getAndIncrement();
                });
                List<Integer> diffBlocks = Utils.skipSequences(diffIndexes);
                diffIterator.setIterator(diffBlocks.listIterator());
                diffCount.set(diffBlocks.size());
            }, FXExecutor.getInstance());
        });
    }

    private Selection<Collection<String>, String, Collection<String>> createSelection(JsonEditor editor, String text, int index, Color color) {
        Selection<Collection<String>, String, Collection<String>> selection = new SelectionImpl<>(
                "diff" + System.currentTimeMillis(),
                editor,
                path -> path.setHighlightFill(color));
        selection.selectRange(index, 0, index, text.length());
        return selection;
    }

    private void scrollTo(int lineNumber) {
        Bounds bb = doc1Area.getLayoutBounds();
        int percent = (int) (bb.getHeight() / 3);
        doc1Area.showParagraphRegion(lineNumber, new BoundingBox(bb.getMinX() + percent, 0, 0, bb.getHeight() - percent));
        doc1Area.moveTo(lineNumber, 0);
    }
}
