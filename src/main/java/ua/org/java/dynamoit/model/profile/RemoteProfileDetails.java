package ua.org.java.dynamoit.model.profile;

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
}
