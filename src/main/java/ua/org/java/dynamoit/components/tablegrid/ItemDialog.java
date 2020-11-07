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

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.observers.JavaFxObserver;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.controlsfx.control.textfield.TextFields;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.Selection;
import org.fxmisc.richtext.SelectionImpl;
import org.reactfx.EventStream;
import org.reactfx.Subscription;
import ua.org.java.dynamoit.widgets.JsonEditor;
import ua.org.java.dynamoit.utils.DX;
import ua.org.java.dynamoit.utils.ObservableListIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ItemDialog extends Dialog<String> {

    private Subscription validationSubscribe;
    private final JsonEditor textArea = new JsonEditor();
    private final TextField searchField = TextFields.createClearableTextField();
    private Disposable focusDisposable;
    private final List<Selection<Collection<String>, String, Collection<String>>> selections = new ArrayList<>();
    private final SimpleBooleanProperty showSearch = new SimpleBooleanProperty(false);
    private final SimpleObjectProperty<ObservableListIterator<Pair<Integer, String>>> findIterator = new SimpleObjectProperty<>();

    public ItemDialog(String title, String json, Function<EventStream<String>, EventStream<Boolean>> validator) {
        this.setTitle(title);

        Observable<List<Pair<Integer, String>>> matches = JavaFxObservable.changesOf(this.searchField.textProperty())
                .debounce(100, TimeUnit.MILLISECONDS)
                .map(stringChange -> {
                            AtomicInteger index = new AtomicInteger(0);
                            return textArea.getText().lines()
                                    .map(line -> new Pair<>(index.getAndIncrement(), line))
                                    .filter(pair -> !pair.getValue().isBlank() && !stringChange.getNewVal().isBlank() && pair.getValue().contains(stringChange.getNewVal()))
                                    .collect(Collectors.toList());
                        }
                )
                .observeOn(JavaFxScheduler.platform());

        matches.subscribe(list -> {
            findIterator.set(new ObservableListIterator<>(list.listIterator()));
            cleanAllSelections();
            list.forEach(line -> {
                Selection<Collection<String>, String, Collection<String>> selection = createSelection(line);
                textArea.addSelection(selection);
                selections.add(selection);
            });
        });

        textArea.setPrefWidth(800);
        textArea.setPrefHeight(800);
        textArea.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode().equals(KeyCode.F)) {
                this.showToolbar();
            }
        });

        this.getDialogPane().setContent(
                DX.create(VBox::new, vBox -> {
                    vBox.setPadding(new Insets(1, 1, 1, 1));
                    vBox.getChildren().addAll(
                            DX.toolBar(toolBar -> {
                                        toolBar.visibleProperty().bind(showSearch);
                                        toolBar.managedProperty().bind(showSearch);
                                        return List.of(
                                                DX.create(() -> this.searchField, textField -> {
                                                    HBox.setHgrow(textField, Priority.ALWAYS);
                                                    textField.setOnKeyPressed(this::hideToolbarOnEscPress);
                                                }),
                                                DX.create(Label::new, label -> {
                                                    label.textProperty().bind(JavaFxObserver.toBinding(matches.map(list -> list.size() + " matches")));
                                                }),
                                                DX.create(Button::new, button -> {
                                                    button.setGraphic(DX.icon("icons/arrow_up.png"));
                                                    button.setDisable(true);
                                                    button.setOnKeyPressed(this::hideToolbarOnEscPress);
                                                    button.setOnAction(event -> scrollTo(findIterator.get().previous().getKey()));
                                                    findIterator.addListener((observable, oldValue, newValue) -> {
                                                        button.disableProperty().bind(Bindings.not(newValue.hasPreviousProperty()));
                                                    });
                                                }),
                                                DX.create(Button::new, button -> {
                                                    button.setGraphic(DX.icon("icons/arrow_down.png"));
                                                    button.setDisable(true);
                                                    button.setOnKeyPressed(this::hideToolbarOnEscPress);
                                                    button.setOnAction(event -> scrollTo(findIterator.get().next().getKey()));
                                                    findIterator.addListener((observable, oldValue, newValue) -> {
                                                        button.disableProperty().bind(Bindings.not(newValue.hasNextProperty()));
                                                    });
                                                }),
                                                DX.create(Button::new, button -> {
                                                    button.setTooltip(new Tooltip("Hide toolbar"));
                                                    button.setGraphic(DX.icon("icons/cross.png"));
                                                    button.setOnKeyPressed(this::hideToolbarOnEscPress);
                                                    button.setOnAction(event -> hideToolbar());
                                                })
                                        );
                                    }
                            ),
                            DX.create(() -> new VirtualizedScrollPane<>(textArea), pane -> VBox.setVgrow(pane, Priority.ALWAYS))
                    );
                })
        );
        ((Stage) this.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/page.png"));
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.LEFT);
        this.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CLOSE);

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

        this.setOnShowing(event -> {
            validationSubscribe = validator.apply(textArea.multiPlainChanges()
                    .map(__ -> textArea.getText()))
                    .subscribe(valid -> saveButtonDisable.accept(!valid));

            textArea.replaceText(json);
            textArea.requestFocus();
            textArea.moveTo(0, 0);
        });

        this.setOnCloseRequest(event -> {
            if (validationSubscribe != null) {
                validationSubscribe.unsubscribe();
            }
            if (focusDisposable != null) {
                focusDisposable.dispose();
            }
        });
    }

    private void cleanAllSelections() {
        selections.forEach(selection -> {
            selection.deselect();
            textArea.removeSelection(selection);
        });
        selections.clear();
    }

    private void showToolbar() {
        this.showSearch.set(true);

        focusDisposable = Observable.timer(100, TimeUnit.MILLISECONDS)
                .observeOn(JavaFxScheduler.platform())
                .subscribe(aLong -> this.searchField.requestFocus());
    }

    private void hideToolbar() {
        this.showSearch.set(false);
        this.textArea.requestFocus();
        this.focusDisposable.dispose();
        this.searchField.clear();
        this.cleanAllSelections();
    }

    private void hideToolbarOnEscPress(KeyEvent event) {
        if (KeyCode.ESCAPE == event.getCode()) {
            event.consume();
            hideToolbar();
        }
    }

    private void scrollTo(int lineNumber) {
        Bounds bb = textArea.getLayoutBounds();
        int percent = (int) (bb.getHeight() / 3);
        textArea.showParagraphRegion(lineNumber, new BoundingBox(bb.getMinX() + percent, 0, 0, bb.getHeight() - percent));
        textArea.moveTo(lineNumber, 0);
    }

    private Selection<Collection<String>, String, Collection<String>> createSelection(Pair<Integer, String> indexLine) {
        Selection<Collection<String>, String, Collection<String>> selection = new SelectionImpl<>(
                "match" + indexLine.getKey(),
                textArea,
                path -> {
                    path.setStroke(Color.GREEN);
                    path.setStrokeWidth(1);
                    path.setHighlightFill(Color.LIGHTYELLOW);
                });
        selection.selectRange(indexLine.getKey(), 0, indexLine.getKey(), indexLine.getValue().length());
        return selection;
    }

}
