package com.kalynx.centralindexer.spi;

/**
 * Allows a provider plugin to register HTTP POST endpoints on the indexer's webhook
 * listener.
 *
 * <p>An implementation is provided by the indexer and injected into the active plugin via
 * {@link ProviderPlugin#start(ProviderConfig, EventSink, WebhookRouter)}.  The
 * indexer mounts all registered handlers under the {@code /webhooks/} base path.
 */
public interface WebhookRouter {

    /**
     * Registers a handler for HTTP POST requests arriving at the given path suffix.
     *
     * <p>The full path exposed by the server is {@code /webhooks/{pathSuffix}}.
     * Plugins may call this method more than once to register multiple endpoints
     * (e.g. separate paths for push events and pull-request events when a provider
     * sends them to different URLs).
     *
     * @param pathSuffix path segment appended after {@code /webhooks/}; must not be
     *                   {@code null} or blank.
     * @param handler    the handler that will process matching requests; must not be
     *                   {@code null}.
     */
    void registerPost(String pathSuffix, WebhookHandler handler);
}


