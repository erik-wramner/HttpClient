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

import java.util.Locale;

/**
 * A HTTP header represents the header name for a header in a HTTP request or response. For comparisons the header name
 * is case insensitive, but the original case is preserved.
 *
 * @author Erik Wramner
 */
public class HttpHeader implements Comparable<HttpHeader> {
    private final String _name;
    private final String _nameLowerCase;

    /**
     * Constructor.
     *
     * @param headerName The header name.
     */
    public HttpHeader(String headerName) {
        _name = headerName;
        _nameLowerCase = headerName.toLowerCase(Locale.ENGLISH);
    }

    /**
     * Get this header with a value.
     *
     * @param value The value.
     * @return header with value.
     */
    public HttpHeaderWithValue withValue(String value) {
        return new HttpHeaderWithValue(this, value);
    }

    /**
     * Get the header name using the original case.
     *
     * @return name.
     */
    public String getName() {
        return _name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return _nameLowerCase.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HttpHeader other = (HttpHeader) obj;
        if (_nameLowerCase == null) {
            if (other._nameLowerCase != null)
                return false;
        } else if (!_nameLowerCase.equals(other._nameLowerCase))
            return false;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(HttpHeader o) {
        return _nameLowerCase.compareTo(o._nameLowerCase);
    }
}