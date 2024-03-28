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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Utils {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    public static final ObjectWriter PRETTY_PRINTER = OBJECT_MAPPER.writerWithDefaultPrettyPrinter();

    public static <T> Stream<T> asStream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public static boolean isHashKey(String attributeName, TableDescription tableDescription) {
        return tableDescription.keySchema().stream().anyMatch(keySchemaElement -> keySchemaElement.attributeName().equals(attributeName) && keySchemaElement.keyType().equals(KeyType.HASH));
    }

    public static boolean isRangeKey(String attributeName, TableDescription tableDescription) {
        return tableDescription.keySchema().stream().anyMatch(keySchemaElement -> keySchemaElement.attributeName().equals(attributeName) && keySchemaElement.keyType().equals(KeyType.RANGE));
    }

    public static Optional<String> getHashKey(TableDescription tableDescription) {
        return lookUpKeyName(tableDescription.keySchema(), KeyType.HASH);
    }

    public static Optional<String> getRangeKey(TableDescription tableDescription) {
        return lookUpKeyName(tableDescription.keySchema(), KeyType.RANGE);
    }

    public static Optional<String> lookUpKeyName(List<KeySchemaElement> keySchemaElements, KeyType keyType) {
        return keySchemaElements.stream()
                .filter(keySchemaElement -> keySchemaElement.keyType().equals(keyType))
                .map(KeySchemaElement::attributeName)
                .findFirst();
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
            System.out.println(ignored);
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

    public static List<Integer> skipSequences(List<Integer> dataList) {
        return groupBySeq(dataList).stream().map(list -> list.get(0)).collect(Collectors.toList());
    }

    public static List<List<Integer>> groupBySeq(List<Integer> dataList) {
        return dataList.stream().sorted().collect(ArrayList::new,
                (acc, val) -> {
                    if (acc.isEmpty()) {
                        acc.add(new ArrayList<>());
                    }
                    List<Integer> lastGroup = acc.get(acc.size() - 1);
                    if (lastGroup.isEmpty() || val - lastGroup.get(lastGroup.size() - 1) == 1) {
                        lastGroup.add(val);
                    } else {
                        ArrayList<Integer> newGroup = new ArrayList<>();
                        newGroup.add(val);
                        acc.add(newGroup);
                    }
                }, (lists, lists2) -> {
                });
    }

    public static boolean isKeyModifierDown(KeyEvent event) {
        return event.isAltDown() || event.isShiftDown() || event.isControlDown() || event.isMetaDown();
    }

    public static Map<String, AttributeValue> rawJsonToMap(String json) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, new TypeReference<>() {
        });
    }

    public static EnhancedDocument rawJsonToItem(String json) throws JsonProcessingException {
        return EnhancedDocument.fromAttributeValueMap(rawJsonToMap(json));
    }

    public static String convertJsonDocument(String json, boolean toRaw) {
        try {
            if (toRaw) {
                EnhancedDocument item = EnhancedDocument.fromJson(json);
                Map<String, AttributeValue> map = item.toMap();
                return PRETTY_PRINTER.writeValueAsString(map);
            } else {
                EnhancedDocument item = rawJsonToItem(json);
                return item.toJson();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public static void copyToClipboard(String value) {
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard clipboard = Clipboard.getSystemClipboard();
        clipboard.setContent(content);
    }

    public static String uglyToPrettyJson(String json) {
        try {
            Object j = OBJECT_MAPPER.readValue(json, Object.class);
            return PRETTY_PRINTER.writeValueAsString(j);
        } catch (JsonProcessingException e) {
            return json;
        }
    }

}
