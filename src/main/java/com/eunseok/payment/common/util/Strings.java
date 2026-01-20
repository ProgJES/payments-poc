package com.eunseok.payment.common.util;

import java.util.UUID;

public final class Strings {
    private Strings() {}

    public static String normalizedOrGenerate(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return rawKey.trim();
    }
}
