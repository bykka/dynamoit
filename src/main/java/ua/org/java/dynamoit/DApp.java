package ua.org.java.dynamoit;

import javafx.application.Application;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class DApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(
                DX.scene(() -> {
                            AppFactory appFactory = DaggerAppFactory.create();
                            Region mainView = appFactory.mainView();
                            mainView.setPrefWidth(1000);
                            mainView.setPrefHeight(600);
                            return mainView;
                        }
                )
        );
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(DApp.class);
    }

}
