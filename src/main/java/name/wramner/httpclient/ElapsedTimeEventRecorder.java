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

import java.util.ArrayList;
import java.util.List;

/**
 * Simple event recorder that records events along with the elapsed time in nanoseconds since the previous event. This
 * recorder is not thread safe.
 *
 * @author Erik Wramner
 */
public class ElapsedTimeEventRecorder implements EventRecorder {
    private long _prevEventTimeNanos = System.nanoTime();
    private final List<ElapsedTimeEventRecorder.TimedEvent> _events = new ArrayList<ElapsedTimeEventRecorder.TimedEvent>();

    @Override
    public void recordEvent(HttpClient.Event event) {
        long now = System.nanoTime();
        _events.add(new TimedEvent(event, now - _prevEventTimeNanos));
        _prevEventTimeNanos = now;
    }

    public List<ElapsedTimeEventRecorder.TimedEvent> getEvents() {
        return _events;
    }

    public static class TimedEvent {
        private final HttpClient.Event _event;
        private final long _timeNanos;

        public TimedEvent(HttpClient.Event event, long timeNanos) {
            _event = event;
            _timeNanos = timeNanos;
        }

        public HttpClient.Event getEvent() {
            return _event;
        }

        public long getNanosSincePreviousEvent() {
            return _timeNanos;
        }

        @Override
        public String toString() {
            return String.format("%s: %d ns", _event.name(), _timeNanos);
        }
    }
}