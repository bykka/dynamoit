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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.protocols.json.SdkJsonGenerator;
import software.amazon.awssdk.protocols.json.internal.marshall.JsonMarshallerContext;
import software.amazon.awssdk.protocols.json.internal.marshall.JsonProtocolMarshaller;
import software.amazon.awssdk.protocols.json.internal.marshall.JsonProtocolMarshallerBuilder;
import software.amazon.awssdk.protocols.json.internal.marshall.SimpleTypeJsonMarshaller;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;
import software.amazon.awssdk.utils.StringUtils;
import ua.org.java.dynamoit.components.tablegrid.parser.expression.FilterExpressionBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Utils {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static final ObjectWriter PRETTY_PRINTER = OBJECT_MAPPER
            .writerWithDefaultPrettyPrinter();

    private static final ObjectMapper PROPERTIES_MAPPER = new ObjectMapper()
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    public static <T> Stream<T> asStream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public static boolean isHashKey(String attributeName, TableDescription tableDescription) {
        return tableDescription.keySchema().stream()
                .anyMatch(keySchemaElement -> keySchemaElement.attributeName().equals(attributeName)
                        && keySchemaElement.keyType().equals(KeyType.HASH));
    }

    public static boolean isRangeKey(String attributeName, TableDescription tableDescription) {
        return tableDescription.keySchema().stream()
                .anyMatch(keySchemaElement -> keySchemaElement.attributeName().equals(attributeName)
                        && keySchemaElement.keyType().equals(KeyType.RANGE));
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
            return PROPERTIES_MAPPER.writeValueAsString(value);
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
        return groupBySeq(dataList).stream().map(List::getFirst).collect(Collectors.toList());
    }

    public static List<List<Integer>> groupBySeq(List<Integer> dataList) {
        return dataList.stream().sorted().collect(ArrayList::new,
                (acc, val) -> {
                    if (acc.isEmpty()) {
                        acc.add(new ArrayList<>());
                    }
                    List<Integer> lastGroup = acc.getLast();
                    if (lastGroup.isEmpty() || val - lastGroup.getLast() == 1) {
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

    static AttributeValue singlePropertyParser(JsonNode jsonNode) {
        AttributeValue.Builder builder = AttributeValue.builder();
        for (SdkField<?> sdkField: builder.sdkFields()) {
            var fieldOptional = jsonNode.field(sdkField.locationName());
            fieldOptional.flatMap(fieldValue -> {
                if (sdkField.marshallingType() == MarshallingType.STRING) {
                    return Optional.of(fieldValue.asString());
                }
                if (sdkField.marshallingType() == MarshallingType.BOOLEAN) {
                    return Optional.of(fieldValue.asBoolean());
                }
                if (sdkField.marshallingType() == MarshallingType.SDK_BYTES) {
                    return Optional.of(SdkBytes.fromString(fieldValue.asString(), StandardCharsets.UTF_8));
                }
                if (sdkField.marshallingType() == MarshallingType.NULL) {
                    return Optional.of(fieldValue.isNull());
                }
                if (sdkField.marshallingType() == MarshallingType.MAP && fieldValue.isObject()) {
                    return Optional.of(mapPropertyParser(fieldValue));
                }
                if (sdkField.marshallingType() == MarshallingType.LIST && fieldValue.isArray()) {
                    Function<JsonNode, Object> valueConvertor = switch (sdkField.locationName()) {
                        case "SS" -> JsonNode::asString;
                        case "NS" -> JsonNode::asNumber;
                        case "BS" -> json -> SdkBytes.fromUtf8String(json.asString());
                        default -> Utils::singlePropertyParser;
                    };
                    return Optional.of(fieldValue.asArray().stream().map(valueConvertor).toList());
                }
                if (sdkField.marshallingType() == MarshallingType.LIST && fieldValue.isArray()) {
                    return Optional.of(fieldValue.asArray().stream().map(Utils::singlePropertyParser).toList());
                }
                return Optional.empty();
            }).ifPresent(value -> sdkField.set(builder, value));
        }

        return builder.build();
    }

    static Map<String, AttributeValue> mapPropertyParser(JsonNode jsonNode) {
        return jsonNode.asObject()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> singlePropertyParser(entry.getValue())
                ));
    }

    static Map<String, AttributeValue> jsonRawParsing(String rawJson) {
        JsonNode jsonRoot = JsonNode.parser().parse(rawJson);

        return mapPropertyParser(jsonRoot);
    }

    /**
     * Convert raw dynamodb document to plain presentation
     * from {"name": {"S": "Mr. Smith"}} to {"name": "Mr. Smith"}
     *
     * @param json raw json
     * @return plain json
     */
    public static String jsonRawToPlain(String json) {
        EnhancedDocument document = EnhancedDocument.fromAttributeValueMap(jsonRawParsing(json));
        var plainJson = document.toJson();

        try {
            Map<String, Object> toMap = OBJECT_MAPPER.readValue(plainJson, new TypeReference<>() {
            });
            return PRETTY_PRINTER.writeValueAsString(toMap);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return plainJson;
    }

    public static String jsonPlainToRaw(String json) {
        EnhancedDocument item = EnhancedDocument.fromJson(json);
        Map<String, AttributeValue> map = item.toMap();

        return attributeValueMapToJson(map);
    }

    public static String attributeValueMapToJson(Map<String, AttributeValue> map) {
        try {
            SdkJsonGenerator jsonGenerator = new SdkJsonGenerator(new JsonFactory(), "application/json");
            var context = JsonMarshallerContext
                    .builder()
                    .jsonGenerator(jsonGenerator)
                    .protocolHandler((JsonProtocolMarshaller) JsonProtocolMarshallerBuilder
                            .create()
                            .protocolMetadata(AwsJsonProtocolMetadata.builder().build())
                            .endpoint(new URI("https://fake.com"))
                            .jsonGenerator(jsonGenerator)
                            .sendExplicitNullForPayload(true)
                            .operationInfo(OperationInfo.builder().build())
                            .build())
                    .build();

            jsonGenerator.writeStartObject();

            map.forEach((s, attributeValue) -> {
                SimpleTypeJsonMarshaller.SDK_POJO.marshall(attributeValue, context, s, null);
            });

            jsonGenerator.writeEndObject();

            String rawJson = new String(jsonGenerator.getBytes());
            Map<String, Object> toMap = OBJECT_MAPPER.readValue(rawJson, new TypeReference<>() {
            });

            return PRETTY_PRINTER.writeValueAsString(toMap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    /**
     * Builds a {@link QueryEnhancedRequest} with specified attributes for querying
     * a DynamoDB table.
     *
     * @param hashName           The name of the partition key attribute.
     * @param rangeName          The name of the sort key attribute. Can be null if
     *                           the table doesn't use a sort key.
     * @param attributeFilterMap A map containing attribute names and their
     *                           corresponding filter values.
     * @param numberOfDocuments  The maximum number of documents to be returned by
     *                           the query.
     * @return A {@link QueryEnhancedRequest} object configured with the specified
     * attributes and filter conditions.
     * @throws IllegalArgumentException if the partition key value is not present in
     *                                  the attribute filter map.
     */
    public static QueryEnhancedRequest buildQuerySpec(String hashName, String rangeName,
                                                      Map<String, String> attributeFilterMap, int numberOfDocuments) {
        String hashValue = attributeFilterMap.get(hashName);
        Key.Builder keyBuilder = Key.builder().partitionValue(hashValue);

        if (rangeName != null && StringUtils.isNotBlank(attributeFilterMap.get(rangeName))) {
            String sortValue = attributeFilterMap.get(rangeName);
            keyBuilder.sortValue(sortValue);
        }

        var attributesWithoutKeys = attributeFilterMap.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(hashName) && !entry.getKey().equals(rangeName))
                .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
                .toList();

        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();

        attributesWithoutKeys
                .forEach(entry -> filterExpressionBuilder.addAttributeValue(entry.getKey(), entry.getValue()));

        QueryEnhancedRequest.Builder querySpec = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(keyBuilder.build()))
                .filterExpression(filterExpressionBuilder.build());

        return querySpec.limit(numberOfDocuments).build();
    }

    private static synchronized String hashValue(String value) {
        return String.valueOf(Math.abs(value.hashCode()));
    }

    public static String attributeName(String attribute) {
        return "#attr_" + hashValue(attribute);
    }

    public static String attributeValue(String attribute) {
        return ":val_" + hashValue(attribute);
    }

}
