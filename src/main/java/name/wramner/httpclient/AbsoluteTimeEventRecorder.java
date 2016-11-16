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

import name.wramner.httpclient.HttpClient.Event;

/**
 * Event recorder that records the time for each event in milliseconds. It is not thread safe.
 *
 * @author Erik Wramner
 */
public class AbsoluteTimeEventRecorder implements EventRecorder {
    private final long[] _eventTimes = new long[Event.values().length];

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordEvent(Event event) {
        _eventTimes[event.ordinal()] = System.currentTimeMillis();
    }

    /**
     * Get the time when an event occurred or 0 if it has not happened.
     *
     * @param event The event.
     * @return time as reported by {@link System#currentTimeMillis()} for event or 0.
     */
    public long getEventTimeMillis(Event event) {
        return _eventTimes[event.ordinal()];
    }

    /**
     * Get the time in milliseconds for establishing TCP connection.
     *
     * @return connection time or null.
     */
    public Long getConnectionTime() {
        long endTime = _eventTimes[Event.CONNECTED.ordinal()];
        return endTime != 0 ? Long.valueOf(endTime - _eventTimes[Event.CONNECTING.ordinal()]) : null;
    }

    /**
     * Get the time in milliseconds for the SSL handshake if using SSL.
     *
     * @return SSL handshake time or null.
     */
    public Long getSslHandshakeTime() {
        long endTime = _eventTimes[Event.SSL_HANDSHAKE_COMPLETE.ordinal()];
        return endTime != 0 ? Long.valueOf(endTime - _eventTimes[Event.CONNECTED.ordinal()]) : null;
    }

    /**
     * Get the time in milliseconds for sending the request.
     *
     * @return time for sending request or null.
     */
    public Long getSendRequestTime() {
        long endTime = _eventTimes[Event.SENT_REQUEST.ordinal()];
        return endTime != 0 ? Long.valueOf(endTime - _eventTimes[Event.SENDING_REQUEST.ordinal()]) : null;
    }

    /**
     * Get the total time. This should be valid for successful and failed requests alike.
     *
     * @return time in milliseconds.
     */
    public long getTotalTime() {
        int i = _eventTimes.length - 1;
        while (i > 0 && _eventTimes[i] == 0L) {
            i--;
        }
        return i > 0 ? _eventTimes[i] - _eventTimes[0] : 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Event e : Event.values()) {
            if (e.ordinal() > 0) {
                sb.append(", ");
            }
            sb.append(e.name()).append(": ").append(_eventTimes[e.ordinal()]);
        }
        return sb.toString();
    }
}
