package ua.org.java.dynamoit.tables;

import org.openide.nodes.BeanNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;

import java.beans.IntrospectionException;
import java.util.List;

public class TableChildFactory extends ChildFactory<String> {

    private List<String> tables;

    public TableChildFactory(List<String> tables) {
        this.tables = tables;
    }

    @Override
    protected boolean createKeys(List<String> list) {
        list.addAll(tables);
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
