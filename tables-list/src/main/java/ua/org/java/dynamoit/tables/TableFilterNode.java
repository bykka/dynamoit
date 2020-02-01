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
        ((BeanNode)this.getOriginal()).setIconBaseWithExtension("ua/org/java/dynamoit/tables/table.png");
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

//    @Override
//    public Image getIcon(int type) {
//        return ImageUtilities.loadImage(
//                "org/netbeans/examples/modules/povproject/resources/scenes.gif");
//    }


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

            String[] columns = new String[] {
                    "Id", "Name", "Hourly Rate", "Part Time"
            };

            //actual data for the table in a 2d array
            Object[][] data = new Object[][] {
                    {1, "John", 40.0, false },
                    {2, "Rambo", 70.0, false },
                    {3, "Zorro", 60.0, true },
            };

            JTable grid = new JTable(data, columns);
            add(new JScrollPane(grid), BorderLayout.CENTER);


        }
    }
}
