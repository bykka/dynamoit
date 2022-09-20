package ua.org.java.dynamoit.model.profile;

/**
 * Access some local DynamoDB by endpoint
 */
public class LocalProfileDetails extends ProfileDetails {

    private final String endPoint;

    public LocalProfileDetails(String name, String endPoint) {
        super(name);
        this.endPoint = endPoint;
    }

    public String getEndPoint() {
        return endPoint;
    }
}
