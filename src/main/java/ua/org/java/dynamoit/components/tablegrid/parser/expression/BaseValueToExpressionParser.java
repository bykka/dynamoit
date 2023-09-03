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

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Matches regular expression and build a new dynamodb filter expression if yes
 */
public abstract class BaseValueToExpressionParser implements ValueToExpressionParser {

    protected final String attribute;
    private final Matcher matcher;

    public BaseValueToExpressionParser(String attribute, String value) {
        this.attribute = attribute;
        matcher = regPattern().matcher(value.trim());
    }

    protected abstract Pattern regPattern();

    protected abstract Function<String, Expression> termConverter();

    @Override
    public boolean matches() {
        return matcher.matches();
    }

    @Override
    public Optional<Expression> parse() {
        String group = matcher.group(1);
        if (!group.isBlank()) {
            return Optional.of(termConverter().apply(group));
        }
        return Optional.empty();
    }
}
