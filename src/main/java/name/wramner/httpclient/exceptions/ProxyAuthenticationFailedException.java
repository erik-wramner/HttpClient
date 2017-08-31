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
package name.wramner.httpclient.exceptions;

import java.io.IOException;

/**
 * Thrown when proxy authentication has failed. Clients should avoid trying again as this may lock out the user.
 *
 * @author Erik Wramner
 */
public class ProxyAuthenticationFailedException extends IOException {
    private static final long serialVersionUID = 1L;

    public ProxyAuthenticationFailedException(String message) {
        super(message);
    }

    public ProxyAuthenticationFailedException(Throwable cause) {
        super(cause);
    }

    public ProxyAuthenticationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
