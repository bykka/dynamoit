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
import ua.org.java.dynamoit.components.tablegrid.Attributes;

import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class NotEqualsParser<T extends Filter<T>> extends BaseValueToFilterParser<T> {

    private static final Pattern PATTERN = Pattern.compile("^!=(.*)$");
    private final Attributes.Type type;

    public NotEqualsParser(String value, Attributes.Type type, T filter) {
        super(value, filter);
        this.type = type;
    }

    @Override
    protected Pattern regPattern() {
        return PATTERN;
    }

    @Override
    protected Consumer<String> termConsumer() {
        return term -> {
            try {
                if (type == Attributes.Type.NUMBER) {
                    filter.ne(new BigDecimal(term));
                } else if (type == Attributes.Type.BOOLEAN) {
                    filter.ne(Boolean.valueOf(term));
                } else {
                    filter.ne(term);
                }
            } catch (Exception e) {
                e.printStackTrace();
                filter.ne(term);
            }
        };
    }
}
