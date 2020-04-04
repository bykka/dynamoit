package ua.org.java.dynamoit.table;

public class TableContext {

    private String profileName;
    private String tableName;
    private String propertyName;
    private String propertyValue;

    public TableContext(String profileName, String tableName) {
        this.profileName = profileName;
        this.tableName = tableName;
    }

    public TableContext(String profileName, String tableName, String propertyName, String propertyValue) {
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

}
