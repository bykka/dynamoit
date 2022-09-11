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

public enum HighlightColors {

    COLOR_CHART_1("1"),
    COLOR_CHART_2("2"),
    COLOR_CHART_3("3"),
    COLOR_CHART_4("4"),
    COLOR_CHART_5("5"),
    COLOR_CHART_6("6"),
    COLOR_CHART_7("7"),
    COLOR_CHART_8("8");

    private final String cssClassName;

    HighlightColors(String cssClassName) {
        this.cssClassName = cssClassName;
    }

    public String toggleButtonClass() {
        return "toggle-button-" + this.cssClassName;
    }

    public String tabClass() {
        return "tab-" + this.cssClassName;
    }

}
