package ua.org.java.dynamoit.components.activityindicator;

import javafx.beans.binding.Bindings;
import javafx.scene.control.ProgressIndicator;
import ua.org.java.dynamoit.EventBus;

public class ActivityIndicator extends ProgressIndicator {

    public ActivityIndicator(EventBus eventBus) {
        setPrefSize(16, 16);
        visibleProperty().bind(Bindings.greaterThan(eventBus.activityCountProperty(), 0));
    }
}
