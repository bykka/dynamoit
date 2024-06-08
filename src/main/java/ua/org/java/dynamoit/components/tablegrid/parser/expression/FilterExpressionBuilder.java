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

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ua.org.java.dynamoit.utils.Utils.attributeName;
import static ua.org.java.dynamoit.utils.Utils.attributeValue;

public class FilterExpressionBuilder {

    private enum AttributeExpression {

        BEGINS_WITH(Pattern.compile("^\\^(.*)$"), (attrName, attrValue) -> "begins_with(" + attrName + ", " + attrValue + ")"),
        CONTAINS(Pattern.compile("^~(.*)$"), (attrName, attrValue) -> "contains(" + attrName + ", " + attrValue + ")"),
        NOT_CONTAINS(Pattern.compile("^!~(.*)$"), (attrName, attrValue) -> "NOT contains(" + attrName + ", " + attrValue + ")"),
        EXISTS(Pattern.compile("^\\$$"), (attrName, attrValue) -> "attribute_exists(" + attrName + ")"),
        NOT_EXISTS(Pattern.compile("^!\\$$"), (attrName, attrValue) -> "attribute_not_exists(" + attrName + ")"),
        NOT_EQUALS(Pattern.compile("^!=(.*)$"), (attrName, attrValue) -> attrName + " <> " + attrValue),
        EQUALS(Pattern.compile("(.*)"), (attrName, attrValue) -> attrName + " = " + attrValue);

        AttributeExpression(Pattern pattern, BiFunction<String, String, String> attributeValueToExpression) {
            this.pattern = pattern;
            this.attributeValueToExpression = attributeValueToExpression;
        }

        private final Pattern pattern;
        private final BiFunction<String, String, String> attributeValueToExpression;

        public Pattern getPattern() {
            return pattern;
        }

        public BiFunction<String, String, String> getAttributeValueToExpression() {
            return attributeValueToExpression;
        }
    }

    private Expression expression = Expression.builder().build();

    public static boolean isEqualExpression(String value) {
        if (value != null && !value.isBlank()) {
            for (AttributeExpression attributeExpression : AttributeExpression.values()) {
                Matcher matcher = attributeExpression.getPattern().matcher(value.trim());
                if (matcher.matches()) {
                    return attributeExpression == AttributeExpression.EQUALS;
                }
            }

        }
        return false;
    }

    public FilterExpressionBuilder addAttributeValue(String attribute, String value) {
        if (value != null && !value.isBlank()) {

            String attrName = attributeName(attribute);
            String attrValue = attributeValue(attribute);

            for (AttributeExpression attributeExpression : AttributeExpression.values()) {
                Matcher matcher = attributeExpression.getPattern().matcher(value.trim());
                if (matcher.matches()) {
                    Expression.Builder expBuilder = Expression.builder();

                    // the first group is whole value
                    if(matcher.groupCount() == 1) {
                        String term = matcher.group(1);
                        if (!term.isBlank()) {
                            expBuilder.expression(attributeExpression.getAttributeValueToExpression().apply(attrName, attrValue))
                                    .expressionNames(Map.of(attrName, attribute))
                                    .expressionValues(Map.of(attrValue, AttributeValue.builder().s(term).build())).build();
                        }
                    } else {
                        expBuilder.expression(attributeExpression.getAttributeValueToExpression().apply(attrName, attrValue))
                                .expressionNames(Map.of(attrName, attribute));
                    }

                    if (StringUtils.isBlank(expression.expression())) {
                        expression = expBuilder.build();
                    } else {
                        expression = Expression.join(expression, expBuilder.build(), " AND ");
                    }

                    break;
                }
            }
        }
        return this;
    }

    public Expression build() {
        return expression;
    }

}
