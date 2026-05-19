package com.kalynx.centralindexer.model;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable canonical event produced by a provider plugin and stored by the central indexer.
 *
 * <p>Fields common to every event are promoted to top-level record components. Any
 * provider-specific or event-specific data is carried in {@link #payload()} as a
 * simple string-to-string map so that the API module has no external dependencies.
 *
 * @param sequenceNo  Monotonically increasing sequence number assigned by the indexer
 *                    after the event is persisted. Zero when the event has not yet been
 *                    stored (i.e. while in-flight from plugin to indexer).
 * @param timestamp   UTC instant at which the event occurred on the provider side.
 * @param repository  Canonical repository identifier in the form {@code owner/repo}.
 * @param eventType   The type of change that occurred.
 * @param reviewId    Identifier of the affected review, or {@code null} for branch-only
 *                    events ({@link EventType#BRANCH_UPDATED}, {@link EventType#BRANCH_DELETED}).
 * @param actorUser   Username of the user who triggered the event, or {@code null} if
 *                    the provider did not supply one.
 * @param deliveryId  Provider-supplied delivery identifier used for cross-node
 *                    deduplication (e.g. {@code X-GitHub-Delivery}). May be {@code null}
 *                    for events that originate from polling rather than webhooks.
 * @param payload     Arbitrary key-value pairs carrying event-specific detail (e.g.
 *                    {@code "branchName"}, {@code "commitSha"}, {@code "commentId"}).
 *                    Must not be {@code null}; use {@link Map#of()} when there is no
 *                    extra data.
 */
public record ReviewEvent(
        long sequenceNo,
        Instant timestamp,
        String repository,
        EventType eventType,
        String reviewId,
        String actorUser,
        String deliveryId,
        Map<String, String> payload
) {

    /**
     * Constructs a {@code ReviewEvent}, enforcing that mandatory fields are non-null.
     */
    public ReviewEvent {
        if (timestamp == null) throw new IllegalArgumentException("timestamp must not be null");
        if (repository == null || repository.isBlank()) throw new IllegalArgumentException("repository must not be blank");
        if (eventType == null) throw new IllegalArgumentException("eventType must not be null");
        if (payload == null) throw new IllegalArgumentException("payload must not be null");
        payload = Map.copyOf(payload);
    }
}



