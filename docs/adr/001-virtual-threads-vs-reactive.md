# 001: Virtual Threads vs Reactive Programming

## Status
Accepted

## Context
PayStream needs to handle high concurrency with non-blocking I/O. Two main approaches:

1. **Reactive Programming (WebFlux + Project Reactor)**: Event-driven, callback-based
2. **Virtual Threads (Project Loom)**: Sequential imperative code with OS thread scheduling

## Decision
Use Java 21 virtual threads with Spring WebFlux for the following reasons:

- **Readability**: Sequential code is easier to write, debug, and maintain
- **Throughput**: Virtual threads provide similar performance to reactive without complexity
- **Compatibility**: Works with existing JDBC/R2DBC drivers
- **Future-proof**: Virtual threads are the future of Java concurrency

## Consequences
- Requires Java 21+
- Some libraries may need updates for virtual thread compatibility
- Reactive operators still useful for complex async operations