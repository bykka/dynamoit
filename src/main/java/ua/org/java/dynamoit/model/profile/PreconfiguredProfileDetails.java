package ua.org.java.dynamoit.model.profile;

/**
 * AWS CLI configured
 */
public class PreconfiguredProfileDetails extends ProfileDetails {

    private final String region;

    public PreconfiguredProfileDetails(String name, String region) {
        super(name);
        this.region = region;
    }

    public String getRegion() {
        return region;
    }
}
