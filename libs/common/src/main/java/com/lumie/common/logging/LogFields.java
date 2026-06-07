package com.lumie.common.logging;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

public final class LogFields {

    private LogFields() {
    }

    public static Object field(String key, Object value) {
        return keyValue(key, value);
    }
}
