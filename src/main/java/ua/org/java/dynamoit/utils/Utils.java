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

import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Utils {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

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

    public static String logAsJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ignored) {
        }
        return String.valueOf(value);
    }

    public static String trimToBlank(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }

    public static String truncateWithDots(String value) {
        if (value.length() > 40) {
            return value.substring(0, 40) + "..";
        }
        return value;
    }
}
