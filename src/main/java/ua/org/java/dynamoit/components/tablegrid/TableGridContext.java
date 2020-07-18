/*
 * This file is part of DynamoIt.
 *
 *     DynamoIt is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Foobar is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with DynamoIt.  If not, see <https://www.gnu.org/licenses/>.
 */

package ua.org.java.dynamoit.components.tablegrid;

public class TableGridContext {

    private final String profileName;
    private final String tableName;
    private String propertyName;
    private String propertyValue;

    public TableGridContext(String profileName, String tableName) {
        this.profileName = profileName;
        this.tableName = tableName;
    }

    public TableGridContext(String profileName, String tableName, String propertyName, String propertyValue) {
        this.profileName = profileName;
        this.tableName = tableName;
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    public String getProfileName() {
        return profileName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    @Override
    public String toString() {
        return "TableContext{" +
                "profileName='" + profileName + '\'' +
                ", tableName='" + tableName + '\'' +
                ", propertyName='" + propertyName + '\'' +
                ", propertyValue='" + propertyValue + '\'' +
                '}';
    }
}
