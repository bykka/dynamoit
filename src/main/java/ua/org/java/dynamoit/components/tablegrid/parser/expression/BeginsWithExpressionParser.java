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
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

public class BeginsWithExpressionParser extends BaseValueToExpressionParser {

    private static final Pattern PATTERN = Pattern.compile("^\\^(.*)$");

    public BeginsWithExpressionParser(String attribute, String value) {
        super(attribute, value);
    }

    @Override
    protected Pattern regPattern() {
        return PATTERN;
    }

    @Override
    protected Function<String, Expression> termConverter() {
        CRC32 crc32 = new CRC32();
        crc32.update(attribute.getBytes());
        long attributeHash = crc32.getValue();

        String attributeName = "#attr_" + attributeHash;
        String attributeValue = ":val_" + attributeHash;

        return term -> Expression.builder()
                .expression("begins_with(" + attributeName + ", " + attributeValue + ")")
                .expressionNames(Map.of(attributeName, attribute))
                .expressionValues(Map.of(attributeValue, AttributeValue.builder().s(term).build()))
                .build();
    }
}
