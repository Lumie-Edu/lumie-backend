package com.lumie.exam.application.port.out;

public interface CallbackDeduplicationPort {

    /**
     * Atomically marks {@code key} as seen.
     *
     * @return {@code true} if this is the first delivery; {@code false} if the key
     *         was already present (duplicate).
     */
    boolean markIfAbsent(String key);
}
