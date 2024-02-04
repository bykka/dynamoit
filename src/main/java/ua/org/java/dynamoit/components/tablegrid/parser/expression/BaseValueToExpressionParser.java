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
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * Matches regular expression and build a new dynamodb filter expression if yes
 */
public abstract class BaseValueToExpressionParser implements ValueToExpressionParser {

    private static final CRC32 CRC_32 = new CRC32();

    protected final String attribute;
    private final Matcher matcher;

    public BaseValueToExpressionParser(String attribute, String value) {
        this.attribute = attribute;
        matcher = regPattern().matcher(value.trim());
    }

    protected abstract Pattern regPattern();

    protected abstract String buildExpression(String attrName, String attrValue);

    protected static synchronized String hashAttribute(String attribute) {
        CRC_32.update(attribute.getBytes());
        return String.valueOf(CRC_32.getValue());
    }

    protected static String attributeName(String attribute) {
        return "#attr_" + hashAttribute(attribute);
    }

    protected static String attributeValue(String attribute) {
        return ":val_" + hashAttribute(attribute);
    }

    @Override
    public boolean matches() {
        return matcher.matches();
    }

    @Override
    public Optional<Expression> parse() {
        String term = matcher.group(1);
        if (!term.isBlank()) {
            String attrName = attributeName(attribute);
            String attrValue = attributeValue(attribute);

            Expression expression = Expression.builder()
                    .expression(buildExpression(attrName, attrValue).get())
                    .expressionNames(Map.of(attrName, attribute))
                    .expressionValues(Map.of(attrValue, AttributeValue.builder().s(term).build()))
                    .build();

            return Optional.of(expression);
        }
        return Optional.empty();
    }
}
