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
package name.wramner.httpclient.exceptions;

import java.io.IOException;
import java.util.List;

/**
 * Exception thrown when failing to connect through a proxy server as it requires authentication.
 */
public class ProxyAuthenticationRequiredException extends IOException {
    private static final long serialVersionUID = 1L;
    private final List<String> _proxyAuthHeaders;

    /**
     * Constructor.
     *
     * @param message The message.
     * @param proxyAuthHeaders The proxy authentication header values with supported methods.
     */
    public ProxyAuthenticationRequiredException(String message, List<String> proxyAuthHeaders) {
        super(message);
        _proxyAuthHeaders = proxyAuthHeaders;
    }

    /**
     * Get the Proxy-Authenticate headers returned by the proxy server with supported methods.
     *
     * @return list with header values.
     */
    public List<String> getProxyAuthenticationHeaders() {
        return _proxyAuthHeaders;
    }
}
