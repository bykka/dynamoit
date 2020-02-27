package ua.org.java.dynamoit.utils;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Utils {

    public static <T> Stream<T> asStream(Iterable<T> iterable){
        return StreamSupport.stream(iterable.spliterator(), false);
    }

}
