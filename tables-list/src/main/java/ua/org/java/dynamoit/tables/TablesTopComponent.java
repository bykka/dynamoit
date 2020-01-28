package ua.org.java.dynamoit.tables;

import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.swing.*;
import java.awt.*;

@TopComponent.Description(
        preferredID = "TablesTopComponent"
)
@TopComponent.Registration(
        mode = "explorer",
        openAtStartup = true
)
@NbBundle.Messages({
        "CTL_TablesTopComponent=Tables",
        "HINT_TablesTopComponent=List of tables"
})
public class TablesTopComponent extends TopComponent implements ExplorerManager.Provider {

    private final ExplorerManager manager = new ExplorerManager();

    public TablesTopComponent() {
        setName(Bundle.CTL_TablesTopComponent());
        setToolTipText(Bundle.HINT_TablesTopComponent());

        setLayout(new BorderLayout());
        add(new JButton("hello"), BorderLayout.NORTH);
        BeanTreeView beanTreeView = new BeanTreeView();
        beanTreeView.setRootVisible(false);
        add(beanTreeView, BorderLayout.CENTER);

        DynamoDbClient dynamoDbClient = DynamoDbClient.create();

        AbstractNode rootNode = new AbstractNode(Children.create(new TableChildFactory(dynamoDbClient), true));
        rootNode.setName("tables");
        rootNode.setDisplayName("All tables");
        manager.setRootContext(rootNode);

        // sync tree and properties views
        associateLookup(ExplorerUtils.createLookup(manager, getActionMap()));
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return this.manager;
    }
}
