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

import name.wramner.httpclient.exceptions.NtlmAuthenticationException;

/**
 * This interface makes it possible to supply the {@link HttpClient} with an NTLM implementation for authenticating with
 * a proxy (or by all means a web server) using the NTLM over HTTP protocol as defined in MS-NTHT and MS-NLMP.
 * <p>
 * The protocol uses a handshake mechanism with three message types. First the client sends a negotiate message (type 1)
 * to the server. The server responds with a challenge message (type 2). The client finally generates an authenticate
 * message (type 3) based on the challenge and possibly also on data from the negotiate message.
 * <p>
 * Unfortunately it is quite difficult to write a good NTLM implementation as there are so many variations. Testing it
 * with all the different servers it may encounter is a huge undertaking. For that reason an existing implementation can
 * be passed in with an adapter instead, for example jCIFS or Apache HTTP client.
 * <p>
 * Combining this HTTP client with another one may seem odd, but generating the NTLM messages is a limited task with no
 * effect on socket or thread management and the dependency is not explicit, so only code that must use an NTLM proxy
 * will have to deal with it. Overall it seems like an acceptable compromise.
 * <p>
 * The handler is allowed to keep state. The same instance is guaranteed to be used for all messages in a conversation.
 *
 * @author Erik Wramner
 */
public interface NtlmAuthenticationHandler {

    /**
     * Generate a negotiate or type 1 message.
     * 
     * @param domain The domain or null.
     * @param workstation The workstation name or null.
     * @return base64-encoded negotiate message.
     * @throws NtlmAuthenticationException on errors.
     */
    String generateNegotiateMessage(String domain, String workstation) throws NtlmAuthenticationException;

    /**
     * Generate an authentication or type 3 message.
     * 
     * @param userName The user name.
     * @param password The user's password.
     * @param challenge The base64-encoded challenge or type 2 message from the server.
     * @return base64-encoded authentication message.
     * @throws NtlmAuthenticationException on errors.
     */
    String generateAuthenticationMessage(String userName, char[] password, String challenge)
                    throws NtlmAuthenticationException;
}