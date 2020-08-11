/*
 * This file is part of DynamoIt.
 *
 *     DynamoIt is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Foobar is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with DynamoIt.  If not, see <https://www.gnu.org/licenses/>.
 */

package ua.org.java.dynamoit.components.tablegrid;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.internal.Filter;
import javafx.util.Pair;
import ua.org.java.dynamoit.utils.Utils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Attributes {

    public static final String ASTERISK = "*";

    public enum Type {
        STRING, NUMBER, BOOLEAN
    }

    private Attributes() {
    }


    public static Map<String, Attributes.Type> defineAttributesTypes(List<Item> itemList) {
        Function<Object, Attributes.Type> mapper = value -> {
            if (value != null) {
                if (value instanceof Boolean) {
                    return Attributes.Type.BOOLEAN;
                }
                if (value instanceof Number) {
                    return Attributes.Type.NUMBER;
                }
            }
            return Attributes.Type.STRING;
        };

        return itemList.stream()
                .flatMap(item ->
                        Utils.asStream(item.attributes())
                                .map(entry -> new Pair<>(entry.getKey(), mapper.apply(entry.getValue()))))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue, (attributeType, attributeType2) -> attributeType));
    }

    public static Type fromDynamoDBType(String dynamoDBType) {
        switch (dynamoDBType) {
            case "N": return Type.NUMBER;
            case "BOOL": return Type.BOOLEAN;
        }
        return Attributes.Type.STRING;
    }

    public static <T extends Filter<T>> T attributeValueToFilter(String attribute, String value, Type type, Function<String, T> filterProvider) {
        T filter = filterProvider.apply(attribute);

        if (type == Attributes.Type.NUMBER) {
            filter.eq(new BigDecimal(value));
        } else if (type == Attributes.Type.BOOLEAN) {
            filter.eq(Boolean.valueOf(value));
        } else {
            if (value.startsWith(ASTERISK) && value.endsWith(ASTERISK)) {
                filter.contains(value);
            } else if (value.endsWith(ASTERISK)) {
                filter.beginsWith(value.substring(0, value.length() - 1));
            } else {
                filter.eq(value);
            }
        }
        return filter;
    }

}
