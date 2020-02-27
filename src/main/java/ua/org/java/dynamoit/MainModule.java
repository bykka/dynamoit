package ua.org.java.dynamoit;

import dagger.Binds;
import dagger.Module;
import javafx.scene.layout.Region;

@Module
abstract public class MainModule {

    @Binds
    abstract Region mainView(MainView mainView);

}
