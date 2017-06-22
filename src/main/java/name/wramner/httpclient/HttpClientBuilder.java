/*
 * Copyright 2014 Erik Wramner
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package name.wramner.httpclient;

import javax.net.ssl.SSLSocketFactory;

/**
 * This class produces {@link HttpClient} instances. The builder is not thread safe.
 *
 * @author Erik Wramner
 */
public class HttpClientBuilder {
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;
    private static final int DEFAULT_REQUEST_TIMEOUT_MS = 120000;
    private final String _host;
    private int _port;
    private SSLSocketFactory _sslSocketFactory;
    private int _connectTimeoutMillis;
    private int _requestTimeoutMillis;
    private boolean _use100Continue;
    private boolean _useSsl;
    private String _proxyHost;
    private int _proxyPort;

    /**
     * Constructor.
     *
     * @param host The target host.
     */
    public HttpClientBuilder(String host) {
        _host = host;
        _connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MS;
        _requestTimeoutMillis = DEFAULT_REQUEST_TIMEOUT_MS;
    }

    /**
     * Enable or disable SSL.
     *
     * @param useSsl The flag to use SSL.
     * @return builder.
     */
    public HttpClientBuilder withSsl(boolean useSsl) {
        _useSsl = useSsl;
        return this;
    }

    /**
     * Set the SSL socket factory to use. This is commonly used with {@link TrustingSSLSocketFactoryProvider} in test
     * code in order to accept self-signed and/or weak certificates. That is not recommended for production use, but
     * then this HTTP client has been designed primarily for tests.
     *
     * @param sslSocketFactory The SSL socket factory.
     * @return builder.
     */
    public HttpClientBuilder withSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
        _sslSocketFactory = sslSocketFactory;
        return this;
    }

    /**
     * Wait for the server to send 100 continue before sending the request.
     *
     * @param use100Continue The flag to enable/disable 100 continue.
     * @return builder.
     */
    public HttpClientBuilder expect100Continue(boolean use100Continue) {
        _use100Continue = use100Continue;
        return this;
    }

    /**
     * Set the connection timeout in milliseconds.
     *
     * @param timeout The connection timeout.
     * @return builder.
     */
    public HttpClientBuilder withConnectTimeout(int timeout) {
        _connectTimeoutMillis = timeout;
        return this;
    }

    /**
     * Set the request timeout in milliseconds.
     *
     * @param timeout The request timeout.
     * @return builder.
     */
    public HttpClientBuilder withRequestTimeout(int timeout) {
        _requestTimeoutMillis = timeout;
        return this;
    }

    /**
     * Set the TCP port to connect to. Not needed if using the default port (80 or 443).
     *
     * @param port The port.
     * @return builder.
     */
    public HttpClientBuilder withPort(int port) {
        _port = port;
        return this;
    }

    /**
     * Add a proxy.
     *
     * @param host The proxy host.
     * @param port The proxy port.
     * @return builder.
     */
    public HttpClientBuilder withProxy(String host, int port) {
        _proxyHost = host;
        _proxyPort = port;
        return this;
    }

    /**
     * Get {@link HttpClient}.
     *
     * @return client.
     */
    public HttpClient build() {
        return new HttpClient(_host, getPort(), getSSLSocketFactory(), _connectTimeoutMillis, _requestTimeoutMillis,
                _use100Continue, _proxyHost, _proxyPort);
    }

    /**
     * Get SSL socket factory or null if not using SSL.
     *
     * @return SSL socket factory or null.
     */
    private SSLSocketFactory getSSLSocketFactory() {
        return _sslSocketFactory != null ? _sslSocketFactory
                : (_useSsl ? (SSLSocketFactory) SSLSocketFactory.getDefault() : null);
    }

    /**
     * Get port using the configured value or the default value for the scheme.
     *
     * @return port.
     */
    private int getPort() {
        return _port != 0 ? _port : (_useSsl ? 443 : 80);
    }
}
