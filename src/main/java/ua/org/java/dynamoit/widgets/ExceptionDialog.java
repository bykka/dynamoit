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

package ua.org.java.dynamoit.widgets;

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
