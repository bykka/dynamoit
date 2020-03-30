package ua.org.java.dynamoit;

import dagger.Module;
import dagger.Provides;
import javafx.scene.layout.Region;
import ua.org.java.dynamoit.db.DynamoDBService;

import javax.inject.Singleton;

@Module
public class MainModule {

    @Provides
    public Region mainView(DynamoDBService dynamoDBService, MainModel model){
        return new MainView(dynamoDBService, model);
    }

    @Provides
    @Singleton
    public static MainModel model(){
        return new MainModel();
    }

}
