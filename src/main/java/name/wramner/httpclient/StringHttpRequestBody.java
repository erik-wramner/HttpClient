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

/**
 * HTTP request body for strings.
 *
 * @author Erik Wramner
 */
public class StringHttpRequestBody extends ByteArrayHttpRequestBody {

    /**
     * Constructor with request body and character set.
     *
     * @param body The string.
     * @param charset The character set.
     */
    public StringHttpRequestBody(String body, Charset charset) {
        super(body.getBytes(charset));
    }

    /**
     * Constructor with request body using default encoding.
     *
     * @param body The string.
     * @see HttpClient#HTTP_DEFAULT_CHARSET
     */
    public StringHttpRequestBody(String body) {
        this(body, HttpClient.HTTP_DEFAULT_CHARSET);
    }
}