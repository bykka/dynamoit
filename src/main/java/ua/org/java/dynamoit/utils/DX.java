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

package ua.org.java.dynamoit.utils;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class DX {

    private DX() {}

    public static Scene scene(Supplier<Parent> rootBuilder) {
        return new Scene(rootBuilder.get());
    }

    public static <T extends Pane> T create(Supplier<T> creator, Function<T, List<Node>> builder) {
        T pane = creator.get();
        pane.getChildren().addAll(builder.apply(pane));
        return pane;
    }

    public static SplitPane splitPane(Function<SplitPane, List<Node>> builder) {
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(builder.apply(splitPane));
        return splitPane;
    }

    public static ToolBar toolBar(Function<ToolBar, List<Node>> builder) {
        ToolBar toolBar = new ToolBar();
        toolBar.getItems().addAll(builder.apply(toolBar));
        return toolBar;
    }

    public static ContextMenu contextMenu(Function<ContextMenu, List<MenuItem>> builder) {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(builder.apply(contextMenu));
        return contextMenu;
    }

    public static Node spacer() {
        Region spring = new Region();
        HBox.setHgrow(spring, Priority.ALWAYS);
        return spring;
    }

    public static <T> T create(Supplier<T> creator, Consumer<T> builder){
        T component = creator.get();
        builder.accept(component);
        return component;
    }

    public static ImageView icon(String icon){
        return new ImageView(new Image(icon, 16, 16, true, true));
    }

    public static Label boldLabel(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-font-weight: bold");
        return label;
    }

}
