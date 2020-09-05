/*
 * This file is part of DynamoIt.
 *
 *     DynamoIt is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Foobar is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with DynamoIt.  If not, see <https://www.gnu.org/licenses/>.
 */

package ua.org.java.dynamoit.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ThreadNameSimpleFormatter extends Formatter {

    private String format;

    public ThreadNameSimpleFormatter() {
        this.format = LogManager.getLogManager().getProperty("ua.org.java.dynamoit.utils.ThreadNameSimpleFormatter.format");
    }

    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * @see SimpleFormatter#format(LogRecord)
     */
    @Override
    public String format(LogRecord record) {
        ZonedDateTime zdt = record.getInstant().atZone(ZoneId.systemDefault());

        String source = Stream.of(Optional.ofNullable(record.getSourceClassName()), Optional.ofNullable(record.getSourceMethodName()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining(" "));

        String message = formatMessage(record);

        String throwable = Optional.ofNullable(record.getThrown())
                .map(thrown -> {
                    StringWriter sw = new StringWriter();
                    try (PrintWriter pw = new PrintWriter(sw)) {
                        pw.println();
                        record.getThrown().printStackTrace(pw);
                    }
                    return sw.toString();
                }).orElse("");

        String threadName = Thread.currentThread().getName();

        return String.format(format,
                zdt,
                source,
                record.getLoggerName(),
                record.getLevel().getLocalizedName(),
                message,
                throwable,
                threadName
                );
    }

}
