package com.kalynx.centralindexer.spi;

import java.util.Map;

/**
 * Handles a single inbound webhook HTTP request.
 *
 * <p>Implementations are registered via {@link WebhookRouter} and are called by the
 * indexer's HTTP server on matching requests.  The handler is responsible for
 * verifying the request signature, parsing the payload, and forwarding one or more
 * {@link com.kalynx.centralindexer.model.ReviewEvent ReviewEvents} to the
 * {@link EventSink}.
 */
@FunctionalInterface
public interface WebhookHandler {

    /**
     * Processes an inbound webhook request.
     *
     * @param headers     HTTP request headers as a case-insensitive map; never {@code null}.
     * @param rawBody     Raw UTF-8 request body bytes; never {@code null}.
     */
    void handle(Map<String, String> headers, byte[] rawBody);
}

