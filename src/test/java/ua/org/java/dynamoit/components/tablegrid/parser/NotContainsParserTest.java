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

package ua.org.java.dynamoit.components.tablegrid.parser;

import com.amazonaws.services.dynamodbv2.document.QueryFilter;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import org.junit.Test;

import static org.junit.Assert.*;

public class NotContainsParserTest {

    @Test
    public void testBlank() {
        assertFalse(new NotContainsParser<QueryFilter>("", null).matches());
        assertFalse(new NotContainsParser<QueryFilter>(" ", null).matches());
        assertFalse(new NotContainsParser<QueryFilter>("~", null).matches());
        assertTrue(new NotContainsParser<QueryFilter>("!~", null).matches());
    }

    @Test
    public void testValue() {
        QueryFilter filter = new QueryFilter("attr");
        NotContainsParser<QueryFilter> parser = new NotContainsParser<>("!~hello", filter);
        assertTrue(parser.matches());

        parser.parse();

        assertEquals(ComparisonOperator.NOT_CONTAINS, filter.getComparisonOperator());
        assertArrayEquals(new Object[]{"hello"}, filter.getValues());
    }

}
