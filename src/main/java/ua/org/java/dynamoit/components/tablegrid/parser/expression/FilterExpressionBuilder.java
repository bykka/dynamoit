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

import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

public class FilterExpressionBuilder {

    private enum AttributeExpression {

        BEGINS_WITH(Pattern.compile("^\\^(.*)$"), (attrName, attrValue) -> "begins_with(" + attrName + ", " + attrValue + ")"),
        CONTAINS(Pattern.compile("^~(.*)$"), (attrName, attrValue) -> "contains(" + attrName + ", " + attrValue + ")"),
        NOT_CONTAINS(Pattern.compile("(^\\$$)"), (attrName, attrValue) -> "NOT contains(" + attrName + ", " + attrValue + ")"),
        EXISTS(Pattern.compile("(^\\$$)"), (attrName, attrValue) -> "attribute_exists(" + attrName + ")"),
        NOT_EXISTS(Pattern.compile("(^!\\$$)"), (attrName, attrValue) -> "attribute_not_exists(" + attrName + ")"),
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

    private static final CRC32 CRC_32 = new CRC32();

    private Expression expression = Expression.builder().build();

    public FilterExpressionBuilder addAttributeValue(String attribute, String value) {
        String attrName = attributeName(attribute);
        String attrValue = attributeValue(attribute);

        for (AttributeExpression attributeExpression : AttributeExpression.values()) {
            Matcher matcher = attributeExpression.getPattern().matcher(value.trim());
            if (matcher.matches()) {
                String term = matcher.group(1);
                if (!term.isBlank()) {
                    Expression exp = Expression.builder()
                            .expression(attributeExpression.getAttributeValueToExpression().apply(attrName, attrValue))
                            .expressionNames(Map.of(attrName, attribute))
                            .expressionValues(Map.of(attrValue, AttributeValue.builder().s(term).build()))
                            .build();
                    expression = Expression.join(expression, exp, "AND");
                }
            }
        }
        return this;
    }

    public Expression build() {
        return expression;
    }

    private static synchronized String hashValue(String value) {
        CRC_32.update(value.getBytes());
        return String.valueOf(CRC_32.getValue());
    }

    private static String attributeName(String attribute) {
        return "#attr_" + hashValue(attribute);
    }

    private static String attributeValue(String attribute) {
        return ":val_" + hashValue(attribute);
    }


}
