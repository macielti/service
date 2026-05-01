[![Clojars Project](https://img.shields.io/clojars/v/net.clojars.macielti/service-component.svg)](https://clojars.org/net.clojars.macielti/service-component)
![Compatible with GraalVM](https://img.shields.io/badge/compatible_with-GraalVM-green)

# Service

Service is a Pedestal service Integrant component.

If you will be using this library in a project intended to be compiled using GraalVM, you should add the following
dependencies to your project:

``` clojure
  [net.clojars.macielti/service "x.y.z"]
  [io.pedestal/pedestal.service "0.8.1"]
  [io.pedestal/pedestal.jetty "0.8.1"]
  [io.pedestal/pedestal.error "0.8.1"]
```

## Configuration

The service component accepts configuration through the `:service` key in your config map. The following options are available:

| Key                    | Type                | Required | Default           | Description                                                                                                    |
|------------------------|---------------------|----------|-------------------|----------------------------------------------------------------------------------------------------------------|
| `:host`                | String              | Yes      | —                 | The host address to bind the server to (e.g., `"0.0.0.0"`).                                                   |
| `:port`                | Integer             | Yes      | —                 | The port number to listen on (e.g., `8080`).                                                                   |
| `:idle-timeout-ms`     | Integer             | No       | `30000`           | Jetty idle timeout in milliseconds. Connections idle beyond this duration are closed.                          |
| `:allowed-origins`     | Collection\<String> | No       | Allow all origins | A collection of allowed origin strings for CORS. When omitted or empty, all origins are allowed.               |
| `:min-threads`         | Integer             | No       | `8`               | Minimum number of threads kept alive in the Jetty thread pool.                                                 |
| `:max-threads`         | Integer             | No       | `50`              | Maximum number of concurrent threads. Acts as a concurrency cap for both platform and virtual thread modes.    |
| `:max-queue-size`      | Integer             | No       | `200`             | Maximum number of requests that can queue while all threads are busy (platform threads only). Requests beyond this limit are rejected with HTTP 503. |
| `:use-virtual-threads` | Boolean             | No       | `true`            | When `true` and running on Java 21+, uses Jetty's `VirtualThreadPool` instead of `QueuedThreadPool`. Falls back to platform threads automatically on Java < 21. |

### Thread pool behaviour

The component selects the thread pool implementation at startup based on `:use-virtual-threads` and the detected JVM version:

**Virtual threads (Java 21+, default)**

Uses Jetty's `VirtualThreadPool`. Each request runs in its own virtual thread, which is cheap to create (~few KB) and automatically yields during blocking I/O, allowing thousands of concurrent requests without stacking platform threads. Concurrency is bounded by `:max-threads` via a semaphore. `:min-threads` and `:max-queue-size` are ignored in this mode.

**Platform threads (Java < 21, or `:use-virtual-threads false`)**

Uses Jetty's `QueuedThreadPool` backed by a `BlockingArrayQueue` of size `:max-queue-size`. Requests are served by a pool of `:min-threads` to `:max-threads` platform threads. When all threads are busy and the queue is full, new requests are rejected immediately with **HTTP 503**, providing explicit backpressure instead of silently growing memory until OOM.

### Example

```clojure
{:service {:host                "0.0.0.0"
           :port                8080
           :idle-timeout-ms     60000
           :allowed-origins     ["https://example.com" "https://app.example.com"]
           :min-threads         8
           :max-threads         200
           :max-queue-size      500
           :use-virtual-threads true}}
```

### Recommended values by workload

| Scenario                    | `:min-threads` | `:max-threads` | `:max-queue-size` |
|-----------------------------|----------------|----------------|-------------------|
| IO-bound (DB, HTTP calls)   | `8`            | `100`–`200`    | `500`             |
| CPU-bound                   | `4`            | `nCPU × 2`    | `50`              |
| Small pods / low memory     | `4`            | `20`           | `100`             |
| Java 21+ (virtual threads)  | —              | `500`+         | —                 |

> **Note:** If `:idle-timeout-ms` is not provided, a default of **30 seconds** (`30000` ms) is applied to prevent stalled connections from tying up server resources.

> **Note:** If `:allowed-origins` is not provided or is empty, the server will accept requests from **any origin**. In production, explicitly list trusted origins to prevent unwanted cross-origin access.

## License

Copyright © 2024 Bruno do Nascimento Maciel

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
