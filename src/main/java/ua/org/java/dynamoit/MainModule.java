package ua.org.java.dynamoit;

import dagger.Module;
import dagger.Provides;
import javafx.scene.layout.Region;
import ua.org.java.dynamoit.db.DynamoDBService;

import javax.inject.Singleton;

@Module
public class MainModule {

    @Provides
    public static Region mainView(MainModel model, MainController controller) {
        return new MainView(model, controller);
    }

    @Provides
    @Singleton
    public static MainModel model() {
        return new MainModel();
    }

    @Provides
    public static MainController controller(DynamoDBService dynamoDBService, MainModel model) {
        return new MainController(dynamoDBService, model);
    }

}
