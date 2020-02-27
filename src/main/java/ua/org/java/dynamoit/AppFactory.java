package ua.org.java.dynamoit;

import dagger.Component;
import javafx.scene.layout.Region;
import ua.org.java.dynamoit.db.DynamoDBModule;

import javax.inject.Singleton;

@Component(modules = {MainModule.class, DynamoDBModule.class})
@Singleton
public interface AppFactory {

    Region mainView();

}
