package ua.org.java.dynamoit.db;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class DynamoDBModule {

    @Provides
    @Singleton
    public static DynamoDBService dynamoDBService() {
        return new DynamoDBService();
    }

}
