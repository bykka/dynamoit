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

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static ua.org.java.dynamoit.utils.Utils.convertJsonDocument;

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
        String newLine = System.getProperty("line.separator");

        String document1 = convertJsonDocument("{\"name\": \"user\"}", true);
        assertEquals(String.join(newLine,
                "{",
                "  \"name\" : {",
                "    \"s\" : \"user\"",
                "  }",
                "}"), document1);

        String document2 = convertJsonDocument("{\"name\": {\"s\": \"user\"}}", false);
        assertEquals(String.join(newLine,
                "{",
                "  \"name\" : \"user\"",
                "}"), document2);
    }

}
