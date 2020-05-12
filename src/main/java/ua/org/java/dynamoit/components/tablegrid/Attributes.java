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
