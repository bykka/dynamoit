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

package ua.org.java.dynamoit.services;

import software.amazon.awssdk.profiles.ProfileFile;
import ua.org.java.dynamoit.model.profile.PreconfiguredProfileDetails;
import ua.org.java.dynamoit.model.profile.ProfileDetails;

import java.util.stream.Stream;

import static ua.org.java.dynamoit.utils.RegionsUtils.DEFAULT_REGION;

public class ProfileService {

    /**
     * Returns profiles registered in <b>~/.aws</b> directory
     * @return stream of configured profiles
     */
    public static Stream<ProfileDetails> getDefaultProfiles() {
        // config file contains profile and region values
        return ProfileFile.defaultProfileFile().profiles()
                .values()
                .stream()
                .map(profile -> new PreconfiguredProfileDetails(profile.name(), profile.property("region").orElse(DEFAULT_REGION)));
    }

}
