package com.lumie.event;

/**
 * Marker for cross-module domain events serialized by Spring Modulith's outbox.
 *
 * <p>Carries an explicit {@link #schemaVersion()} so future shape migrations
 * (renamed/added/removed fields) can be detected by listeners without breaking
 * already-published events sitting in {@code event_publication}. New events
 * default to {@code 1}; bump the override when shipping a backwards-incompatible
 * change.
 *
 * <p>Modulith serializes the record as JSON, so the new accessor is additive —
 * existing serialized events without the field deserialize cleanly when consumed
 * by a listener compiled against this interface (Jackson supplies the default).
 */
public interface DomainEvent {

    /**
     * Schema version of this event payload. Increment whenever a field is renamed,
     * removed, or has its semantics changed (additive field changes don't require a bump).
     *
     * @return positive integer; defaults to {@code 1}.
     */
    default int schemaVersion() {
        return 1;
    }
}
