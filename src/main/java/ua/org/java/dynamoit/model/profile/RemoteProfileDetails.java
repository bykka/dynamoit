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
 * Allows to access remote profile
 */
public class RemoteProfileDetails extends ProfileDetails {

    private final String region;
    private final String accessKeyId;
    private final String secretKey;

    public RemoteProfileDetails(String name, String region, String accessKeyId, String secretKey) {
        super(name);
        this.region = region;
        this.accessKeyId = accessKeyId;
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RemoteProfileDetails that)) return false;
        if (!super.equals(o)) return false;
        return region.equals(that.region) && accessKeyId.equals(that.accessKeyId) && secretKey.equals(that.secretKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), region, accessKeyId, secretKey);
    }
}
