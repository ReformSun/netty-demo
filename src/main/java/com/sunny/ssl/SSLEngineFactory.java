package com.sunny.ssl;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import static java.util.Objects.requireNonNull;

public class SSLEngineFactory {
    private final SSLContext sslContext;

    private final String[] enabledProtocols;

    private final String[] enabledCipherSuites;

    private final boolean clientMode;

    final boolean clientAuthentication;

    public SSLEngineFactory(
            final SSLContext sslContext,
            final String[] enabledProtocols,
            final String[] enabledCipherSuites,
            final boolean clientMode,
            final boolean clientAuthentication) {

        this.sslContext = requireNonNull(sslContext, "sslContext must not be null");
        this.enabledProtocols = requireNonNull(enabledProtocols, "enabledProtocols must not be null");
        this.enabledCipherSuites = requireNonNull(enabledCipherSuites, "cipherSuites must not be null");
        this.clientMode = clientMode;
        this.clientAuthentication = clientAuthentication;
    }

    public SSLEngine createSSLEngine() {
        final SSLEngine sslEngine = sslContext.createSSLEngine();
        configureSSLEngine(sslEngine);
        return sslEngine;
    }

    public SSLEngine createSSLEngine(String hostname, int port) {
        final SSLEngine sslEngine = sslContext.createSSLEngine(hostname, port);
        configureSSLEngine(sslEngine);
        return sslEngine;
    }

    private void configureSSLEngine(SSLEngine sslEngine) {
        sslEngine.setEnabledProtocols(enabledProtocols);
        sslEngine.setEnabledCipherSuites(enabledCipherSuites);
        sslEngine.setUseClientMode(clientMode);
        if (!clientMode) {
            sslEngine.setNeedClientAuth(clientAuthentication);
        }
    }
}
