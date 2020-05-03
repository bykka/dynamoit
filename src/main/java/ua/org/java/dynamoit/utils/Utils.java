package ua.org.java.dynamoit.utils;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import javafx.util.Pair;
import ua.org.java.dynamoit.components.tablegrid.TableGridModel;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Utils {

    public static <T> Stream<T> asStream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public static boolean isHashKey(String attributeName, DescribeTableResult describeTableResult) {
        TableDescription tableDescription = describeTableResult.getTable();
        return tableDescription.getKeySchema().stream().anyMatch(keySchemaElement -> keySchemaElement.getAttributeName().equals(attributeName) && keySchemaElement.getKeyType().equals("HASH"));
    }

    public static boolean isRangeKey(String attributeName, DescribeTableResult describeTableResult) {
        TableDescription tableDescription = describeTableResult.getTable();
        return tableDescription.getKeySchema().stream().anyMatch(keySchemaElement -> keySchemaElement.getAttributeName().equals(attributeName) && keySchemaElement.getKeyType().equals("RANGE"));
    }

    public static Optional<String> getHashKey(DescribeTableResult describeTableResult) {
        TableDescription tableDescription = describeTableResult.getTable();
        return tableDescription.getKeySchema().stream().filter(keySchemaElement -> keySchemaElement.getKeyType().equals("HASH")).map(KeySchemaElement::getAttributeName).findFirst();
    }

    public static Optional<String> getRangeKey(DescribeTableResult describeTableResult) {
        TableDescription tableDescription = describeTableResult.getTable();
        return tableDescription.getKeySchema().stream().filter(keySchemaElement -> keySchemaElement.getKeyType().equals("RANGE")).map(KeySchemaElement::getAttributeName).findFirst();
    }


    public static BiFunction<String, String, Comparator<String>> KEYS_FIRST = (String hashKeyName, String rangeKeyName) -> (o1, o2) -> {
        if (o1.equals(hashKeyName)) {
            return -1;
        }
        if (o2.equals(hashKeyName)) {
            return 1;
        }
        if (o1.equals(rangeKeyName)) {
            return -1;
        }
        if (o2.equals(rangeKeyName)) {
            return 1;
        }
        return o1.compareTo(o2);
    };

    public static Map<String, TableGridModel.AttributeType> getAttributesTypes(List<Item> itemList) {
        Function<Object, TableGridModel.AttributeType> mapper = value -> {
            if (value != null) {
                if (value instanceof Boolean) {
                    return TableGridModel.AttributeType.BOOLEAN;
                }
                if (value instanceof Number) {
                    return TableGridModel.AttributeType.NUMBER;
                }
            }
            return TableGridModel.AttributeType.STRING;
        };

        return itemList.stream()
                .flatMap(item ->
                        asStream(item.attributes())
                        .map(entry -> new Pair<>(entry.getKey(), mapper.apply(entry.getValue()))))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue, (attributeType, attributeType2) -> attributeType));
    }

}
