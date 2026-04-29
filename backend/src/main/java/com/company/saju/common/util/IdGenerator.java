package com.company.saju.common.util;

import com.github.f4b6a3.uuid.UuidCreator;

public final class IdGenerator {

    private IdGenerator() {
    }

    public static String generateId() {
        return UuidCreator.getTimeOrderedEpoch().toString();
    }

    public static String generateIdWithPrefix(String prefix) {
        return prefix + "-" + generateId();
    }
}
