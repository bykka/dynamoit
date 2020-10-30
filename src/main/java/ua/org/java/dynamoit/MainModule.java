/*
 * This file is part of DynamoIt.
 *
 *     DynamoIt is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     DynamoIt is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with DynamoIt.  If not, see <https://www.gnu.org/licenses/>.
 */

package ua.org.java.dynamoit;

import dagger.Module;
import dagger.Provides;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.scene.layout.Region;
import ua.org.java.dynamoit.components.activityindicator.ActivityIndicator;
import ua.org.java.dynamoit.db.DynamoDBService;
import ua.org.java.dynamoit.utils.FXExecutor;

import javax.inject.Singleton;

@Module
public class MainModule {

    @Provides
    @Singleton
    public static HostServices hostServices(Application application){
        return application.getHostServices();
    }

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
        return new EventBus(FXExecutor.getInstance());
    }

    @Provides
    public static MainController controller(DynamoDBService dynamoDBService, MainModel model, EventBus eventBus, HostServices hostServices) {
        return new MainController(dynamoDBService, model, eventBus, hostServices);
    }

}
