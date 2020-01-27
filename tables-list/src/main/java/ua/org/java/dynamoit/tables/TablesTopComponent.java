package ua.org.java.dynamoit.tables;

import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;

import java.awt.*;
import java.util.ArrayList;

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
        add(new BeanTreeView(), BorderLayout.CENTER);

        DynamoDbClient dynamoDbClient = DynamoDbClient.create();

        ListTablesResponse listTablesResponse = dynamoDbClient.listTables();

        AbstractNode rootNode = new AbstractNode(Children.create(new TableChildFactory(listTablesResponse.tableNames()), true));
        rootNode.setName("tables");
        rootNode.setDisplayName("All tables");
        manager.setRootContext(rootNode);
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return this.manager;
    }
}
