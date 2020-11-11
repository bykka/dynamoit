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

package ua.org.java.dynamoit.components.tablegrid.parser;

import com.amazonaws.services.dynamodbv2.document.internal.Filter;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseValueToFilterParser<T extends Filter<T>> implements ValueToFilterParser<T> {

    private final Matcher matcher;
    protected T filter;

    public BaseValueToFilterParser(String value, T filter) {
        matcher = regPattern().matcher(value.trim());
        this.filter = filter;
    }

    protected abstract Pattern regPattern();

    protected abstract Consumer<String> termConsumer();

    @Override
    public boolean matches() {
        return matcher.matches();
    }

    @Override
    public T parse() {
        String group = matcher.group(1);
        if (!group.isBlank()) {
            termConsumer().accept(group);
        }
        return filter;
    }
}
