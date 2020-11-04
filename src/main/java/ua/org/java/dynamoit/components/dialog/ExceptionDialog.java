package ua.org.java.dynamoit.components.dialog;

import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ua.org.java.dynamoit.utils.DX;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class ExceptionDialog extends Alert {

    public ExceptionDialog(Throwable e) {
        this(e.getLocalizedMessage(), e);
    }

    public ExceptionDialog(String message, Throwable e) {
        this(message, null, e);
    }

    public ExceptionDialog(String message, String description, Throwable e) {
        super(AlertType.ERROR);
        setResizable(true);

        setTitle("Exceptional error happened");
        setHeaderText(message == null ? e.getLocalizedMessage() : message);
        setContentText(description);

        getDialogPane().setExpandableContent(DX.create(VBox::new, pane -> {
            return List.of(
                    new Label("The exception stacktrace is:"),
                    DX.create(TextArea::new, textArea -> {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        String exceptionText = sw.toString();

                        textArea.setText(exceptionText);
                        textArea.setEditable(false);
                        textArea.setWrapText(true);
                        VBox.setVgrow(textArea, Priority.ALWAYS);
                    })
            );
        }));


    }

}
