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

package ua.org.java.dynamoit.model.profile;

import java.util.Objects;

/**
 * Access some local DynamoDB by endpoint
 */
public class LocalProfileDetails extends ProfileDetails implements Cloneable{

    private final String endPoint;

    public LocalProfileDetails(String name, String endPoint) {
        super(name);
        this.endPoint = endPoint;
    }

    public String getEndPoint() {
        return endPoint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocalProfileDetails that)) return false;
        if (!super.equals(o)) return false;
        return endPoint.equals(that.endPoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), endPoint);
    }

    @Override
    public LocalProfileDetails clone() {
        return (LocalProfileDetails) super.clone();
    }
}
