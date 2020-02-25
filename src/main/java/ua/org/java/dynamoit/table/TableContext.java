package ua.org.java.dynamoit.table;

public class TableContext {

    private String profileName;
    private String tableName;

    public TableContext(String profileName, String tableName) {
        this.profileName = profileName;
        this.tableName = tableName;
    }

    public String getProfileName() {
        return profileName;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public String toString() {
        return "TableContext{" +
                "profileName='" + profileName + '\'' +
                ", tableName='" + tableName + '\'' +
                '}';
    }
}
