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

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import ua.org.java.dynamoit.model.profile.LocalProfileDetails;
import ua.org.java.dynamoit.model.profile.ProfileDetails;
import ua.org.java.dynamoit.model.profile.RemoteProfileDetails;
import ua.org.java.dynamoit.utils.DX;
import ua.org.java.dynamoit.widgets.ValidateTextField;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static ua.org.java.dynamoit.utils.RegionsUtils.ALL_REGIONS;
import static ua.org.java.dynamoit.utils.RegionsUtils.DEFAULT_REGION;

public class NewProfileDialog extends Dialog<ProfileDetails> {

    private static final int LABEL_SIZE = 90;
    private static final Map<String, Predicate<String>> REQUIRED_VALIDATION_RULE = Map.of("Required", s -> s == null || s.isBlank());

    private final SimpleStringProperty profileNameProperty = new SimpleStringProperty();
    private final BooleanProperty profileNameValidProperty = new SimpleBooleanProperty();
    private final SimpleStringProperty regionProperty = new SimpleStringProperty(DEFAULT_REGION);
    private final SimpleStringProperty accessKeyProperty = new SimpleStringProperty();
    private final BooleanProperty accessKeyValidProperty = new SimpleBooleanProperty();
    private final SimpleStringProperty securityKeyProperty = new SimpleStringProperty();
    private final BooleanProperty securityKeyValidProperty = new SimpleBooleanProperty();
    private final SimpleStringProperty endpointUrlProperty = new SimpleStringProperty();
    private final BooleanProperty endpointUrlValidProperty = new SimpleBooleanProperty();
    private final BooleanProperty remoteTabSelectedProperty = new SimpleBooleanProperty();
    private final BooleanProperty localTabSelectedProperty = new SimpleBooleanProperty();

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
                gridPane.addRow(0, DX.boldLabel("Profile:"), DX.create(() -> new ValidateTextField(REQUIRED_VALIDATION_RULE), profileTextField -> {
                    profileNameProperty.bindBidirectional(profileTextField.textProperty());
                    profileNameValidProperty.bind(profileTextField.isValidProperty());
                }));
            };

            tabPane.getTabs().add(DX.create(Tab::new, tab -> {
                tab.setText("Remote");
                remoteTabSelectedProperty.bind(tab.selectedProperty());
                tab.setContent(DX.create(GridPane::new, gridPane -> {
                    defaultSettings.accept(gridPane);

                    gridPane.addRow(1, DX.boldLabel("Access key:"), DX.create(() -> new ValidateTextField(REQUIRED_VALIDATION_RULE), textField -> {
                        accessKeyProperty.bind(textField.textProperty());
                        accessKeyValidProperty.bind(textField.isValidProperty());
                    }));
                    gridPane.addRow(2, DX.boldLabel("Security key:"), DX.create(() -> new ValidateTextField(REQUIRED_VALIDATION_RULE), textField -> {
                        securityKeyProperty.bind(textField.textProperty());
                        securityKeyValidProperty.bind(textField.isValidProperty());
                    }));
                    gridPane.addRow(3, DX.boldLabel("Region:"), DX.create(() -> new ChoiceBox<String>(), regionChoiceBox -> {
                        regionChoiceBox.getItems().addAll(ALL_REGIONS);
                        regionChoiceBox.valueProperty().bindBidirectional(regionProperty);
                    }));
                }));
            }));
            tabPane.getTabs().add(DX.create(Tab::new, tab -> {
                tab.setText("Local");
                localTabSelectedProperty.bind(tab.selectedProperty());
                tab.setContent(DX.create(GridPane::new, gridPane -> {
                    defaultSettings.accept(gridPane);

                    Map<String, Predicate<String>> validationRules = new LinkedHashMap<>(REQUIRED_VALIDATION_RULE);
                    validationRules.put("Should be URL", text -> !text.startsWith("http://") && !text.startsWith("https://"));

                    gridPane.addRow(1, DX.boldLabel("Endpoint url:"), DX.create(() -> new ValidateTextField(validationRules), textField -> {
                        endpointUrlProperty.bindBidirectional(textField.textProperty());
                        endpointUrlValidProperty.bind(textField.isValidProperty());
                    }));
                }));
            }));
        }));

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Node okButton = getDialogPane().lookupButton(ButtonType.OK);

        if (okButton != null) {
            okButton.disableProperty().bind(
                    Bindings.createBooleanBinding(() -> {
                        if (localTabSelectedProperty.get()) {
                            return !(profileNameValidProperty.get() && endpointUrlValidProperty.get());
                        } else if (remoteTabSelectedProperty.get()) {
                            return !(profileNameValidProperty.get() && accessKeyValidProperty.get() && securityKeyValidProperty.get());
                        }
                        return true;
                    }, remoteTabSelectedProperty, localTabSelectedProperty, profileNameValidProperty, endpointUrlValidProperty, accessKeyValidProperty, securityKeyValidProperty)
            );
        }

        this.setResultConverter(param -> {
            if (param == ButtonType.OK) {
                if (localTabSelectedProperty.get()) {
                    return new LocalProfileDetails(profileNameProperty.get(), endpointUrlProperty.get());
                } else if (remoteTabSelectedProperty.get()) {
                    return new RemoteProfileDetails(profileNameProperty.get(), regionProperty.get(), accessKeyProperty.get(), securityKeyProperty.get());
                }
            }
            return null;
        });

    }
}
