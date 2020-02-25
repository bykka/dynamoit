package ua.org.java.dynamoit;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import javafx.scene.layout.Region;

import javax.inject.Singleton;

@Module
abstract public class MainModule {

    @Binds
    abstract Region mainView(MainView mainView);

    @Provides
    @Singleton
    public static MainController mainController(){
        return new MainController();
    }

}
