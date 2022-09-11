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

package ua.org.java.dynamoit.components.thememanager;

import atlantafx.base.theme.NordDark;
import atlantafx.base.theme.NordLight;
import atlantafx.base.theme.Theme;
import javafx.application.Application;
import javafx.css.PseudoClass;
import javafx.scene.Node;

public class ThemeManager {

    private static final PseudoClass DARK = PseudoClass.getPseudoClass("dark");

    private enum ThemeType {
        LIGHT(new NordLight()),
        DARK(new NordDark());
        private final Theme theme;

        ThemeType(Theme theme) {
            this.theme = theme;
        }

        public Theme getTheme() {
            return theme;
        }
    }

    private ThemeType currentTheme = ThemeType.LIGHT;

    public Theme getCurrentTheme() {
        return currentTheme.getTheme();
    }

    public ThemeManager switchTheme() {
        if (currentTheme == ThemeType.LIGHT) {
            currentTheme = ThemeType.DARK;
        } else {
            currentTheme = ThemeType.LIGHT;
        }
        return this;
    }

    public ThemeManager applyCurrentTheme() {
        Application.setUserAgentStylesheet(this.currentTheme.getTheme().getUserAgentStylesheet());
        return this;
    }

    public ThemeManager applyPseudoClasses(Node node){
        node.pseudoClassStateChanged(DARK, currentTheme.getTheme().isDarkMode());
        return this;
    }

}
