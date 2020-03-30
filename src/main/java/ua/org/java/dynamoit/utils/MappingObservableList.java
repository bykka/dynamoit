package ua.org.java.dynamoit.utils;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.TransformationList;

import java.util.function.Function;

public class MappingObservableList<E, F> extends TransformationList<E, F> {

    private final Function<F, E> mapping;

    protected MappingObservableList(ObservableList<? extends F> source, Function<F, E> mapper) {
        super(source);
        this.mapping = mapper;
    }

    @Override
    protected void sourceChanged(ListChangeListener.Change<? extends F> c) {

    }

    @Override
    public int getSourceIndex(int index) {
        return index;
    }

    @Override
    public int getViewIndex(int index) {
        return index;
    }

    @Override
    public E get(int index) {
        return mapping.apply(getSource().get(index));
    }

    @Override
    public int size() {
        return getSource().size();
    }
}
