package ua.org.java.dynamoit.utils;

import javafx.application.Platform;

import java.util.concurrent.Executor;

public class FXExecutor implements Executor {

    private static final FXExecutor INSTANCE = new FXExecutor();

    public static Executor getInstance() {
        return INSTANCE;
    }

    @Override
    public void execute(Runnable command) {
        Platform.runLater(command);
    }

}
