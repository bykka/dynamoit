package ua.org.java.dynamoit.model.profile;

public class ProfileDetails {

    protected final String name;

    protected ProfileDetails(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

