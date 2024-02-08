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

package ua.org.java.dynamoit.components.tablegrid.parser.expression;

import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.Expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FilterExpressionBuilderTest {

    @Test
    public void TestBeginWith(){
        FilterExpressionBuilder expressionBuilder = new FilterExpressionBuilder();
        expressionBuilder.addAttributeValue("greetings", "^hello");
        Expression expression = expressionBuilder.build();
        assertNotNull(expression);
        assertEquals(expression.expression(), "begins_with()");
    }



}
