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
