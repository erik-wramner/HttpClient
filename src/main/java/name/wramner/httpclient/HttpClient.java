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

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * This is a light-weight HTTP client without external dependencies and with support for detailed time recording. It is
 * not as powerful as the Apache HTTP client or even as the built-in HTTP support, but it does offer full control over
 * connections and threads.
 *
 * @author Erik Wramner
 */
public class HttpClient {
    private static final int RECEIVE_BUFFER_SIZE = 1024;
    private static final String CRLF = "\r\n";

    /**
     * According to the HTTP 1.1 standard the header fields may be encoded in ISO-8859-1, though ideally they should
     * stick to US ASCII. The body should use ISO-8859-1 unless a specific character set has been specified.
     */
    private static final Charset HTTP_HEADER_CHARSET = Charset.forName("ISO-8859-1");

    /**
     * According to the HTTP 1.1 standard the request and response body should be encoded in ISO-8859-1 unless a
     * specific encoding has been specified using the content type charset parameter.
     */
    public static final Charset HTTP_DEFAULT_CHARSET = Charset.forName("ISO-8859-1");

    /**
     * Request headers that the client wants to control. These will be ignored if specified by the caller.
     */
    private static final Set<HttpHeader> RESERVED_HEADERS = new HashSet<HttpHeader>(
                    Arrays.asList(new HttpHeader[] { HttpHeaders.CONTENT_LENGTH, HttpHeaders.ACCEPT_ENCODING,
                                    HttpHeaders.CONNECTION, HttpHeaders.EXPECT, HttpHeaders.HOST }));

    private final String _host;
    private final int _port;
    private final SSLSocketFactory _sslSocketFactory;
    private final int _connectTimeoutMillis;
    private final int _requestTimeoutMillis;
    private final boolean _use100Continue;
    private final String _proxyHost;
    private final int _proxyPort;
    private final PasswordAuthentication _proxyAuthentication;

    /**
     * Constructor.
     *
     * @param host The remote host.
     * @param port The port.
     * @param sslSocketFactory The SSL socket factory or null for no SSL.
     * @param connectTimeoutMillis The connection timeout.
     * @param requestTimeoutMillis The read (or request) timeout.
     * @param use100Continue The flag to expect 100-continue before sending request body or not.
     * @param proxyHost The proxy host or null for no proxy.
     * @param proxyPort The proxy port.
     * @param proxyAuthentication The optional proxy user and password.
     */
    HttpClient(String host, int port, SSLSocketFactory sslSocketFactory, int connectTimeoutMillis,
                    int requestTimeoutMillis, boolean use100Continue, String proxyHost, int proxyPort,
                    PasswordAuthentication proxyAuthentication) {
        _host = host;
        _port = port;
        _sslSocketFactory = sslSocketFactory;
        _connectTimeoutMillis = connectTimeoutMillis;
        _requestTimeoutMillis = requestTimeoutMillis;
        _use100Continue = use100Continue;
        _proxyHost = proxyHost;
        _proxyPort = proxyPort;
        _proxyAuthentication = proxyAuthentication;
    }

    /**
     * Send a request and return the response.
     *
     * @param method The request method.
     * @param url The URL, excluding scheme and host and port.
     * @param body The request body.
     * @param headers The custom HTTP headers if any.
     * @return response.
     * @throws IOException on network errors.
     */
    public HttpResponse sendRequest(HttpRequestMethod method, String url, HttpRequestBody body,
                    HttpHeaderWithValue... headers) throws IOException {
        return sendRequest(EventRecorder.NULL_RECORDER, method, url, body, headers);
    }

    /**
     * Send a request and return the response using the provided {@link EventRecorder} for instrumentation.
     *
     * @param eventRecorder The event recorder for logging.
     * @param method The request method.
     * @param url The URL, excluding scheme and host and port.
     * @param body The request body.
     * @param requestHeaders The custom HTTP headers if any.
     * @return response.
     * @throws IOException on network errors.
     */
    public HttpResponse sendRequest(EventRecorder eventRecorder, HttpRequestMethod method, String url,
                    HttpRequestBody body, HttpHeaderWithValue... requestHeaders) throws IOException {
        eventRecorder.recordEvent(Event.ENTER_SEND_REQUEST);
        Socket socket = null;
        try {
            long deadlineMillis = System.currentTimeMillis() + _requestTimeoutMillis;
            socket = connectToHost(eventRecorder);

            StringBuilder sb = new StringBuilder();
            sb.append(method.name()).append(' ').append(url).append(" HTTP/1.1");
            sb.append(CRLF);
            byte[] requestBodyBytes = body.getBytes();
            appendRequestHeaders(sb, requestBodyBytes.length, requestHeaders);
            sendRequest(eventRecorder, socket, sb.toString().getBytes(HTTP_HEADER_CHARSET), requestBodyBytes);
            return readResponse(eventRecorder, socket, deadlineMillis);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
            eventRecorder.recordEvent(Event.EXIT_SEND_REQUEST);
        }
    }

    private HttpResponse readResponse(EventRecorder eventRecorder, Socket socket, long deadlineMillis)
                    throws IOException {
        eventRecorder.recordEvent(Event.READING_RESPONSE);
        byte[] buffer = new byte[RECEIVE_BUFFER_SIZE];
        InputStream in = socket.getInputStream();
        int totalRead = 0;
        int bodyPosition = 0;
        while (totalRead < buffer.length && bodyPosition == 0) {
            updateSocketTimeout(socket, deadlineMillis);
            int read = in.read(buffer, totalRead, buffer.length - totalRead);
            if (read == -1) {
                throw new EOFException("Unexpected end of response after " + totalRead + " bytes");
            } else if (read > 0) {
                totalRead += read;
                bodyPosition = findBodyPosition(buffer, totalRead);
            }
        }

        if (bodyPosition == 0) {
            throw new IOException("More than " + buffer.length + " bytes read before body!");
        }

        int endOfStatusLine = findEndOfLine(buffer, 0, bodyPosition);
        if (endOfStatusLine == 0) {
            throw new IllegalStateException("Found CRLFCRLF but not CRLF!?!");
        }
        int httpResponseCode = parseHttpStatusCode(buffer, endOfStatusLine);

        ByteArrayOutputStream bodyOutputStream = new ByteArrayOutputStream();
        if (bodyPosition < totalRead) {
            bodyOutputStream.write(buffer, bodyPosition, totalRead - bodyPosition);
        }

        List<HttpHeaderWithValue> responseHeaders = parseHeaders(buffer, endOfStatusLine + 2, bodyPosition);
        Integer contentLength = findContentLength(responseHeaders);
        if (contentLength != null) {
            // Read until content-length body bytes have been read
            int remainingContentLength = contentLength.intValue() - (totalRead - bodyPosition);
            while (remainingContentLength > 0) {
                updateSocketTimeout(socket, deadlineMillis);
                int read = in.read(buffer, 0, Math.min(buffer.length, remainingContentLength));
                if (read == -1) {
                    throw new EOFException("Partial response, " + remainingContentLength + " bytes missing");
                } else if (read == 0) {
                    throw new SocketTimeoutException("Timeout reading response body");
                }
                bodyOutputStream.write(buffer, 0, read);
                remainingContentLength -= read;
            }
        } else {
            // Read until end of file
            for (;;) {
                updateSocketTimeout(socket, deadlineMillis);
                int read = in.read(buffer, 0, buffer.length);
                if (read == -1) {
                    // Done!
                    break;
                } else if (read == 0) {
                    throw new SocketTimeoutException("Timeout reading response body");
                }
                bodyOutputStream.write(buffer, 0, read);
            }
        }
        eventRecorder.recordEvent(Event.READ_RESPONSE);
        return new HttpResponse(httpResponseCode, responseHeaders, bodyOutputStream.toByteArray());
    }

    private Integer findContentLength(List<HttpHeaderWithValue> responseHeaders) {
        for (HttpHeaderWithValue headerWithValue : responseHeaders) {
            if (HttpHeaders.CONTENT_LENGTH.equals(headerWithValue.getHeader())) {
                return Integer.valueOf(headerWithValue.getValue());
            }
        }
        return null;
    }

    private void appendRequestHeaders(StringBuilder sb, int contentLength, HttpHeaderWithValue... requestHeaders) {
        List<HttpHeaderWithValue> requestHeaderList = new ArrayList<HttpHeaderWithValue>();
        for (HttpHeaderWithValue headerWithValue : requestHeaders) {
            if (!RESERVED_HEADERS.contains(headerWithValue.getHeader())) {
                requestHeaderList.add(headerWithValue);
            }
        }
        requestHeaderList.add(HttpHeaders.CONTENT_LENGTH.withValue(String.valueOf(contentLength)));
        // We don't want to handle compressed data for now
        requestHeaderList.add(HttpHeaders.ACCEPT_ENCODING.withValue("identity"));
        // No persistent connections
        requestHeaderList.add(HttpHeaders.CONNECTION.withValue("close"));
        requestHeaderList.add(HttpHeaders.HOST.withValue(_host + ":" + _port));
        if (_use100Continue) {
            requestHeaderList.add(HttpHeaders.EXPECT.withValue("100-continue"));
        }
        for (HttpHeaderWithValue headerWithValue : requestHeaderList) {
            sb.append(headerWithValue.getHeader().getName()).append(": ");
            sb.append(headerWithValue.getValue());
            sb.append(CRLF);
        }
        sb.append(CRLF);
    }

    private int parseHttpStatusCode(byte[] buffer, int endOfStatusLine) throws IOException {
        String statusLine = new String(buffer, 0, endOfStatusLine, HTTP_HEADER_CHARSET);
        String[] statusFields = statusLine.split(" ");
        if (statusFields.length >= 3 && statusFields[0].startsWith("HTTP/")) {
            return Integer.parseInt(statusFields[1]);
        } else {
            throw new IOException("Invalid HTTP response status line: " + statusLine);
        }
    }

    /**
     * Send request to remote server.
     *
     * @param eventRecorder The event recorder for statistics.
     * @param socket The connected socket.
     * @param requestHeader The request headers as bytes.
     * @param requestBody The request body as bytes.
     *
     * @throws IOException on I/O errors.
     */
    private void sendRequest(EventRecorder eventRecorder, Socket socket, byte[] requestHeader, byte[] requestBody)
                    throws IOException {
        eventRecorder.recordEvent(Event.SENDING_REQUEST);
        OutputStream out = socket.getOutputStream();
        out.write(requestHeader);
        if (_use100Continue) {
            out.flush();
            eventRecorder.recordEvent(Event.SENT_HEADERS_WAITING_FOR_100_CONTINUE);
            Thread.yield();
            waitFor100Continue(socket);
            eventRecorder.recordEvent(Event.RECEIVED_100_CONTINUE);
        }
        out.write(requestBody);
        out.flush();
        eventRecorder.recordEvent(Event.SENT_REQUEST);
    }

    /**
     * Wait for the server to send 100 continue. The implementation is inefficient, but keeps the rest of the code
     * simple and this part is not performance-critical.
     *
     * @param socket The socket.
     * @throws IOException on IO errors.
     */
    private void waitFor100Continue(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        boolean foundCr = false;
        boolean found100Continue = false;
        for (int c = in.read();; c = in.read()) {
            if (c == -1) {
                throw new EOFException("End of file waiting for 100-continue!");
            } else if (c == '\r') {
                foundCr = true;
            } else if (c == '\n') {
                if (foundCr) {
                    foundCr = false;
                    byte[] lineAsBytes = byteStream.toByteArray();
                    if (lineAsBytes.length == 0) {
                        if (found100Continue) {
                            break;
                        } else {
                            throw new IOException("Unexpected blank line before 100 continue!");
                        }
                    } else {
                        String line = new String(lineAsBytes, HTTP_HEADER_CHARSET);
                        if (line.startsWith("HTTP/1.1 100")) {
                            byteStream = new ByteArrayOutputStream();
                            found100Continue = true;
                        } else {
                            throw new IOException("Unexpected response waiting for 100 continue: " + line);
                        }
                    }
                }
            } else {
                foundCr = false;
                byteStream.write(c);
            }
        }
    }

    /**
     * Update the socket timeout with a new value, fail on timeout.
     *
     * @param socket The socket.
     * @param deadlineMillis The deadline in milliseconds.
     * @throws SocketTimeoutException on timeout.
     * @throws SocketException on errors.
     */
    private void updateSocketTimeout(Socket socket, long deadlineMillis)
                    throws SocketTimeoutException, SocketException {
        long remainingTimeMillis = deadlineMillis - System.currentTimeMillis();
        if (remainingTimeMillis <= 0) {
            throw new SocketTimeoutException("Request timed out");
        } else {
            socket.setSoTimeout((int) remainingTimeMillis);
        }
    }

    private List<HttpHeaderWithValue> parseHeaders(byte[] buffer, int startPos, int endPos) {
        List<HttpHeaderWithValue> responseHeaders = new ArrayList<HttpHeaderWithValue>();
        int lineStartPos = startPos;
        for (int lineEndPos = 1; lineEndPos < endPos; lineEndPos++) {
            if (buffer[lineEndPos] == '\n' && buffer[lineEndPos - 1] == '\r') {
                if (lineEndPos > lineStartPos + 1) {
                    String headerLine = new String(buffer, lineStartPos, lineEndPos - 1 - lineStartPos,
                                    HTTP_HEADER_CHARSET);
                    int separatorPos = headerLine.indexOf(":");
                    if (separatorPos == -1) {
                        responseHeaders.add(new HttpHeader(headerLine).withValue(""));
                    } else if (separatorPos > 0) {
                        String headerKey = headerLine.substring(0, separatorPos).toLowerCase();
                        String headerValue = (separatorPos < headerLine.length()
                                        ? headerLine.substring(separatorPos + 1) : "").trim();
                        responseHeaders.add(new HttpHeader(headerKey).withValue(headerValue));
                    }
                }
                lineStartPos = lineEndPos + 1;
            }
        }
        return responseHeaders;
    }

    private static int findEndOfLine(byte[] buffer, int startPos, int endPos) {
        for (int pos = startPos; pos < endPos - 1; pos++) {
            if (buffer[pos] == '\r' && buffer[pos + 1] == '\n') {
                return pos;
            }
        }
        return -1;
    }

    private static int findBodyPosition(byte[] buffer, int endPos) {
        int bodyPos = 0;
        while (bodyPos + 3 < endPos) {
            if (buffer[bodyPos] == '\r' && buffer[bodyPos + 1] == '\n' && buffer[bodyPos + 2] == '\r'
                            && buffer[bodyPos + 3] == '\n')
                return bodyPos + 4;
            bodyPos++;
        }
        return 0;
    }

    /**
     * Determine the character set from the specified content type header and fall back to the default HTTP character
     * set (ISO-8859-1) if the content type is missing or unsupported.
     * 
     * @param contentType The content type header.
     * @return character set.
     */
    public static Charset extractCharsetFromContentType(String contentType) {
        int charsetIndex = contentType.indexOf("charset");
        if (charsetIndex != -1) {
            int startIndex = contentType.indexOf("=", charsetIndex) + 1;
            if (startIndex > charsetIndex) {
                int nextCommaIndex = contentType.indexOf(",", startIndex);
                int nextSemiColonIndex = contentType.indexOf(";", startIndex);
                int endIndex = (nextCommaIndex > startIndex && nextSemiColonIndex > startIndex)
                                ? Math.min(nextCommaIndex, nextSemiColonIndex)
                                : (((nextCommaIndex > startIndex) ? nextCommaIndex
                                                : ((nextSemiColonIndex > startIndex) ? nextSemiColonIndex
                                                                : contentType.length())));
                String charsetName = contentType.substring(startIndex, endIndex);
                try {
                    return Charset.forName(charsetName);
                } catch (UnsupportedCharsetException e) {
                    return HTTP_DEFAULT_CHARSET;
                }
            }
        }
        return HTTP_DEFAULT_CHARSET;
    }

    /**
     * Connect to target host directly or through proxy and complete the SSL handshake if using SSL.
     *
     * @param recorder The event recorder.
     * @return connected socket.
     * @throws IOException on errors.
     */
    private Socket connectToHost(EventRecorder recorder) throws IOException {
        recorder.recordEvent(Event.CONNECTING);
        Socket nonSslSocket = (_proxyHost == null) ? connect(_host, _port) : connectThroughProxy(recorder);
        recorder.recordEvent(Event.CONNECTED);

        if (_sslSocketFactory != null) {
            Socket socketToClose = nonSslSocket;
            try {
                SSLSocket sslSocket = (SSLSocket) _sslSocketFactory.createSocket(nonSslSocket, _host, _port, true);
                sslSocket.setUseClientMode(true);
                sslSocket.startHandshake();
                recorder.recordEvent(Event.SSL_HANDSHAKE_COMPLETE);
                socketToClose = null;
                return sslSocket;
            } finally {
                if (socketToClose != null) {
                    socketToClose.close();
                }
            }
        } else {
            return nonSslSocket;
        }
    }

    /**
     * Connect to the given host and port and configure the socket.
     *
     * @param host The host.
     * @param port The port.
     * @return connected socket.
     * @throws IOException on errors.
     */
    private Socket connect(String host, int port) throws IOException {
        Socket socketToClose = null;
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), _connectTimeoutMillis);
            socketToClose = socket;
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(false);
            socket.setSoTimeout(_requestTimeoutMillis);
            socketToClose = null;
            return socket;
        } finally {
            if (socketToClose != null) {
                socketToClose.close();
            }
        }
    }

    /**
     * Connect to the target host with a tunnel through the configured proxy server.
     * 
     * @param recorder The event recorder.
     * @return socket tunneling to the target host.
     * @throws IOException on errors.
     */
    private Socket connectThroughProxy(EventRecorder recorder) throws IOException {
        Socket socketToClose = null;
        try {
            Socket socket = connect(_proxyHost, _proxyPort);
            socketToClose = socket;
            recorder.recordEvent(Event.CONNECTED_PROXY);

            socket.getOutputStream().write(createProxyConnectRequest().getBytes(HTTP_HEADER_CHARSET));
            HttpResponse resp = readProxyConnectResponse(socket, _requestTimeoutMillis + System.currentTimeMillis());

            if (resp.isSuccess()) {
                socketToClose = null;
                return socket;
            } else if (resp.getHttpResponseCode() == 407) {
                socket.close();
                socketToClose = null;

                List<String> proxyAuthHeaders = resp.getHeaders(HttpHeaders.PROXY_AUTHENTICATE);
                if (_proxyAuthentication != null) {
                    if (proxyAuthHeaders.contains("NTLM")) {
                        return connectThroughProxyWithNtlmAuthentication(recorder);
                    } else if (proxyAuthHeaders.stream().anyMatch(s -> s.startsWith("Basic"))) {
                        return connectThroughProxyWithBasicAuthentication(recorder);
                    }
                }

                throw new ProxyAuthenticationRequiredException("Failed to connect through " + _proxyHost + ":"
                                + _proxyPort + ", authentication required", proxyAuthHeaders);
            } else {
                throw new ProxyProtocolException("Failed to connect through " + _proxyHost + ":" + _proxyPort
                                + ", error " + resp.getHttpResponseCode());
            }
        } finally {
            if (socketToClose != null) {
                socketToClose.close();
            }
        }
    }

    /**
     * Connect to the specified proxy server and send a CONNECT command along with base64-coded basic authentication.
     * 
     * @param recorder The event recorder.
     * @return connected socket tunneling to the target host.
     * @throws IOException on errors.
     */
    private Socket connectThroughProxyWithBasicAuthentication(EventRecorder recorder) throws IOException {
        Socket socketToClose = null;
        try {
            Socket socket = connect(_proxyHost, _proxyPort);
            socketToClose = socket;
            socket.getOutputStream()
                            .write(createProxyConnectRequest("Proxy-Authorization: Basic " + Base64.getEncoder()
                                            .encodeToString((_proxyAuthentication.getUserName() + ":"
                                                            + new String(_proxyAuthentication.getPassword()))
                                                                            .getBytes(HTTP_HEADER_CHARSET))).getBytes(
                                                                                            HTTP_HEADER_CHARSET));
            HttpResponse resp = readProxyConnectResponse(socket, _requestTimeoutMillis + System.currentTimeMillis());
            if (resp.isSuccess()) {
                recorder.recordEvent(Event.AUTHENTICATED_PROXY);
                socketToClose = null;
                return socket;
            } else {
                throw new ProxyProtocolException("Failed to connect through " + _proxyHost + ":" + _proxyPort
                                + ", error " + resp.getHttpResponseCode());
            }
        } finally {
            if (socketToClose != null) {
                socketToClose.close();
            }
        }
    }

    /**
     * Connect to the specified proxy server, authenticate using NTLM and send a CONNECT message.
     * 
     * @param recorder The event recorder.
     * @return connected socket tunneling to the target host.
     * @throws IOException on errors.
     */
    private Socket connectThroughProxyWithNtlmAuthentication(EventRecorder recorder) throws IOException {
        Socket socketToClose = null;
        try {
            NtlmHelper ntlmHelper = new NtlmHelper();
            Socket socket = connect(_proxyHost, _proxyPort);
            socketToClose = socket;
            socket.getOutputStream()
                            .write(createProxyConnectRequest("Proxy-Authorization: NTLM "
                                            + ntlmHelper.createNtlmNegotiateMessage(null, null))
                                                            .getBytes(HTTP_HEADER_CHARSET));
            HttpResponse resp = readProxyConnectResponse(socket, _requestTimeoutMillis + System.currentTimeMillis());

            if (resp.getHttpResponseCode() == 407) {
                String encodedChallenge = resp.getHeaders(HttpHeaders.PROXY_AUTHENTICATE).stream()
                                .filter(h -> h.startsWith("NTLM ")).findFirst().map(h -> h.substring(5).trim())
                                .orElse("");
                if (!encodedChallenge.isEmpty()) {
                    // TODO: test code to parse the response, then we need to send a new message
                    ntlmHelper.parseNtlmChallengeMessage(encodedChallenge);
                }
                throw new ProxyProtocolException("Failed to connect through " + _proxyHost + ":" + _proxyPort
                                + ", error " + resp.getHttpResponseCode());
            }
            if (resp.isSuccess()) {
                recorder.recordEvent(Event.AUTHENTICATED_PROXY);
                socketToClose = null;
                return socket;
            } else {
                throw new ProxyProtocolException("Failed to connect through " + _proxyHost + ":" + _proxyPort
                                + ", error " + resp.getHttpResponseCode());
            }
        } finally {
            if (socketToClose != null) {
                socketToClose.close();
            }
        }
    }

    private String createProxyConnectRequest(String... headers) {
        StringBuilder sb = new StringBuilder();
        sb.append("CONNECT ").append(_host).append(':').append(_port).append(" HTTP/1.1").append(CRLF);
        sb.append("Host: ").append(_host).append(':').append(_port).append(CRLF);
        sb.append("Proxy-Connection: keep-alive").append(CRLF);
        sb.append("User-Agent: HttpClient").append(CRLF);
        for (String header : headers) {
            sb.append(header).append(CRLF);
        }
        sb.append(CRLF);
        return sb.toString();
    }

    private HttpResponse readProxyConnectResponse(Socket socket, long deadlineMillis) throws IOException {
        byte[] buffer = new byte[RECEIVE_BUFFER_SIZE];
        InputStream in = socket.getInputStream();
        int totalRead = 0;
        int bodyPosition = 0;
        while (totalRead < buffer.length && bodyPosition == 0) {
            updateSocketTimeout(socket, deadlineMillis);
            int read = in.read(buffer, totalRead, buffer.length - totalRead);
            if (read == -1) {
                throw new EOFException("Unexpected end of response after " + totalRead + " bytes");
            } else if (read > 0) {
                totalRead += read;
                bodyPosition = findBodyPosition(buffer, totalRead);
            }
        }

        if (bodyPosition == 0) {
            throw new IOException("More than " + buffer.length + " bytes read before body!");
        }

        int endOfStatusLine = findEndOfLine(buffer, 0, bodyPosition);
        if (endOfStatusLine == 0) {
            throw new IllegalStateException("Found CRLFCRLF but not CRLF!?!");
        }
        int httpResponseCode = parseHttpStatusCode(buffer, endOfStatusLine);
        List<HttpHeaderWithValue> responseHeaders = parseHeaders(buffer, endOfStatusLine + 2, bodyPosition);
        return new HttpResponse(httpResponseCode, responseHeaders, new byte[0]);
    }

    /**
     * Events logged to the event recorder for a request.
     */
    public enum Event {
        ENTER_SEND_REQUEST, //
        CONNECTING, //
        CONNECTED_PROXY, //
        AUTHENTICATED_PROXY, //
        CONNECTED, //
        SSL_HANDSHAKE_COMPLETE, //
        SENDING_REQUEST, //
        SENT_HEADERS_WAITING_FOR_100_CONTINUE, //
        RECEIVED_100_CONTINUE, //
        SENT_REQUEST, //
        READING_RESPONSE, //
        READ_RESPONSE, //
        EXIT_SEND_REQUEST
    }
}
