package ua.org.java.dynamoit;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import ua.org.java.dynamoit.utils.FXExecutor;

import java.util.concurrent.CompletableFuture;

public class EventBus {

    private final SimpleIntegerProperty activityCount = new SimpleIntegerProperty();

    public void startActivity() {
        if (Platform.isFxApplicationThread()) {
            activityCount.set(activityCount.get() + 1);
        } else {
            CompletableFuture.runAsync(() -> activityCount.set(activityCount.get() + 1), FXExecutor.getInstance());
        }
    }

    public void stopActivity() {
        if (Platform.isFxApplicationThread()) {
            activityCount.set(activityCount.get() - 1);
        } else {
            CompletableFuture.runAsync(() -> activityCount.set(activityCount.get() - 1), FXExecutor.getInstance());
        }
    }

    public SimpleIntegerProperty activityCountProperty() {
        return activityCount;
    }

    public void activity(CompletableFuture<?> completableFuture) {
        startActivity();
        completableFuture.whenComplete((o, throwable) -> {
            stopActivity();
        });
    }

}
