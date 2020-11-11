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
import ua.org.java.dynamoit.utils.Utils;

import java.util.*;

public class Highlightor {

    private static final String[] COLORS = new String[]{
            "aliceblue",
            "antiquewhite",
            "aqua",
            "aquamarine",
            "azure",
            "beige",
            "bisque",
            "black",
            "blanchedalmond",
            "blue",
            "blueviolet",
            "brown",
            "burlywood",
            "cadetblue",
            "chartreuse",
            "chocolate",
            "coral",
            "cornflowerblue",
            "cornsilk",
            "crimson",
            "cyan",
            "darkblue",
            "darkcyan",
            "darkgoldenrod",
            "darkgray",
            "darkgreen",
            "darkgrey",
            "darkkhaki",
            "darkmagenta",
            "darkolivegreen",
            "darkorange",
            "darkorchid",
            "darkred",
            "darksalmon",
            "darkseagreen",
            "darkslateblue",
            "darkslategray",
            "darkslategrey",
            "darkturquoise",
            "darkviolet",
            "deeppink",
            "deepskyblue",
            "dimgray",
            "dimgrey",
            "dodgerblue",
            "firebrick",
            "floralwhite",
            "forestgreen",
            "fuchsia",
            "gainsboro",
            "ghostwhite",
            "gold",
            "goldenrod",
            "gray",
            "green",
            "greenyellow",
            "grey",
            "honeydew",
            "hotpink",
            "indianred",
            "indigo",
            "ivory",
            "khaki",
            "lavender",
            "lavenderblush",
            "lawngreen",
            "lemonchiffon",
            "lightblue",
            "lightcoral",
            "lightcyan",
            "lightgoldenrodyellow",
            "lightgray",
            "lightgreen",
            "lightgrey",
            "lightpink",
            "lightsalmon",
            "lightseagreen",
            "lightskyblue",
            "lightslategray",
            "lightslategrey",
            "lightsteelblue",
            "lightyellow",
            "lime",
            "limegreen",
            "linen",
            "magenta",
            "maroon",
            "mediumaquamarine",
            "mediumblue",
            "mediumorchid",
            "mediumpurple",
            "mediumseagreen",
            "mediumslateblue",
            "mediumspringgreen",
            "mediumturquoise",
            "mediumvioletred",
            "midnightblue",
            "mintcream",
            "mistyrose",
            "moccasin",
            "navajowhite",
            "navy",
            "oldlace",
            "olive",
            "olivedrab",
            "orange",
            "orangered",
            "orchid",
            "palegoldenrod",
            "palegreen",
            "paleturquoise",
            "palevioletred",
            "papayawhip",
            "peachpuff",
            "peru",
            "pink",
            "plum",
            "powderblue",
            "purple",
            "red",
            "rosybrown",
            "royalblue",
            "saddlebrown",
            "salmon",
            "sandybrown",
            "seagreen",
            "seashell",
            "sienna",
            "silver",
            "skyblue",
            "slateblue",
            "slategray",
            "slategrey",
            "snow",
            "springgreen",
            "steelblue",
            "tan",
            "teal",
            "thistle",
            "tomato",
            "turquoise",
            "violet",
            "wheat",
            "white",
            "whitesmoke",
            "yellow",
            "yellowgreen"};
    private static final Random RAND = new Random();
    private final Map<String, ObservableList<Criteria>> attributeCriteria = new HashMap<>();

    public ObservableList<Criteria> getCriteria(String attribute) {
        return attributeCriteria.computeIfAbsent(attribute, s -> FXCollections.observableArrayList());
    }

    public void addEqHighlighting(String attribute, String value) {
        getCriteria(attribute).add(new Criteria(value, ValueComparator.EQ, COLORS[RAND.nextInt(COLORS.length)], COLORS[RAND.nextInt(COLORS.length)]));
    }

    public void setEqHighlighting(String attribute, String value) {
        getCriteria(attribute).clear();
        getCriteria(attribute).add(new Criteria(value, ValueComparator.EQ, COLORS[RAND.nextInt(COLORS.length)], COLORS[RAND.nextInt(COLORS.length)]));
    }

    public void clear() {
        attributeCriteria.values().forEach(List::clear);
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
