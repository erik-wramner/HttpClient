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

import java.nio.charset.Charset;
import java.util.*;

/**
 * This class encapsulates the response from a HTTP request.
 *
 * @author Erik Wramner
 */
public class HttpResponse {
    private final int _httpResponseCode;
    private final Map<HttpHeader, List<String>> _headerMap;
    private final byte[] _body;

    /**
     * Constructor.
     *
     * @param httpResponseCode The HTTP response code.
     * @param headers The response headers.
     * @param body The response body as bytes.
     */
    public HttpResponse(int httpResponseCode, List<HttpHeaderWithValue> headers, byte[] body) {
        _httpResponseCode = httpResponseCode;
        _headerMap = new HashMap<HttpHeader, List<String>>();
        for (HttpHeaderWithValue headerWithValue : headers) {
            List<String> values = _headerMap.get(headerWithValue.getHeader());
            if (values == null) {
                values = new ArrayList<String>();
                _headerMap.put(headerWithValue.getHeader(), values);
            }
            values.add(headerWithValue.getValue());
        }
        _body = body;
    }

    /**
     * Get a header with a given name. The name is not case sensitive. If there are multiple headers with the same name
     * only the first value is returned.
     *
     * @param headerName The name.
     * @return header value or null if missing.
     */
    public String getHeader(String headerName) {
        return getHeader(new HttpHeader(headerName));
    }

    /**
     * Get a header. If there are multiple headers with the same name only the first value is returned.
     *
     * @param header The header.
     * @return header value or null if missing.
     */
    public String getHeader(HttpHeader header) {
        final List<String> values = _headerMap.get(header);
        return values != null ? values.get(0) : null;
    }

    /**
     * Get a list with all header values for a given header. THe name is not case sensitive.
     *
     * @param header The header.
     * @return list with all values, possibly empty.
     */
    public List<String> getHeaders(String header) {
        return getHeaders(new HttpHeader(header));
    }

    /**
     * Get a list with all header values for a given header. THe name is not case sensitive.
     *
     * @param header The header.
     * @return list with all values, possibly empty.
     */
    public List<String> getHeaders(HttpHeader header) {
        List<String> values = _headerMap.get(header);
        if (values == null) {
            return Collections.emptyList();
        }
        return values;
    }

    /**
     * Get HTTP response code.
     *
     * @return response code.
     */
    public int getHttpResponseCode() {
        return _httpResponseCode;
    }

    /**
     * Check if the request was successful. Requests with responses between 200 and 299 are considered successful.
     *
     * @return true if successful.
     */
    public boolean isSuccess() {
        return _httpResponseCode > 199 && _httpResponseCode < 300;
    }

    /**
     * Get response body as raw bytes.
     *
     * @return response body.
     */
    public byte[] getBody() {
        return _body;
    }

    /**
     * Get the response body as text. The correct encoding is determined from the response headers if possible,
     * otherwise the default encoding is used.
     *
     * @return response body.
     * @see HttpClient#HTTP_DEFAULT_CHARSET
     */
    public String getBodyAsText() {
        Charset responseCharset = determineResponseCharset(_headerMap);
        return new String(_body, responseCharset);
    }

    /**
     * Determine the character set used by the response. According to the specification the server should either
     * indicate this with a charset attribute in the content type header OR use the default ISO-8859-1 encoding. Many
     * servers use other encodings anyway, but we'll obey the specification and hope for the best.
     *
     * @param headers The response headers.
     * @return character set.
     */
    private Charset determineResponseCharset(Map<HttpHeader, List<String>> headers) {
        final List<String> contentTypeHeaders = headers.get(HttpHeaders.CONTENT_TYPE);
        if (contentTypeHeaders != null && !contentTypeHeaders.isEmpty()) {
            return HttpClient.extractCharsetFromContentType(contentTypeHeaders.get(0));
        }
        return HttpClient.HTTP_DEFAULT_CHARSET;
    }

    /**
     * Return a human-readable string. As the response body may be quite large it is not included.
     *
     * @return human readable string representation for debugging purposes.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP response code: ").append(_httpResponseCode).append("\n");
        for (HttpHeader header : _headerMap.keySet()) {
            for (String value : _headerMap.get(header)) {
                sb.append(header.getName()).append(": ").append(value).append("\n");
            }
        }
        return sb.toString();
    }
}