package com.solubris;

import java.util.stream.Stream;

import static java.util.function.Function.identity;

public class StreamSugar {
    private StreamSugar() {
    }

    public static <T> Stream<T> prepend(T t, Stream<T> tStream) {
        return Stream.concat(Stream.of(t), tStream);
    }

    public static <T> Stream<T> append(Stream<T> tStream, T t) {
        return Stream.concat(tStream, Stream.of(t));
    }

    @SafeVarargs
    public static <T> Stream<T> concat(Stream<T>... tStreams) {
        return Stream.of(tStreams).flatMap(identity());
    }
}
