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

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.collections.ObservableList;
import org.junit.Test;
import ua.org.java.dynamoit.components.main.MainModel;
import ua.org.java.dynamoit.model.TableDef;
import ua.org.java.dynamoit.model.profile.PreconfiguredProfileDetails;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProfileViewTest {

    @Test
    public void testChangesInAllTables() {
        MainModel.ProfileModel model = new MainModel.ProfileModel(new PreconfiguredProfileDetails("profile1", "region1"));
        ObservableList<TableDef> tables = model.getAvailableTables();

        ObjectBinding<List<String>> tab2 = Bindings.createObjectBinding(() -> tables.stream()
                .map(TableDef::getName)
                .filter(t -> t.startsWith("tab2"))
                .collect(Collectors.toList()), tables);

        assertTrue(tab2.get().isEmpty());

        tables.add(new TableDef("tab1"));

        assertTrue(tab2.get().isEmpty());

        tables.add(new TableDef("tab2"));

        assertEquals(1, tab2.get().size());
    }

}
