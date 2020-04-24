package ua.org.java.dynamoit;

import dagger.Module;
import dagger.Provides;
import javafx.scene.layout.Region;
import ua.org.java.dynamoit.components.activityindicator.ActivityIndicator;
import ua.org.java.dynamoit.db.DynamoDBService;

import javax.inject.Singleton;

@Module
public class MainModule {

    @Provides
    public static ActivityIndicator activityIndicator(EventBus eventBus){
        return new ActivityIndicator(eventBus);
    }

    @Provides
    public static Region mainView(MainModel model, MainController controller, ActivityIndicator activityIndicator) {
        return new MainView(model, controller, activityIndicator);
    }

    @Provides
    @Singleton
    public static MainModel model() {
        return new MainModel();
    }

    @Provides
    @Singleton
    public static EventBus eventBus(){
        return new EventBus();
    }

    @Provides
    public static MainController controller(DynamoDBService dynamoDBService, MainModel model, EventBus eventBus) {
        return new MainController(dynamoDBService, model, eventBus);
    }

}
