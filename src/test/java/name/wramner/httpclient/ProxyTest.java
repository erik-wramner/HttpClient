package name.wramner.httpclient;

import java.net.PasswordAuthentication;

public class ProxyTest {

    public static void main(String[] args) {
        String proxyHost = args[0];
        int proxyPort = Integer.parseInt(args[1]);
        String user = args[2];
        char[] password = args[3].toCharArray();
        String targetHost = args[4];
        HttpClient client = new HttpClientBuilder(targetHost).withProxy(proxyHost, proxyPort).withSsl(true)
                        .withProxyAuthentication(new PasswordAuthentication(user, password), true)
                        .withNtlmAuthenticationHandler(new ApacheHttpClientNtlmAuthenticationHandlerAdapter())
                        .build();
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
