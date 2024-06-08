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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.internal.conditional.EqualToConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static ua.org.java.dynamoit.utils.Utils.attributeName;
import static ua.org.java.dynamoit.utils.Utils.attributeValue;

public class BuildQuerySpecTest {

    private Map<String, String> attributeFilterMap;

    @BeforeEach
    public void setUp() {
        attributeFilterMap = new HashMap<>();
        attributeFilterMap.put("hashKey", "hashValue");
        attributeFilterMap.put("rangeKey", "rangeValue");
        attributeFilterMap.put("attribute1", "value1");
        attributeFilterMap.put("attribute2", "value2");
    }

    @Test
    public void testBuildQuerySpecWithRangeKey() {
        QueryEnhancedRequest request = Utils.buildQuerySpec("hashKey", "rangeKey", attributeFilterMap, 10);

        assertNotNull(request);
        assertEquals(10, request.limit());

        QueryConditional queryConditional = request.queryConditional();
        assertInstanceOf(EqualToConditional.class, queryConditional);
        assertEquals(new EqualToConditional(Key.builder()
                .partitionValue("hashValue")
                .sortValue("rangeValue")
                .build()), queryConditional);


        assertNotNull(request.filterExpression());
        assertEquals(2, request.filterExpression().expressionNames().size());
        assertEquals(2, request.filterExpression().expressionValues().size());

        assertEquals("attribute1", request.filterExpression().expressionNames().get(attributeName("attribute1")));
        assertEquals("attribute2", request.filterExpression().expressionNames().get(attributeName("attribute2")));
        assertEquals("value1", request.filterExpression().expressionValues().get(attributeValue("attribute1")).s());
        assertEquals("value2", request.filterExpression().expressionValues().get(attributeValue("attribute2")).s());
        assertEquals(String.format("(%1$s = %2$s) AND (%3$s = %4$s)", attributeName("attribute1"), attributeValue("attribute1"), attributeName("attribute2"), attributeValue("attribute2")), request.filterExpression().expression());
    }

//    @Test
//    public void testBuildQuerySpecWithoutRangeKey() {
//        QueryEnhancedRequest request = QuerySpecBuilder.buildQuerySpec("hashKey", null, attributeFilterMap, 5);
//
//        assertNotNull(request);
//        assertEquals(5, request.limit());
//
//        Key key = request.queryConditional().keyEqualTo();
//        assertEquals("hashValue", key.partitionValue().s());
//        assertNull(key.sortValue());
//
//        assertNotNull(request.filterExpression());
//        assertTrue(request.filterExpression().expressionValues().containsKey(":attribute1"));
//        assertTrue(request.filterExpression().expressionValues().containsKey(":attribute2"));
//    }
//
//    @Test
//    public void testBuildQuerySpecWithEmptyFilterMap() {
//        Map<String, String> emptyFilterMap = new HashMap<>();
//        emptyFilterMap.put("hashKey", "hashValue");
//
//        QueryEnhancedRequest request = QuerySpecBuilder.buildQuerySpec("hashKey", "rangeKey", emptyFilterMap, 3);
//
//        assertNotNull(request);
//        assertEquals(3, request.limit());
//
//        Key key = request.queryConditional().keyEqualTo();
//        assertEquals("hashValue", key.partitionValue().s());
//        assertNull(key.sortValue());
//
//        assertNull(request.filterExpression());
//    }
//
//    @Test
//    public void testBuildQuerySpecWithMissingPartitionKey() {
//        Map<String, String> invalidFilterMap = new HashMap<>();
//        invalidFilterMap.put("rangeKey", "rangeValue");
//
//        assertThrows(IllegalArgumentException.class, () ->
//                QuerySpecBuilder.buildQuerySpec("hashKey", "rangeKey", invalidFilterMap, 3)
//        );
//    }

}
