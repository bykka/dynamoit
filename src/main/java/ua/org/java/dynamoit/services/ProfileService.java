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
