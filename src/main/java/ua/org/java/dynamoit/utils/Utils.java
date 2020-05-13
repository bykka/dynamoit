package ua.org.java.dynamoit.utils;

import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
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


    public static <T> Comparator<T> KEYS_FIRST(String hashKeyName, String rangeKeyName, Function<T, String> convert) {
        return (o1, o2) -> {
            String v1 = convert.apply(o1);
            String v2 = convert.apply(o2);

            if (v1.equals(hashKeyName)) {
                return -1;
            }
            if (v2.equals(hashKeyName)) {
                return 1;
            }
            if (v1.equals(rangeKeyName)) {
                return -1;
            }
            if (v2.equals(rangeKeyName)) {
                return 1;
            }
            return v1.compareTo(v2);
        };
    }
    public static Comparator<String> KEYS_FIRST(String hashKeyName, String rangeKeyName) {
        return KEYS_FIRST(hashKeyName, rangeKeyName, s -> s);
    }

    public static <T> T log(T t){
        System.out.println(t);
        return t;
    }
}
