package name.wramner.httpclient;

import java.lang.reflect.Constructor;

import org.apache.http.impl.auth.NTLMEngine;
import org.apache.http.impl.auth.NTLMEngineException;

import name.wramner.httpclient.exceptions.NtlmAuthenticationException;

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
