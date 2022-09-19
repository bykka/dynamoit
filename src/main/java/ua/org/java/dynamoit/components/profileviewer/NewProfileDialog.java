package ua.org.java.dynamoit.components.profileviewer;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

public class NewProfileDialog extends Dialog<Void> {

    public NewProfileDialog() {
        setTitle("Create a new Profile");

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    }
}
