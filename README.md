# HttpClient

This is a very simple but well instrumented HTTP client primarily intended for tests.
It has no external dependencies at all and there are no hidden surprises (unwanted
keepalives, connection pools, caches). There is support for self-signed certificates,
weak certificates and weak algorithms, all common in test environmants.

The built-in classes are more powerful, but are hard to use in a controlled way.
They are designed for the real world, not for tests. They can consume too much
resources (at least on Raspberry Pi and similar devices) and they are not eager
to trust self-signed certificates. In addition they don't expose connection time,
SSL handshake time and so on.

There are other more powerful HTTP clients as well, but they are also designed
more for real-world use from mainstream computers and less for tests and low-powered
devices.

Need a simple HTTP client that can run on limited hardware and that makes it possible
to measure what takes time? Give it a try. Otherwise look into something more mainstream.