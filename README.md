[![Clojars Project](https://img.shields.io/clojars/v/net.clojars.macielti/service-component.svg)](https://clojars.org/net.clojars.macielti/service-component)
![Compatible with GraalVM](https://img.shields.io/badge/compatible_with-GraalVM-green)

# Service

Service is a Pedestal service Integrant component.

If you will be using this library in a project intended to be compiled using GraalVM, you should add the following
dependencies to your project:

``` clojure
  [net.clojars.macielti/service "x.y.z"]
  [io.pedestal/pedestal.service "0.8.1"]
  [io.pedestal/pedestal.http-kit "0.8.1"]
  [io.pedestal/pedestal.error "0.8.1"]
```

## Configuration

The service component accepts configuration through the `:service` key in your config map. The following options are available:

| Key               | Type    | Required | Default                         | Description                                                                           |
|-------------------|---------|----------|---------------------------------|---------------------------------------------------------------------------------------|
| `:host`           | String  | Yes      | —                               | The host address to bind the server to (e.g., `"0.0.0.0"`).                          |
| `:port`           | Integer | Yes      | —                               | The port number to listen on (e.g., `8080`).                                          |
| `:worker-threads` | Integer | No       | http-kit default (4 × CPU cores) | Number of worker threads for handling requests. Passed directly to http-kit's server. |

### Http-Kit worker threads

Http-Kit uses Java NIO to handle connections asynchronously with a fixed pool of worker threads.

**`:worker-threads` option**

Controls the number of threads processing requests. If not set, http-kit's default applies (typically 4 × available CPU cores). For most services the default is adequate; increase it for workloads with high thread utilisation or slow blocking handlers.

### Example

```clojure
{:service {:host           "0.0.0.0"
           :port           8080
           :worker-threads 16}}
```

## Interceptors

CORS and other default request interceptors must be configured **in your consuming application**, not at the component level.

The service component does not register default interceptors. All interceptors must be explicitly added by the consuming application using `io.pedestal.connector/with-interceptors` or per-route definitions.

To add error handling, CORS, authentication, rate limiting, or other cross-cutting concerns, use one of these approaches in your application:

**Option 1: Per-route interceptors**

Define interceptors on individual routes in your route definitions:
```clojure
["/api/resource"
 {:get {:handler my-handler
        :interceptors [cors-interceptor auth-interceptor]}}]
```

**Option 2: Connector-level default interceptors**

Use `io.pedestal.connector/with-default-interceptors` in your route setup to apply interceptors to all routes:
```clojure
(io.pedestal.connector/with-default-interceptors connector :allowed-origins cors-origins)
```

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
