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

package ua.org.java.dynamoit.utils;

import javafx.beans.property.SimpleBooleanProperty;

import java.util.ListIterator;

public class ObservableListIterator<T> implements ListIterator<T> {

    private final ListIterator<T> delegate;
    private final SimpleBooleanProperty hasNext = new SimpleBooleanProperty();
    private final SimpleBooleanProperty hasPrevious = new SimpleBooleanProperty();

    public ObservableListIterator(ListIterator<T> delegate) {
        this.delegate = delegate;
        this.hasNext.set(delegate.hasNext());
        this.hasPrevious.set(delegate.hasPrevious());
    }

    public SimpleBooleanProperty hasNextProperty() {
        return this.hasNext;
    }

    public SimpleBooleanProperty hasPreviousProperty() {
        return hasPrevious;
    }

    @Override
    public boolean hasNext() {
        return hasNext.get();
    }

    @Override
    public T next() {
        T value = delegate.next();
        this.hasNext.set(delegate.hasNext());
        this.hasPrevious.set(delegate.hasPrevious());
        return value;
    }

    @Override
    public boolean hasPrevious() {
        return hasPrevious.get();
    }

    @Override
    public T previous() {
        T value = delegate.previous();
        this.hasNext.set(delegate.hasNext());
        this.hasPrevious.set(delegate.hasPrevious());
        return value;
    }

    @Override
    public int nextIndex() {
        return delegate.nextIndex();
    }

    @Override
    public int previousIndex() {
        return delegate.previousIndex();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(T t) {
        throw new UnsupportedOperationException();
    }
}
