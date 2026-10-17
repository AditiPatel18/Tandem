package com.tandem.tandem_backend.sync;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VersionVectorTest {

    @Test
    void shouldDetectEqualVectors() {
        Map<String, Long> first = Map.of(
                "device-A", 2L,
                "device-B", 1L
        );

        Map<String, Long> second = Map.of(
                "device-A", 2L,
                "device-B", 1L
        );

        assertEquals(
                VersionVector.Relation.EQUAL,
                VersionVector.compare(first, second)
        );
    }

    @Test
    void shouldDetectHappensBefore() {
        Map<String, Long> first = Map.of(
                "device-A", 2L,
                "device-B", 1L
        );

        Map<String, Long> second = Map.of(
                "device-A", 3L,
                "device-B", 1L
        );

        assertEquals(
                VersionVector.Relation.HAPPENS_BEFORE,
                VersionVector.compare(first, second)
        );
    }

    @Test
    void shouldDetectHappensAfter() {
        Map<String, Long> first = Map.of(
                "device-A", 3L,
                "device-B", 1L
        );

        Map<String, Long> second = Map.of(
                "device-A", 2L,
                "device-B", 1L
        );

        assertEquals(
                VersionVector.Relation.HAPPENS_AFTER,
                VersionVector.compare(first, second)
        );
    }

    @Test
    void shouldDetectConcurrentVectors() {
        Map<String, Long> first = Map.of(
                "device-A", 3L,
                "device-B", 1L
        );

        Map<String, Long> second = Map.of(
                "device-A", 2L,
                "device-B", 2L
        );

        assertEquals(
                VersionVector.Relation.CONCURRENT,
                VersionVector.compare(first, second)
        );
    }
}