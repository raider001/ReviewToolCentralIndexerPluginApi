package com.kalynx.centralindexer.spi;

import java.time.Instant;

/**
 * SPI contract that all provider plugins must implement.
 *
 * <p>Exactly one plugin is loaded per indexer instance. The plugin is discovered at
 * startup via {@link java.util.ServiceLoader} from JARs placed in the configured plugins
 * directory. If zero or more than one implementation is found the indexer exits with an
 * error.
 *
 * <p>The plugin handles one git hosting provider (e.g. GitHub, Bitbucket, GitLab) and is
 * responsible for:
 * <ul>
 *   <li>Registering webhook endpoints via {@link WebhookRouter}.</li>
 *   <li>Verifying request signatures and parsing payloads into canonical
 *       {@link com.kalynx.centralindexer.model.ReviewEvent ReviewEvents}.</li>
 *   <li>Optionally polling the provider API when webhooks are unavailable.</li>
 *   <li>Reconciling missed events after an indexer restart via {@link #reconcile}.</li>
 * </ul>
 *
 * <p>The lifecycle is:
 * <ol>
 *   <li>{@link #start} — called once with the plugin's {@link ProviderConfig}.</li>
 *   <li>{@link #reconcile} — called once per repository in {@code repository_state}
 *       so the plugin can backfill anything missed during downtime.</li>
 *   <li>{@link #stop} — called on orderly shutdown; implementations must release all
 *       resources (threads, connections, scheduler tasks).</li>
 * </ol>
 *
 * <p>All methods must be thread-safe.
 */
public interface ProviderPlugin {

    /**
     * Returns the unique identifier for this provider (e.g. {@code "github"},
     * {@code "bitbucket"}, {@code "gitlab"}).
     *
     * <p>The indexer validates this value against {@link ProviderConfig#providerId()} from
     * {@code config.json} on startup. A mismatch — indicating the wrong plugin JAR has been
     * placed in the plugins directory — causes the indexer to exit with an error.
     *
     * @return a non-null, non-blank provider identifier
     */
    String providerId();

    /**
     * Starts the plugin with the configuration provided by the operator.
     *
     * <p>Called exactly once at indexer startup. The plugin should register all required
     * webhook paths via {@code router} and start any background polling threads if needed.
     * The {@code sink} reference remains valid until {@link #stop()} returns and must not
     * be used afterwards.
     *
     * @param config  the full plugin configuration block from {@code config.json}; never {@code null}
     * @param sink    target for canonical events emitted by this plugin; never {@code null}
     * @param router  used to register HTTP POST webhook endpoints; never {@code null}
     */
    void start(ProviderConfig config, EventSink sink, WebhookRouter router);

    /**
     * Backfills any events that were missed while the indexer was offline.
     *
     * <p>Called by the indexer at startup for each repository that this plugin is
     * responsible for and that already has a row in {@code repository_state} (i.e. has
     * previously received at least one event). The plugin must query the provider API
     * for all changes that occurred after {@code since} and submit them via the
     * {@link EventSink} that was provided to {@code start}.
     *
     * <p>Implementations should be idempotent: duplicate events submitted here will be
     * deduplicated by the indexer using the provider delivery ID held in the event
     * payload.
     *
     * @param repository canonical repository identifier ({@code owner/repo}); never {@code null}
     * @param since      UTC instant of the last successfully stored event for this
     *                   repository; the plugin must return all changes strictly after this
     *                   point.  Will be {@link Instant#EPOCH} when no events have been
     *                   stored yet.
     */
    void reconcile(String repository, Instant since);

    /**
     * Emits a {@link com.kalynx.centralindexer.model.EventType#BRANCH_UPDATED} event for
     * every branch currently present in the repository.
     *
     * <p>Called once per repository during startup reconciliation so that the
     * {@code branches} table is populated even when no webhooks have been received yet.
     * The default implementation is a no-op — override to enable branch backfill.
     *
     * @param repository canonical repository identifier ({@code owner/repo}); never {@code null}
     */
    default void reconcileAllBranches(String repository) {
    }

    /**
     * Emits review events for every file in the full tree of
     * {@code refs/heads/kalynx-reviews} at the given commit.
     *
     * <p>Called once per repository on the very first startup (when no
     * {@code kalynx_review_head} cursor has been stored yet) so that
     * {@code reviews_index} is fully populated from the existing branch content.
     * The default implementation is a no-op — override to enable full-tree indexing.
     *
     * @param repository canonical repository identifier ({@code owner/repo}); never {@code null}
     * @param headCommit the HEAD commit SHA of {@code refs/heads/kalynx-reviews}
     * @return {@code true} if reconciliation completed successfully and the cursor may be
     *         advanced; {@code false} if it failed (e.g., provider API unreachable) and the
     *         cursor must not be advanced
     */
    default boolean reconcileFullReviewTree(String repository, String headCommit) {
        return false;
    }

    /**
     * Returns the current HEAD commit SHA of the {@code refs/heads/kalynx-reviews} orphan
     * branch in the given repository, or {@code null} if the branch does not exist or the
     * plugin does not support this operation.
     *
     * <p>Called by the indexer at startup for each tracked repository to determine whether
     * the stored {@code kalynx_review_head} cursor is behind the live state. If this method
     * returns {@code null}, no commit-based reconciliation is attempted for that repository.
     *
     * <p>The default implementation returns {@code null} (opt-out). Override to enable
     * commit-based startup reconciliation.
     *
     * @param repository canonical repository identifier ({@code owner/repo}); never {@code null}
     * @return the 40-character commit SHA, or {@code null}
     */
    default String fetchKalynxReviewHead(String repository) {
        return null;
    }

    /**
     * Replays all review events from commits on {@code refs/heads/kalynx-reviews} in the
     * range {@code (fromCommit, toCommit]} — i.e., commits reachable from {@code toCommit}
     * that are not reachable from {@code fromCommit}.
     *
     * <p>For each file changed in that commit range whose path matches
     * {@code reviews/{reviewId}/{streamName}}, the plugin constructs a {@link com.kalynx.centralindexer.model.ReviewEvent}
     * and submits it via the {@link EventSink} provided to {@link #start}. Submissions are
     * idempotent from the indexer's perspective.
     *
     * <p>The default implementation is a no-op. Override alongside
     * {@link #fetchKalynxReviewHead} to enable commit-based reconciliation.
     *
     * @param repository canonical repository identifier ({@code owner/repo}); never {@code null}
     * @param fromCommit the exclusive lower-bound commit SHA (the last known-good state)
     * @param toCommit   the inclusive upper-bound commit SHA (the current live HEAD)
     * @return {@code true} if reconciliation completed successfully and the cursor may be
     *         advanced; {@code false} if it failed and the cursor must not be advanced
     */
    default boolean reconcileFromCommit(String repository, String fromCommit, String toCommit) {
        return false;
    }

    /**
     * Stops the plugin and releases all resources.
     *
     * <p>After this method returns the plugin must not call {@link EventSink#submit} or
     * interact with the {@link WebhookRouter} that was supplied to {@link #start}.
     */
    void stop();
}





