package com.kalynx.centralindexer.spi;

import com.kalynx.centralindexer.model.ReviewEvent;

/**
 * Accepts canonical {@link ReviewEvent} instances from a provider plugin and passes
 * them into the central indexer pipeline for persistence and fan-out to connected clients.
 *
 * <p>Implementations are provided by the indexer and injected into the plugin via
 * {@link ProviderPlugin#start(ProviderConfig, EventSink, WebhookRouter)}.
 *
 * <p>The indexer runs exactly one plugin at a time. All events submitted through this
 * sink originate from that single plugin instance.
 */
public interface EventSink {

    /**
     * Submits an event into the indexer pipeline.
     *
     * <p>The call returns as soon as the event is accepted; persistence and fan-out
     * happen asynchronously. Implementations are thread-safe and may be called from
     * any thread (e.g. a webhook handler thread or a polling virtual thread).
     *
     * @param event the event to submit; must not be {@code null}
     */
    void submit(ReviewEvent event);
}


