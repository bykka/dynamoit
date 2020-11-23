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

package ua.org.java.dynamoit.components.profileviewer;

import dagger.Module;
import dagger.Provides;
import ua.org.java.dynamoit.EventBus;
import ua.org.java.dynamoit.components.main.MainModel;
import ua.org.java.dynamoit.db.DynamoDBService;

import javax.inject.Singleton;

@Module
public class ProfileModule {

    @Provides
    @Singleton
    public MainModel.ProfileModel model(MainModel mainModel, String profile) {
        return mainModel.getAvailableProfiles().get(profile);
    }

    @Provides
    public ProfileView view(ProfileController controller, MainModel.ProfileModel model){
        return new ProfileView(controller, model);
    }

    @Provides
    public ProfileController controller(MainModel.ProfileModel model, DynamoDBService dynamoDBService, EventBus eventBus){
        return new ProfileController(model, dynamoDBService, eventBus);
    }

}
