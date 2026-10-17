package com.tandem.tandem_backend.sync;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VersionVector {

    public enum Relation {
        EQUAL,
        HAPPENS_BEFORE,
        HAPPENS_AFTER,
        CONCURRENT
    }

    private VersionVector() {
    }

    public static Relation compare(
            Map<String, Long> first,
            Map<String, Long> second
    ) {
        Set<String> devices = new HashSet<>();

        if (first != null) {
            devices.addAll(first.keySet());
        }

        if (second != null) {
            devices.addAll(second.keySet());
        }

        boolean firstIsLess = false;
        boolean firstIsGreater = false;

        for (String deviceId : devices) {
            long firstValue = getValue(first, deviceId);
            long secondValue = getValue(second, deviceId);

            if (firstValue < secondValue) {
                firstIsLess = true;
            }

            if (firstValue > secondValue) {
                firstIsGreater = true;
            }
        }

        if (firstIsLess && firstIsGreater) {
            return Relation.CONCURRENT;
        }

        if (firstIsLess) {
            return Relation.HAPPENS_BEFORE;
        }

        if (firstIsGreater) {
            return Relation.HAPPENS_AFTER;
        }

        return Relation.EQUAL;
    }

    private static long getValue(
            Map<String, Long> vector,
            String deviceId
    ) {
        if (vector == null) {
            return 0;
        }

        return vector.getOrDefault(deviceId, 0L);
    }
}