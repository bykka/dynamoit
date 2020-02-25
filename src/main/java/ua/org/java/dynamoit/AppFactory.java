package ua.org.java.dynamoit;

import dagger.Component;
import javafx.scene.layout.Region;

import javax.inject.Singleton;

@Component(modules = {MainModule.class})
@Singleton
public interface AppFactory {

    Region mainView();

}
