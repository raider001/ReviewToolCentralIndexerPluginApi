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
     * Stops the plugin and releases all resources.
     *
     * <p>After this method returns the plugin must not call {@link EventSink#submit} or
     * interact with the {@link WebhookRouter} that was supplied to {@link #start}.
     */
    void stop();
}





