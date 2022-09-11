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

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import ua.org.java.dynamoit.utils.DX;

import java.io.IOException;
import java.util.jar.Manifest;

public class DynamoItApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        System.setProperty("prism.lcdtext", "false");

        AppFactory appFactory = DaggerAppFactory.builder().application(this).build();
        appFactory.themeManager().applyCurrentTheme();

        primaryStage.getIcons().add(new Image("icons/dynamite.png"));
        primaryStage.setTitle(buildAppTitle());
        primaryStage.setScene(
                DX.scene(() -> {
                            Region mainView = appFactory.mainView();
                            mainView.setPrefWidth(1000);
                            mainView.setPrefHeight(600);
                            return mainView;
                        }
                )
        );

        primaryStage.getScene().getStylesheets().add(getClass().getResource("/css/global.css").toExternalForm());
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(DynamoItApp.class);
    }

    private static String buildAppTitle() {
        String title = "DynamoIt";
        var manifestStream = DynamoItApp.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF");
        if (manifestStream != null) {
            try (manifestStream) {
                Manifest manifest = new Manifest(manifestStream);
                String version = manifest.getMainAttributes().getValue("Implementation-Version");
                if (version != null) {
                    return String.format("%s: %s", title, version);
                }
            } catch (IOException ignored) {
            }
        }
        return title;
    }

}
