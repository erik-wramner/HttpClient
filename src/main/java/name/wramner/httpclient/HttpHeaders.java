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
 * Constants for frequently used HTTP headers.
 *
 * @author Erik Wramner
 */
public interface HttpHeaders {
    public static final HttpHeader AUTHORIZATION = new HttpHeader("Authorization");
    public static final HttpHeader ACCEPT_ENCODING = new HttpHeader("Accept-Encoding");
    public static final HttpHeader CONNECTION = new HttpHeader("Connection");
    public static final HttpHeader CONTENT_LENGTH = new HttpHeader("Content-Length");
    public static final HttpHeader CONTENT_TYPE = new HttpHeader("Content-Type");
    public static final HttpHeader COOKIE = new HttpHeader("Cookie");
    public static final HttpHeader EXPECT = new HttpHeader("Expect");
    public static final HttpHeader HOST = new HttpHeader("Host");
}