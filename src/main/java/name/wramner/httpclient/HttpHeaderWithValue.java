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

/**
 * This class keeps track of a HTTP header and an associated value for a HTTP request or response.
 *
 * @author Erik Wramner
 */
public class HttpHeaderWithValue {
    private final HttpHeader _header;
    private final String _value;

    /**
     * Constructor.
     *
     * @param header The header.
     * @param value The value.
     */
    public HttpHeaderWithValue(HttpHeader header, String value) {
        _header = header;
        _value = value;
    }

    /**
     * Get header.
     *
     * @return header.
     */
    public HttpHeader getHeader() {
        return _header;
    }

    /**
     * Get value.
     *
     * @return value.
     */
    public String getValue() {
        return _value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s: %s\r\n", _header.getName(), _value);
    }
}