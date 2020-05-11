package ua.org.java.dynamoit;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Alert;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class EventBus {

    private final SimpleIntegerProperty activityCount = new SimpleIntegerProperty();
    private Executor uiExecutor;

    public EventBus(Executor uiExecutor) {
        this.uiExecutor = uiExecutor;
    }

    public void startActivity() {
        if (Platform.isFxApplicationThread()) {
            activityCount.set(activityCount.get() + 1);
        } else {
            CompletableFuture.runAsync(() -> activityCount.set(activityCount.get() + 1), uiExecutor);
        }
    }

    public void stopActivity() {
        if (Platform.isFxApplicationThread()) {
            activityCount.set(activityCount.get() - 1);
        } else {
            CompletableFuture.runAsync(() -> activityCount.set(activityCount.get() - 1), uiExecutor);
        }
    }

    public SimpleIntegerProperty activityCountProperty() {
        return activityCount;
    }

    public <T> CompletableFuture<T> activity(CompletableFuture<T> completableFuture) {
        startActivity();
        return completableFuture.whenComplete((o, throwable) -> {
            stopActivity();
            if (throwable != null) {
                throwable.printStackTrace();
                CompletableFuture.runAsync(() -> new Alert(Alert.AlertType.ERROR, throwable.getLocalizedMessage()).show(), uiExecutor);
            }
        });
    }

}
