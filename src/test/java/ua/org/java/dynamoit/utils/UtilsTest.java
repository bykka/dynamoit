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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonUnmarshaller;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonUnmarshallerContext;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.builder.Buildable;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static ua.org.java.dynamoit.utils.Utils.*;

public class UtilsTest {

    @Test
    public void skipSequences() {
        List<Integer> result = Utils.skipSequences(List.of(2, 5, 6, 7, 10, 11, 12, 15, 16));
        assertEquals(List.of(2, 5, 10, 15), result);
    }

    @Test
    public void groupBySeq() {
        List<List<Integer>> result = Utils.groupBySeq(List.of(2, 5, 6, 7, 10, 11, 12, 15, 16));
        assertEquals(List.of(
                List.of(2),
                List.of(5, 6, 7),
                List.of(10, 11, 12),
                List.of(15, 16)
        ), result);

        result = Utils.groupBySeq(List.of());
        assertEquals(List.of(), result);
    }

    @Test
    public void testJsonRawToPlain() throws UnsupportedEncodingException {
        //language=json
        var rawJson = """
                {
                  "name" : {
                    "S" : "user"
                  }
                }""";

        //language=json
        var plainJson = """
                {
                  "name" : "user"
                }""";

        var result = jsonRawToPlain(rawJson);
        assertEquals(plainJson, result);
    }

    private Map<String, ?> unmarshallMap(JsonUnmarshallerContext context, JsonNode jsonContent) {
        if (jsonContent == null || jsonContent.isNull()) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        jsonContent.asObject().forEach((fieldName, value) -> {
            map.put(fieldName, unmarshall(context, value));
        });
        return map;
    }

    private AttributeValue unmarshall(JsonUnmarshallerContext context, JsonNode jsonContent) {
        var attrVB = AttributeValue.builder();

        for (SdkField<?> field : attrVB.sdkFields()) {
            JsonNode jsonFieldContent = jsonContent.field(field.locationName()).orElse(null);
            JsonUnmarshaller<Object> unmarshaller = context.getUnmarshaller(field.location(), field.marshallingType());
            field.set(attrVB, unmarshaller.unmarshall(context, jsonFieldContent, (SdkField<Object>) field));
        }
        return (AttributeValue) ((Buildable) attrVB).build();
    }

    @Test
    public void testJsonPlainToRaw() {
        //language=json
        var rawJson = """
                {
                  "name" : {
                    "S" : "user"
                  }
                }""";

        //language=json
        var plainJson = """
                {
                    "name" : "user"
                }
                """;

        var result = jsonPlainToRaw(plainJson);

        assertEquals(rawJson, result);
    }

    @ParameterizedTest
    @MethodSource("jsonOfSingleProperty")
    void singlePropertyParser(String rawJson, AttributeValue expectedValue) {
        JsonNode jsonNode = JsonNode.parser().parse(rawJson);
        AttributeValue result = Utils.singlePropertyParser(jsonNode);
        assertEquals(expectedValue, result);
    }

    // data types according to the documentation
    // https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Programming.LowLevelAPI.html#Programming.LowLevelAPI.DataTypeDescriptors
    private static Stream<Arguments> jsonOfSingleProperty() {
        return Stream.of(
                arguments("""
                        {"S": "user"}
                        """, AttributeValue.fromS("user")),
                arguments("""
                        {"N": "1.2"}
                        """, AttributeValue.fromN("1.2")),
                arguments("""
                        {"B": "hello"}
                        """, AttributeValue.fromB(SdkBytes.fromUtf8String("hello"))),
                arguments("""
                        {"BOOL": true}
                        """, AttributeValue.fromBool(true)),
                arguments("""
                        {"NULL": true}
                        """, AttributeValue.fromNul(true)),
                arguments("""
                        {
                            "M": {
                                "content-type": { "S": "application/json" }
                            }
                        }
                        """, AttributeValue.fromM(Map.of("content-type", AttributeValue.fromS("application/json")))),
                arguments("""
                        {
                            "L": [ { "S": "user" } ]
                        }
                        """, AttributeValue.fromL(List.of(AttributeValue.fromS("user")))),
                arguments("""
                        {
                            "SS": [ "user" ]
                        }
                        """, AttributeValue.fromSs(List.of("user"))),
                arguments("""
                        {
                            "BS": [ "user" ]
                        }
                        """, AttributeValue.fromBs(List.of(SdkBytes.fromUtf8String("user"))))
                );
    }


    @Test
    public void testJsonRawParsing() {
        //language=json
        var rawJson = """
                {
                  "name": {
                    "S": "user"
                  },
                  "age": {
                    "N": "20"
                  },
                  "valid": {
                    "BOOL": true
                  },
                  "tags": {
                    "L": [
                      {"S": "tag1"},
                      {"S": "tag2"}
                    ]
                  },
                  "meta": {
                    "M": {
                      "x-tags": {
                        "L": [
                          {"S": "x-tag1"},
                          {"S": "x-tag2"}
                        ]
                      }
                    }
                  }
                 }""";

        var map = Utils.jsonRawParsing(rawJson);
        System.out.println(map);

        assertEquals(5, map.entrySet().size());
        assertEquals("user", map.get("name").s());
        assertEquals("20", map.get("age").n());
        assertEquals(2, map.get("tags").l().size());
    }

}
