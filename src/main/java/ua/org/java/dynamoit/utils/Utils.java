package ua.org.java.dynamoit.utils;

import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

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

}
