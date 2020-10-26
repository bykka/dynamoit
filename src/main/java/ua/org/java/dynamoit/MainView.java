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

package ua.org.java.dynamoit;

import io.reactivex.rxjavafx.observables.JavaFxObservable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ua.org.java.dynamoit.components.activityindicator.ActivityIndicator;
import ua.org.java.dynamoit.components.profileviewer.ProfileComponent;
import ua.org.java.dynamoit.components.profileviewer.ProfileView;
import ua.org.java.dynamoit.components.tablegrid.TableGridComponent;
import ua.org.java.dynamoit.components.tablegrid.TableGridContext;
import ua.org.java.dynamoit.components.tablegrid.TableGridView;
import ua.org.java.dynamoit.utils.DX;
import ua.org.java.dynamoit.utils.HighlightColors;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainView extends VBox {

    private final MainModel mainModel;
    private final MainController controller;

    private final ToggleGroup profileToggleGroup = new ToggleGroup();
    private ToolBar profilesToolBar;
    private TabPane tabPane;
    private SplitPane splitPane;
    private final Map<String, ProfileView> profileViews = new HashMap<>();
    private double dividerPosition = 0.35;

    public MainView(MainModel mainModel, MainController controller, ActivityIndicator activityIndicator) {
        this.mainModel = mainModel;
        this.controller = controller;
        this.controller.setSelectedTableConsumer(this::createAndOpenTab);

        this.getChildren().addAll(

                DX.create(HBox::new, (HBox hBox1) -> {
                    VBox.setVgrow(hBox1, Priority.ALWAYS);
                    hBox1.getChildren().addAll(
                            DX.toolBar(toolBar -> {
                                this.profilesToolBar = toolBar;
                                toolBar.setOrientation(Orientation.VERTICAL);
                                toolBar.getStylesheets().add(getClass().getResource("/css/toggle-buttons.css").toExternalForm());
                                return List.of();
                            }),

                            DX.splitPane(splitPane -> {
                                        this.splitPane = splitPane;
                                        HBox.setHgrow(splitPane, Priority.ALWAYS);
                                        splitPane.setDividerPositions(dividerPosition);
                                        return List.of(
                                                DX.create(TabPane::new, (TabPane tabPane) -> {
                                                    this.tabPane = tabPane;
                                                    tabPane.getStylesheets().add(getClass().getResource("/css/tab-pane.css").toExternalForm());
                                                })
                                        );
                                    }
                            )
                    );
                }),


                DX.create(HBox::new, hBox -> {
                    hBox.setPadding(new Insets(3, 3, 3, 3));
                    hBox.setAlignment(Pos.CENTER);
                    hBox.setMinHeight(22);
                    hBox.getChildren().addAll(
                            DX.create(Pane::new, pane -> {
                                HBox.setHgrow(pane, Priority.ALWAYS);
                            }),
                            activityIndicator
                    );
                })
        );

        Iterator<HighlightColors> colorsIterator = List.of(HighlightColors.values()).iterator();
        JavaFxObservable.additionsOf(mainModel.getAvailableProfiles())
                .subscribe(profile -> {
                    String profileName = profile.getKey();
                    if (colorsIterator.hasNext()) {
                        profile.getValue().setColor(colorsIterator.next());
                    }

                    profilesToolBar.getItems().add(
                            new Group(DX.create(ToggleButton::new, (ToggleButton button) -> {
                                button.setText(profileName);
                                button.setUserData(profileName);
                                button.setRotate(-90);
                                button.setToggleGroup(profileToggleGroup);
                                profile.getValue().getColor().ifPresent(color -> button.getStyleClass().add(color.toggleButtonClass()));
                            }))
                    );
                });

        JavaFxObservable.changesOf(profileToggleGroup.selectedToggleProperty())
                .subscribe(toggleChange -> {
                    if (toggleChange.getOldVal() != null) {
                        dividerPosition = splitPane.getDividerPositions()[0];
                        this.splitPane.getItems().remove(0);
                    }
                    if (toggleChange.getNewVal() != null) {
                        String profileName = toggleChange.getNewVal().getUserData().toString();

                        ProfileView profileView = profileViews.computeIfAbsent(profileName, s -> {
                            ProfileComponent profileComponent = controller.buildProfileComponent(profileName);
                            return profileComponent.view();
                        });

                        this.splitPane.getItems().add(0, profileView);
                        SplitPane.setResizableWithParent(profileView, false);
                        splitPane.setDividerPositions(dividerPosition);
                    }
                });

    }

    private void createAndOpenTab(TableGridContext tableContext) {
        TableGridComponent tableComponent = controller.buildTableGridComponent(tableContext);

        TableGridView tableItemsView = tableComponent.view();
        tableItemsView.setOnSearchInTable(this::createAndOpenTab);

        Tab tab = new Tab(tableContext.getTableName(), tableItemsView);
        MainModel.ProfileModel profileModel = mainModel.getAvailableProfiles().get(tableContext.getProfileName());
        profileModel.getColor().ifPresent(color -> tab.getStyleClass().add(color.tabClass()));

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }


}
