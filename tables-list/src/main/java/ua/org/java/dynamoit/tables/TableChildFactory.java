package ua.org.java.dynamoit.tables;

import org.openide.nodes.BeanNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.beans.IntrospectionException;
import java.util.List;

public class TableChildFactory extends ChildFactory<String> {

    private DynamoDbClient dynamoDbClient;

    public TableChildFactory(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    protected boolean createKeys(List<String> list) {
        try {
            list.addAll(dynamoDbClient.listTables().tableNames());
        } catch (Exception e){
            Exceptions.printStackTrace(e);
        }
        return true;
    }

    @Override
    protected Node createNodeForKey(String key) {
        try {
            return new BeanNode<>(new Table(key));
        } catch (IntrospectionException e) {
            Exceptions.printStackTrace(e);
            return null;
        }
    }
}
