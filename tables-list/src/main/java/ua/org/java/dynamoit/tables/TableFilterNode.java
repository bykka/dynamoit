package ua.org.java.dynamoit.tables;

import org.openide.actions.OpenAction;
import org.openide.cookies.OpenCookie;
import org.openide.nodes.BeanNode;
import org.openide.nodes.FilterNode;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.Lookups;
import org.openide.windows.TopComponent;

import javax.swing.*;
import java.awt.*;
import java.beans.IntrospectionException;

public class TableFilterNode extends FilterNode {

    private String tableName;

    public TableFilterNode(String tableName) throws IntrospectionException {
        super(new BeanNode<>(new Table(tableName)), org.openide.nodes.Children.LEAF, Lookups.fixed(new TableOpenCookie(tableName)));
        this.tableName = tableName;
    }

    @Override
    public String getHtmlDisplayName() {
        return this.tableName;
    }

    @Override
    public Action[] getActions(boolean context) {
        return new Action[]{SystemAction.get(OpenAction.class)};
    }

    @Override
    public Action getPreferredAction() {
        return getActions(false)[0];
    }

    public static class TableOpenCookie implements OpenCookie {

        private String tableName;

        public TableOpenCookie(String tableName) {
            this.tableName = tableName;
        }

        @Override
        public void open() {
            TableGridView view = new TableGridView(tableName);
            view.open();
            view.requestActive();
        }
    }

    public static class TableGridView extends TopComponent {

        public TableGridView(String tableName) {
            setName(tableName);
            setLayout(new BorderLayout());
            JEditorPane editorPane = new JEditorPane();
            editorPane.setEditable(false);
            editorPane.setText(tableName);
            add(new JScrollPane(editorPane), BorderLayout.CENTER);
        }
    }
}
