package ua.org.java.dynamoit.widgets;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static atlantafx.base.theme.Styles.STATE_DANGER;

public class ValidateTextField extends TextField {

    private final BooleanProperty isValidProperty = new SimpleBooleanProperty();

    /**
     * Shows the error message for the first successful predicate
     *
     * @param validationPredicatesWithMessages messages with predicates
     */
    public ValidateTextField(Map<String, Predicate<String>> validationPredicatesWithMessages) {
        textProperty().addListener((observable, oldValue, newValue) -> {
            Optional<String> errorMessage = validationPredicatesWithMessages.entrySet()
                    .stream()
                    .map(entry -> entry.getValue().test(newValue) ? entry.getKey() : null)
                    .filter(Objects::nonNull)
                    .findFirst();

            if (errorMessage.isPresent()) {
                pseudoClassStateChanged(STATE_DANGER, true);
                Tooltip tooltip = new Tooltip(errorMessage.get());
                tooltip.setShowDelay(Duration.millis(100));
                tooltipProperty().set(tooltip);
                isValidProperty.set(false);
            } else {
                pseudoClassStateChanged(STATE_DANGER, false);
                tooltipProperty().set(null);
                isValidProperty.set(true);
            }
        });
    }

    public boolean getIsValidProperty() {
        return isValidProperty.get();
    }

    public BooleanProperty isValidProperty() {
        return isValidProperty;
    }
}
