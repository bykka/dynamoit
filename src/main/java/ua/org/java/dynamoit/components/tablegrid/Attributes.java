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

package ua.org.java.dynamoit.components.tablegrid;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.internal.Filter;
import javafx.util.Pair;
import ua.org.java.dynamoit.components.tablegrid.parser.BaseValueToFilterParser;
import ua.org.java.dynamoit.components.tablegrid.parser.BeginsWithParser;
import ua.org.java.dynamoit.components.tablegrid.parser.ContainsParser;
import ua.org.java.dynamoit.components.tablegrid.parser.EqualsParser;
import ua.org.java.dynamoit.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            case "N":
                return Type.NUMBER;
            case "BOOL":
                return Type.BOOLEAN;
        }
        return Attributes.Type.STRING;
    }

    public static <T extends Filter<T>> T attributeValueToFilter(String attribute, String value, Type type, Function<String, T> filterProvider) {
        T filter = filterProvider.apply(attribute);

        if (value == null || value.isBlank()) {
            return filter;
        }

        Stream.of(
                new ContainsParser<>(value, filter),
                new BeginsWithParser<>(value, filter),
                new EqualsParser<>(value, type, filter) // last parser
        )
                .filter(BaseValueToFilterParser::matches)
                .findFirst()
                .map(BaseValueToFilterParser::parse);

        return filter;
    }

}
