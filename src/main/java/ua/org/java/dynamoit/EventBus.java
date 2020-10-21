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

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import ua.org.java.dynamoit.components.dialog.ExceptionDialog;
import ua.org.java.dynamoit.components.tablegrid.TableGridContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class EventBus {

    private final SimpleIntegerProperty activityCount = new SimpleIntegerProperty();
    private final SimpleObjectProperty<TableGridContext> selectedTable = new SimpleObjectProperty<>();
    private final Executor uiExecutor;

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
        return activity(completableFuture, null, null);
    }

    public <T> CompletableFuture<T> activity(CompletableFuture<T> completableFuture, String errorMessage, String errorDescription) {
        startActivity();
        return completableFuture.whenComplete((o, throwable) -> {
            stopActivity();
            if (throwable != null) {
                throwable.printStackTrace();
                CompletableFuture.runAsync(() -> new ExceptionDialog(errorMessage, errorDescription, throwable).show(), uiExecutor);
            }
        });
    }

    public void setSelectedTable(TableGridContext context) {
        this.selectedTable.set(context);
    }

    public SimpleObjectProperty<TableGridContext> selectedTableProperty() {
        return selectedTable;
    }
}
