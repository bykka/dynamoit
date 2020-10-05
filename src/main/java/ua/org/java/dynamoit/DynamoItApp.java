/*
 * This file is part of DynamoIt.
 *
 *     DynamoIt is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Foobar is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with DynamoIt.  If not, see <https://www.gnu.org/licenses/>.
 */

package ua.org.java.dynamoit;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import ua.org.java.dynamoit.utils.DX;

public class DynamoItApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.getIcons().add(new Image("icons/dynamite.png"));
        primaryStage.setTitle("DynamoIt");
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

        JMetro jMetro = new JMetro(Style.LIGHT);
        jMetro.setScene(primaryStage.getScene());

        primaryStage.getScene().getStylesheets().add(getClass().getResource("/css/global.css").toExternalForm());
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(DynamoItApp.class);
    }

}
