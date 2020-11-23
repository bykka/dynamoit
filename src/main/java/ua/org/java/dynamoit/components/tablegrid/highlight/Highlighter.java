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

package ua.org.java.dynamoit.components.tablegrid.highlight;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import javafx.util.Pair;
import ua.org.java.dynamoit.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Highlighter {

    private static final double SUPER_LIGHT_BRIGHTNESS = 0.95;
    private static final List<Color> COLORS = Stream.of(
            Color.ALICEBLUE,
            Color.ANTIQUEWHITE,
            Color.AQUA,
            Color.AQUAMARINE,
            Color.AZURE,
            Color.BEIGE,
            Color.BISQUE,
            Color.BLACK,
            Color.BLANCHEDALMOND,
            Color.BLUE,
            Color.BLUEVIOLET,
            Color.BROWN,
            Color.BURLYWOOD,
            Color.CADETBLUE,
            Color.CHARTREUSE,
            Color.CHOCOLATE,
            Color.CORAL,
            Color.CORNFLOWERBLUE,
            Color.CORNSILK,
            Color.CRIMSON,
            Color.CYAN,
            Color.DARKBLUE,
            Color.DARKCYAN,
            Color.DARKGOLDENROD,
            Color.DARKGRAY,
            Color.DARKGREEN,
            Color.DARKGREY,
            Color.DARKKHAKI,
            Color.DARKMAGENTA,
            Color.DARKOLIVEGREEN,
            Color.DARKORANGE,
            Color.DARKORCHID,
            Color.DARKRED,
            Color.DARKSALMON,
            Color.DARKSEAGREEN,
            Color.DARKSLATEBLUE,
            Color.DARKSLATEGRAY,
            Color.DARKSLATEGREY,
            Color.DARKTURQUOISE,
            Color.DARKVIOLET,
            Color.DEEPPINK,
            Color.DEEPSKYBLUE,
            Color.DIMGRAY,
            Color.DIMGREY,
            Color.DODGERBLUE,
            Color.FIREBRICK,
            Color.FLORALWHITE,
            Color.FORESTGREEN,
            Color.FUCHSIA,
            Color.GAINSBORO,
            Color.GHOSTWHITE,
            Color.GOLD,
            Color.GOLDENROD,
            Color.GRAY,
            Color.GREEN,
            Color.GREENYELLOW,
            Color.GREY,
            Color.HONEYDEW,
            Color.HOTPINK,
            Color.INDIANRED,
            Color.INDIGO,
            Color.IVORY,
            Color.KHAKI,
            Color.LAVENDER,
            Color.LAVENDERBLUSH,
            Color.LAWNGREEN,
            Color.LEMONCHIFFON,
            Color.LIGHTBLUE,
            Color.LIGHTCORAL,
            Color.LIGHTCYAN,
            Color.LIGHTGOLDENRODYELLOW,
            Color.LIGHTGRAY,
            Color.LIGHTGREEN,
            Color.LIGHTGREY,
            Color.LIGHTPINK,
            Color.LIGHTSALMON,
            Color.LIGHTSEAGREEN,
            Color.LIGHTSKYBLUE,
            Color.LIGHTSLATEGRAY,
            Color.LIGHTSLATEGREY,
            Color.LIGHTSTEELBLUE,
            Color.LIGHTYELLOW,
            Color.LIME,
            Color.LIMEGREEN,
            Color.LINEN,
            Color.MAGENTA,
            Color.MAROON,
            Color.MEDIUMAQUAMARINE,
            Color.MEDIUMBLUE,
            Color.MEDIUMORCHID,
            Color.MEDIUMPURPLE,
            Color.MEDIUMSEAGREEN,
            Color.MEDIUMSLATEBLUE,
            Color.MEDIUMSPRINGGREEN,
            Color.MEDIUMTURQUOISE,
            Color.MEDIUMVIOLETRED,
            Color.MIDNIGHTBLUE,
            Color.MINTCREAM,
            Color.MISTYROSE,
            Color.MOCCASIN,
            Color.NAVAJOWHITE,
            Color.NAVY,
            Color.OLDLACE,
            Color.OLIVE,
            Color.OLIVEDRAB,
            Color.ORANGE,
            Color.ORANGERED,
            Color.ORCHID,
            Color.PALEGOLDENROD,
            Color.PALEGREEN,
            Color.PALETURQUOISE,
            Color.PALEVIOLETRED,
            Color.PAPAYAWHIP,
            Color.PEACHPUFF,
            Color.PERU,
            Color.PINK,
            Color.PLUM,
            Color.POWDERBLUE,
            Color.PURPLE,
            Color.RED,
            Color.ROSYBROWN,
            Color.ROYALBLUE,
            Color.SADDLEBROWN,
            Color.SALMON,
            Color.SANDYBROWN,
            Color.SEAGREEN,
            Color.SEASHELL,
            Color.SIENNA,
            Color.SILVER,
            Color.SKYBLUE,
            Color.SLATEBLUE,
            Color.SLATEGRAY,
            Color.SLATEGREY,
            Color.SNOW,
            Color.SPRINGGREEN,
            Color.STEELBLUE,
            Color.TAN,
            Color.TEAL,
            Color.THISTLE,
            Color.TOMATO,
            Color.TURQUOISE,
            Color.VIOLET,
            Color.WHEAT,
            Color.WHITE,
            Color.WHITESMOKE,
            Color.YELLOW,
            Color.YELLOWGREEN)
            .distinct()
            .filter(color -> getBrightness(color) < SUPER_LIGHT_BRIGHTNESS)
            .collect(Collectors.toList());

    private static final Random RAND = new Random();
    private final Map<String, ObservableList<Criteria>> attributeCriteria = new HashMap<>();

    public ObservableList<Criteria> getCriteria(String attribute) {
        return attributeCriteria.computeIfAbsent(attribute, s -> FXCollections.observableArrayList());
    }

    public void addEqHighlighting(String attribute, String value) {
        Pair<String, String> colors = getTwoRandomColors();
        getCriteria(attribute).add(0, new Criteria(value, ValueComparator.EQ, colors.getKey(), colors.getValue()));
    }

    public void setEqHighlighting(String attribute, String value) {
        Pair<String, String> colors = getTwoRandomColors();
        getCriteria(attribute).clear();
        getCriteria(attribute).add(new Criteria(value, ValueComparator.EQ, colors.getKey(), colors.getValue()));
    }

    public void clear() {
        attributeCriteria.values().forEach(List::clear);
    }

    // hsb brightness http://alienryderflex.com/hsp.html
    private static double getBrightness(Color color) {
        double r = color.getRed();
        double g = color.getGreen();
        double b = color.getBlue();
        return Math.sqrt(0.299 * (r * r) + 0.587 * (g * g) + 0.114 * (b * b));
    }

    private static Pair<String, String> getTwoRandomColors() {
        Color background = COLORS.get(RAND.nextInt(COLORS.size()));
        double brightness = getBrightness(background);

        String textColor = brightness < 0.7 && brightness != 0.0 ? "#FFFFFF" : "#000000";

        return new Pair<>(textColor, "#" + Integer.toHexString(background.hashCode()));
    }

    public static class Criteria {
        private final String value;
        private final ValueComparator comparator;
        private final String textColor;
        private final String backgroundColor;

        public Criteria(String value, ValueComparator comparator, String textColor, String backgroundColor) {
            this.value = value;
            this.comparator = comparator;
            this.textColor = textColor;
            this.backgroundColor = backgroundColor;
        }

        public boolean match(String another) {
            return comparator.getComparator().compare(value, another) == 0;
        }

        public String getTextColor() {
            return textColor;
        }

        public String getBackgroundColor() {
            return backgroundColor;
        }
    }

    public enum ValueComparator {

        EQ((o1, o2) -> {
            return Utils.trimToBlank(o1).compareTo(Utils.trimToBlank(o2));
        });

        private final Comparator<String> comparator;

        ValueComparator(Comparator<String> comparator) {
            this.comparator = comparator;
        }

        public Comparator<String> getComparator() {
            return comparator;
        }
    }

}
