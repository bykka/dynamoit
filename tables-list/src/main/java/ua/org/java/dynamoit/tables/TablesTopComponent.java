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
import javax.swing.tree.TreeSelectionModel;
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


        DynamoDbClient dynamoDbClient = DynamoDbClient.create();

        TableChildFactory tableChildFactory = new TableChildFactory(dynamoDbClient);
        manager.setRootContext(new AbstractNode(Children.create(tableChildFactory, true)));

        setLayout(new BorderLayout());
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(actionEvent -> tableChildFactory.refresh());
        add(refresh, BorderLayout.NORTH);
        BeanTreeView beanTreeView = new BeanTreeView();
        beanTreeView.setRootVisible(false);
        beanTreeView.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        add(beanTreeView, BorderLayout.CENTER);

        // sync tree and properties views
        associateLookup(ExplorerUtils.createLookup(manager, getActionMap()));
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return this.manager;
    }

    @Override
    protected void componentActivated() {
        ExplorerUtils.activateActions(manager, true);
    }

    @Override
    protected void componentDeactivated() {
        ExplorerUtils.activateActions(manager, false);
    }
}
