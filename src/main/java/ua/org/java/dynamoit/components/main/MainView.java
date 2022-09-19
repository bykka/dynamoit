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

package ua.org.java.dynamoit.components.main;

import io.reactivex.rxjavafx.observables.JavaFxObservable;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ua.org.java.dynamoit.components.profileviewer.NewProfileDialog;
import ua.org.java.dynamoit.components.profileviewer.ProfileComponent;
import ua.org.java.dynamoit.components.profileviewer.ProfileView;
import ua.org.java.dynamoit.components.tablegrid.TableGridComponent;
import ua.org.java.dynamoit.components.tablegrid.TableGridContext;
import ua.org.java.dynamoit.components.tablegrid.TableGridView;
import ua.org.java.dynamoit.components.thememanager.ThemeManager;
import ua.org.java.dynamoit.utils.DX;
import ua.org.java.dynamoit.utils.HighlightColors;
import ua.org.java.dynamoit.widgets.ActivityIndicator;

import javax.inject.Inject;
import java.util.*;

import static atlantafx.base.theme.Styles.BUTTON_ICON;

public class MainView extends VBox {

    private final MainModel mainModel;
    private final MainController controller;

    private final ToggleGroup profileToggleGroup = new ToggleGroup();
    private ToolBar profilesToolBar;
    private TabPane tabPane;
    private SplitPane splitPane;
    private final Map<String, ProfileView> profileViews = new HashMap<>();
    private double dividerPosition = 0.35;

    @Inject
    public MainView(MainModel mainModel, MainController controller, ActivityIndicator activityIndicator, ThemeManager themeManager) {
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
                                return List.of(
                                        DX.create(Button::new, button -> {
                                            button.setGraphic(DX.icon("icons/add.png"));
                                            button.getStyleClass().addAll(BUTTON_ICON);
                                            button.setTooltip(new Tooltip("Add a new profile"));
                                            button.setOnAction(actionEvent -> {
                                                new NewProfileDialog().show();
                                            });
                                        }),
                                        DX.spacerV(),
                                        DX.create(ToggleButton::new, (ToggleButton button) -> {
                                            button.setGraphic(DX.icon("icons/earth_night.png"));
                                            button.getStyleClass().addAll(BUTTON_ICON);
                                            button.setOnAction(actionEvent -> {
                                                themeManager
                                                        .switchTheme()
                                                        .applyCurrentTheme();

                                                String icon = themeManager.getCurrentTheme().isDarkMode() ? "icons/weather_sun.png" : "icons/earth_night.png";
                                                button.setGraphic(DX.icon(icon));

                                                Window.getWindows().forEach(window -> themeManager.applyPseudoClasses(window.getScene().getRoot()));
                                            });
                                        })
                                );
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

                    profilesToolBar.getItems().add(profilesToolBar.getItems().size() - 3,
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

        tabPane.getTabs().add(
                DX.create(() -> new Tab(tableContext.tableName(), tableItemsView), tab -> {
                    MainModel.ProfileModel profileModel = mainModel.getAvailableProfiles().get(tableContext.profileName());
                    profileModel.getColor().ifPresent(color -> tab.getStyleClass().add(color.tabClass()));
                    tab.setContextMenu(DX.contextMenu(contextMenu -> List.of(
                            DX.create(MenuItem::new, menu -> {
                                menu.setText("Close");
                                menu.setOnAction(__ -> closeTab(tab));
                            }),
                            DX.create(MenuItem::new, menu -> {
                                menu.setText("Close others");
                                menu.setOnAction(__ -> closeOtherTabs(tab));
                            }),
                            DX.create(MenuItem::new, menu -> {
                                menu.setText("Close all");
                                menu.setOnAction(__ -> closeAllTabs());
                            })
                    )));
                })
        );
        tabPane.getSelectionModel().selectLast();
    }

    private void closeTab(Tab tab) {
        tabPane.getTabs().remove(tab);
        if (tab.getOnClosed() != null) {
            Event.fireEvent(tab, new Event(Tab.CLOSED_EVENT));
        }
    }

    private void closeOtherTabs(Tab tabToKeep) {
        List<Tab> tabs = tabPane.getTabs().stream().filter(tab -> tab != tabToKeep).toList();

        tabs.forEach(this::closeTab);
    }

    private void closeAllTabs() {
        List<Tab> tabs = new ArrayList<>(tabPane.getTabs());
        tabs.forEach(this::closeTab);
    }

}
