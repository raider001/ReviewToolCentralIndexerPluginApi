package com.kalynx.centralindexer.spi;

import java.util.List;
import java.util.Map;

/**
 * Immutable configuration block passed to the provider plugin at startup.
 *
 * <p>Built from the {@code plugin} block in {@code config.json}. The plugin reads
 * provider-specific keys (e.g. {@code "apiToken"}, {@code "webhookSecret"},
 * {@code "baseUrl"}) from {@link #properties()}.
 *
 * @param providerId   Identifier declared by the plugin via {@link ProviderPlugin#providerId()}.
 *                     The indexer validates that this matches the loaded plugin on startup
 *                     and exits with an error if there is a mismatch.
 * @param repositories Ordered, immutable list of canonical repository identifiers
 *                     ({@code owner/repo}) that the plugin is responsible for.
 *                     Used during startup reconciliation and available to the plugin
 *                     for polling configuration.
 * @param properties   Arbitrary key-value pairs carrying provider-specific settings
 *                     such as API base URLs, access tokens, and webhook secrets.
 *                     Never {@code null}; may be empty.
 */
public record ProviderConfig(
        String providerId,
        List<String> repositories,
        Map<String, String> properties
) {

    /**
     * Constructs a {@code ProviderConfig}, enforcing that mandatory fields are non-null
     * and returning defensive copies of the mutable collections.
     */
    public ProviderConfig {
        if (providerId == null || providerId.isBlank()) throw new IllegalArgumentException("providerId must not be blank");
        if (repositories == null) throw new IllegalArgumentException("repositories must not be null");
        if (properties == null) throw new IllegalArgumentException("properties must not be null");
        repositories = List.copyOf(repositories);
        properties = Map.copyOf(properties);
    }
}


