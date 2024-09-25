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
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonUnmarshaller;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonUnmarshallerContext;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.builder.Buildable;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    public void testConvertJsonToDocument() {

        //language=json
        String document1 = convertJsonDocument("""
                {"name": "user"}
                """, true);

        //language=json
        assertEquals("""
                {
                  "name" : {
                    "S" : "user"
                  }
                }""", document1.replace("\r", ""));

        //language=json
        String document2 = convertJsonDocument("""
                {
                    "name": {
                        "S": "user"
                    }
                }
                """, false);

        //language=json
        assertEquals("""
                {
                    "name" : "user"
                }
                """, document2);
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

        assertEquals(4, map.entrySet().size());
        assertEquals("user", map.get("name").s());
        assertEquals("20", map.get("age").n());
        assertEquals(2, map.get("tags").l().size());
    }

}
