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

NOTE! Starting with 1.1, the HTTP client has rudimentary proxy support, including
support for Basic and NTLM proxy authentication. The NTLM authentication code has
been copied from https://github.com/apache/httpcomponents-client.git, the Apache
HTTP client version 4.5. The two projects both use the same license, the code is
open source and I hereby give credit to the original authors, so without being a
lawyer I assume that I'm doing the right thing. The code is copied rather than
included as I really only want one class and I want to keep this package lean
without external dependencies.
