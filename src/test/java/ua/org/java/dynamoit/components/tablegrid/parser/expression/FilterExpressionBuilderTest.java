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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class FilterExpressionBuilderTest {

    @ParameterizedTest
    @MethodSource("singleValidExpressionArguments")
    public void TestSingleExpression(String filter, String expressionValue) {
        FilterExpressionBuilder expressionBuilder = new FilterExpressionBuilder();
        expressionBuilder.addAttributeValue("greetings", filter);
        Expression expression = expressionBuilder.build();
        assertNotNull(expression);
        assertEquals(expression.expression(), expressionValue);
        assertEquals(expression.expressionNames().size(), 1);
        assertEquals(expression.expressionNames().get("#attr_2073134938"), "greetings");
        assertEquals(expression.expressionValues().size(), 1);
        assertEquals(expression.expressionValues().get(":val_2073134938"), AttributeValue.builder().s("hello").build());
    }

    static Stream<Arguments> singleValidExpressionArguments() {
        return Stream.of(
                Arguments.arguments("^hello", "begins_with(#attr_2073134938, :val_2073134938)"),
                Arguments.arguments("~hello", "contains(#attr_2073134938, :val_2073134938)"),
                Arguments.arguments("!~hello", "NOT contains(#attr_2073134938, :val_2073134938)"),
                Arguments.arguments("hello", "#attr_2073134938 = :val_2073134938"),
                Arguments.arguments("!=hello", "#attr_2073134938 <> :val_2073134938")
        );
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = {"  ", "^", "^ ", " ^ ", "~", "~ ", " ~ ", "!~", "!~ ", " !~ ", "!=", "!= ", " != "})
    public void TestBlankValueExpression(String filter) {
        FilterExpressionBuilder expressionBuilder = new FilterExpressionBuilder();
        expressionBuilder.addAttributeValue("greetings", filter);
        Expression expression = expressionBuilder.build();
        assertNotNull(expression);
        assertNull(expression.expression());
        assertNull(expression.expressionNames());
        assertNull(expression.expressionValues());
    }


}
