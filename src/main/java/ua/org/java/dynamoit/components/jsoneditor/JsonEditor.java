package ua.org.java.dynamoit.components.jsoneditor;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonEditor extends CodeArea {

    private static final Pattern JSON_REGEX = Pattern.compile(
            "(?<JSONCURLY>[{}])" + "|"
                    + "(?<JSONPROPERTY>\".*\")\\s*:\\s*" + "|"
                    + "(?<JSONVALUE>\".*\")" + "|"
                    + "\\[(?<JSONARRAY>.*)]" + "|"
                    + "(?<JSONBOOL>true|false)" + "|"
                    + "(?<JSONNUMBER>\\d*.?\\d*)"
    );

    public JsonEditor(String text) {
        setParagraphGraphicFactory(LineNumberFactory.get(this));
        this.getStylesheets().add(getClass().getResource("/css/jsoneditor.css").toString());

        this.multiPlainChanges()
                .successionEnds(Duration.ofMillis(400))
                .subscribe(__ -> highlight());

        replaceText(text);
    }

    private void highlight() {
        this.setStyleSpans(0, computeHighlighting(getText()));
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = JSON_REGEX.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            String styleClass
                    = matcher.group("JSONPROPERTY") != null ? "json_property"
                    : matcher.group("JSONVALUE") != null ? "json_value"
                    : matcher.group("JSONARRAY") != null ? "json_array"
                    : matcher.group("JSONCURLY") != null ? "json_curly"
                    : matcher.group("JSONBOOL") != null ? "json_bool"
                    : matcher.group("JSONNUMBER") != null ? "json_number"
                    : null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

}
