package ua.org.java.dynamoit.utils;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
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

}
