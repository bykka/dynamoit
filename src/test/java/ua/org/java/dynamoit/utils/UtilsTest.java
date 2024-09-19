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

import com.amazonaws.services.dynamodbv2.document.Item;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.json.SdkJsonGenerator;
import software.amazon.awssdk.protocols.json.internal.marshall.*;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonUnmarshallerContext;
import software.amazon.awssdk.protocols.json.internal.unmarshall.document.DocumentUnmarshaller;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;

import java.net.URI;
import java.util.List;

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

        System.out.println("document1");
        System.out.println(document1);

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


        JsonMarshallerRegistry.builder()
                .payloadMarshaller(MarshallingType.STRING, SimpleTypeJsonMarshaller.STRING)
                .payloadMarshaller(MarshallingType.INTEGER, SimpleTypeJsonMarshaller.INTEGER)
                .payloadMarshaller(MarshallingType.LONG, SimpleTypeJsonMarshaller.LONG)
                .payloadMarshaller(MarshallingType.SHORT, SimpleTypeJsonMarshaller.SHORT)
                .payloadMarshaller(MarshallingType.DOUBLE, SimpleTypeJsonMarshaller.DOUBLE)
                .payloadMarshaller(MarshallingType.FLOAT, SimpleTypeJsonMarshaller.FLOAT)
                .payloadMarshaller(MarshallingType.BIG_DECIMAL, SimpleTypeJsonMarshaller.BIG_DECIMAL)
                .payloadMarshaller(MarshallingType.BOOLEAN, SimpleTypeJsonMarshaller.BOOLEAN)
                .payloadMarshaller(MarshallingType.INSTANT, SimpleTypeJsonMarshaller.INSTANT)
                .payloadMarshaller(MarshallingType.SDK_BYTES, SimpleTypeJsonMarshaller.SDK_BYTES)
                .payloadMarshaller(MarshallingType.SDK_POJO, SimpleTypeJsonMarshaller.SDK_POJO)
                .payloadMarshaller(MarshallingType.LIST, SimpleTypeJsonMarshaller.LIST)
                .payloadMarshaller(MarshallingType.MAP, SimpleTypeJsonMarshaller.MAP)
                .payloadMarshaller(MarshallingType.NULL, SimpleTypeJsonMarshaller.NULL)
                .payloadMarshaller(MarshallingType.DOCUMENT, SimpleTypeJsonMarshaller.DOCUMENT)
                .headerMarshaller(MarshallingType.STRING, HeaderMarshaller.STRING)
                .headerMarshaller(MarshallingType.INTEGER, HeaderMarshaller.INTEGER)
                .headerMarshaller(MarshallingType.LONG, HeaderMarshaller.LONG)
                .headerMarshaller(MarshallingType.SHORT, HeaderMarshaller.SHORT)
                .headerMarshaller(MarshallingType.DOUBLE, HeaderMarshaller.DOUBLE)
                .headerMarshaller(MarshallingType.FLOAT, HeaderMarshaller.FLOAT)
                .headerMarshaller(MarshallingType.BOOLEAN, HeaderMarshaller.BOOLEAN)
                .headerMarshaller(MarshallingType.INSTANT, HeaderMarshaller.INSTANT)
                .headerMarshaller(MarshallingType.LIST, HeaderMarshaller.LIST)
                .headerMarshaller(MarshallingType.NULL, HeaderMarshaller.NULL)
                .queryParamMarshaller(MarshallingType.STRING, QueryParamMarshaller.STRING)
                .queryParamMarshaller(MarshallingType.INTEGER, QueryParamMarshaller.INTEGER)
                .queryParamMarshaller(MarshallingType.LONG, QueryParamMarshaller.LONG)
                .queryParamMarshaller(MarshallingType.SHORT, QueryParamMarshaller.SHORT)
                .queryParamMarshaller(MarshallingType.DOUBLE, QueryParamMarshaller.DOUBLE)
                .queryParamMarshaller(MarshallingType.FLOAT, QueryParamMarshaller.FLOAT)
                .queryParamMarshaller(MarshallingType.BOOLEAN, QueryParamMarshaller.BOOLEAN)
                .queryParamMarshaller(MarshallingType.INSTANT, QueryParamMarshaller.INSTANT)
                .queryParamMarshaller(MarshallingType.LIST, QueryParamMarshaller.LIST)
                .queryParamMarshaller(MarshallingType.MAP, QueryParamMarshaller.MAP)
                .queryParamMarshaller(MarshallingType.NULL, QueryParamMarshaller.NULL)
                .pathParamMarshaller(MarshallingType.STRING, SimpleTypePathMarshaller.STRING)
                .pathParamMarshaller(MarshallingType.INTEGER, SimpleTypePathMarshaller.INTEGER)
                .pathParamMarshaller(MarshallingType.LONG, SimpleTypePathMarshaller.LONG)
                .pathParamMarshaller(MarshallingType.SHORT, SimpleTypePathMarshaller.SHORT)
                .pathParamMarshaller(MarshallingType.NULL, SimpleTypePathMarshaller.NULL)
                .greedyPathParamMarshaller(MarshallingType.STRING, SimpleTypePathMarshaller.GREEDY_STRING).greedyPathParamMarshaller(MarshallingType.NULL, SimpleTypePathMarshaller.NULL).build();

    }

    @Test
    public void testJsonRawToPlain() {
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

        var result = jsonRawToPlain(rawJson);

//        assertEquals(plainJson, result);


        var jsonNode = JsonNode.parser().parse(rawJson);
        var doc = jsonNode.visit(new DocumentUnmarshaller());

        System.out.println(doc.getClass().getName());
    }

    // com.amazonaws.services.dynamodbv2.model.transform.GetItemResultJsonUnmarshaller
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

        var item = Item.fromJSON(plainJson);
        System.out.println(item.asMap());

        var result = jsonPlainToRaw(plainJson);

        assertEquals(rawJson, result);
    }

}
