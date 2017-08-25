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

import java.net.PasswordAuthentication;

/**
 * Simple example class that invokes the HTTP client in order to test proxy support manually.
 * 
 * @author Erik Wramner
 */
public class ProxyTest {

    public static void main(String[] args) {
        String proxyHost = args[0];
        int proxyPort = Integer.parseInt(args[1]);
        String user = args[2];
        char[] password = args[3].toCharArray();
        String targetHost = args[4];

        HttpClient client = new HttpClientBuilder(targetHost).withProxy(proxyHost, proxyPort).withSsl(true)
                        .withProxyAuthentication(new PasswordAuthentication(user, password), true)
                        .withNtlmAuthenticationHandler(new ApacheHttpClientNtlmAuthenticationHandlerAdapter()).build();
        ElapsedTimeEventRecorder recorder = new ElapsedTimeEventRecorder();
        try {
            HttpResponse resp = client.sendRequest(recorder, HttpRequestMethod.GET, "/", HttpRequestBody.EMPTY);
            System.out.println(resp.toString());
            System.out.println(recorder.getEvents());
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
