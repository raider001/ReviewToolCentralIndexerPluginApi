package com.kalynx.centralindexer.model;

/**
 * Canonical event types emitted by provider plugins into the central indexer.
 *
 * <p>Review lifecycle events cover the full state machine of a code review backed
 * by git notes. Branch events are emitted for any push to {@code refs/heads/*} so
 * that clients can detect when a reviewed branch receives new commits or is deleted
 * without polling.
 *
 * <p>Status changes (e.g. {@code OPEN → APPROVED}) are represented as
 * {@link #REVIEW_UPDATED} — there is no separate status-change event type.
 */
public enum EventType {

    /**
     * A new review has been opened (first write to a {@code refs/notes/reviews/*} ref
     * for a commit that has no prior review note).
     */
    REVIEW_CREATED,

    /**
     * The review metadata has been updated. This includes title, description, status
     * changes (e.g. {@code OPEN → APPROVED}), and any other review-level field change.
     */
    REVIEW_UPDATED,

    /**
     * The review has been closed (merged, declined, or abandoned).
     */
    REVIEW_CLOSED,

    /**
     * A new comment has been added to the review thread.
     */
    REVIEW_COMMENT_ADDED,

    /**
     * An existing comment has been edited.
     */
    REVIEW_COMMENT_UPDATED,

    /**
     * A branch under {@code refs/heads/*} has received a new commit (force-push included).
     */
    BRANCH_UPDATED,

    /**
     * A branch under {@code refs/heads/*} has been deleted.
     */
    BRANCH_DELETED
}


