package com.solubris;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static com.solubris.StreamSugar.append;
import static com.solubris.StreamSugar.concat;
import static com.solubris.StreamSugar.prepend;
import static org.assertj.core.api.Assertions.assertThat;

class StreamSugarTest {
    @Test
    void canPrependDifferentSubClasses() {
        Stream<Number> stream = prepend(1.0F, Stream.of(2.0, 3.0));

        assertThat(stream).containsExactly(1.0F, 2.0, 3.0);
    }

    @Test
    void canAppendDifferentSubClasses() {
        Stream<Number> stream = append(Stream.of(2.0, 3.0), 1.0F);

        assertThat(stream).containsExactly(2.0, 3.0, 1.0F);
    }

    @Test
    void canConcatDifferentSubClasses() {
        Stream<Number> stream = concat(Stream.of(2.0, 3.0), Stream.of(4.0F, 5.0F), Stream.of(1));

        assertThat(stream).containsExactly(2.0, 3.0, 4.0F, 5.0F, 1);
    }
}