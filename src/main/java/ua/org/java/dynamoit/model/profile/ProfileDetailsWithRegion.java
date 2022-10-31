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

public abstract class ProfileDetailsWithRegion extends ProfileDetails implements Cloneable {

    protected String region;

    protected ProfileDetailsWithRegion(String name, String region) {
        super(name);
        this.region = region;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProfileDetailsWithRegion that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(region, that.region);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), region);
    }

    @Override
    public ProfileDetailsWithRegion clone() {
        return (ProfileDetailsWithRegion) super.clone();
    }
}
