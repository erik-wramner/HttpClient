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

import java.lang.reflect.Constructor;

import org.apache.http.impl.auth.NTLMEngine;
import org.apache.http.impl.auth.NTLMEngineException;

import name.wramner.httpclient.exceptions.NtlmAuthenticationException;

/**
 * Example adapter that illustrates how the NTLM authentication in the Apache HTTP client can be used. The
 * implementation is quite dirty as it relies on a package-private implementation class and might break with Java 9, but
 * it illustrates how it can be done.
 * 
 * @author Erik Wramner
 */
public class ApacheHttpClientNtlmAuthenticationHandlerAdapter implements NtlmAuthenticationHandler {
    private final NTLMEngine _ntlmEngine;
    private String _domain;
    private String _workstation;

    public ApacheHttpClientNtlmAuthenticationHandlerAdapter() {
        NTLMEngine engine;
        try {
            Class<?> cls = Class.forName("org.apache.http.impl.auth.NTLMEngineImpl");
            Constructor<?> constructor = cls.getDeclaredConstructor();
            constructor.setAccessible(true);
            engine = (NTLMEngine) constructor.newInstance();
        } catch (final Exception e) {
            // Failed to construct instance, NTLM authentication will not be available
            engine = new NTLMEngine() {
                @Override
                public String generateType3Msg(String username, String password, String domain, String workstation,
                                String challenge) throws NTLMEngineException {
                    throw new NTLMEngineException("Failed to construct NTLMEngineImpl", e);
                }

                @Override
                public String generateType1Msg(String domain, String workstation) throws NTLMEngineException {
                    throw new NTLMEngineException("Failed to construct NTLMEngineImpl", e);
                }
            };
        }
        _ntlmEngine = engine;
    }

    @Override
    public String generateNegotiateMessage(String domain, String workstation) throws NtlmAuthenticationException {
        try {
            _domain = domain;
            _workstation = workstation;
            return _ntlmEngine.generateType1Msg(domain, workstation);
        } catch (NTLMEngineException e) {
            throw new NtlmAuthenticationException(e);
        }
    }

    @Override
    public String generateAuthenticationMessage(String userName, char[] password, String challenge)
                    throws NtlmAuthenticationException {
        try {
            return _ntlmEngine.generateType3Msg(userName, new String(password), _domain, _workstation, challenge);
        } catch (NTLMEngineException e) {
            throw new NtlmAuthenticationException(e);
        }
    }
}
