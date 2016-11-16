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

import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.*;

/**
 * This class helps disable the security checks performed by SSL by default, making it possible to use self-signed
 * certificates. While dangerous in production code, this can be very convenient during development and tests. The
 * checks are not disabled globally, only for specific connections.
 *
 * @author erik.wramner
 */
public class TrustingSSLSocketFactoryProvider {

    /**
     * Get a SSL socket factory that trusts all certificates.
     *
     * @return trusting SSL socket factory.
     */
    public static SSLSocketFactory getTrustingSSLSocketFactory() {
        return SSLSocketFactoryHolder.INSTANCE;
    }

    /**
     * Create a naive SSL socket factory that trusts everyone.
     *
     * @return SSL socket factory or null on errors.
     */
    private static SSLSocketFactory createSSLSocketFactory() {
        TrustManager[] trustManagers = new TrustManager[] { new NaiveTrustManager() };
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[0], trustManagers, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (GeneralSecurityException e) {
            return null;
        }
    }

    /**
     * Singleton holder for SSL socket factory.
     */
    private static interface SSLSocketFactoryHolder {
        public static final SSLSocketFactory INSTANCE = createSSLSocketFactory();
    }

    /**
     * Trust manager that trusts everyone. Note that it uses the new {@link X509ExtendedTrustManager} provided by Java
     * SE 7 in order to accept weak ciphers as well as self-signed certificates.
     */
    private static class NaiveTrustManager extends X509ExtendedTrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // Accept anything
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // Accept anything
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
                throws CertificateException {
            // Accept anything
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                throws CertificateException {
            // Accept anything
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
                throws CertificateException {
            // Accept anything
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                throws CertificateException {
            // Accept anything
        }
    }
}
