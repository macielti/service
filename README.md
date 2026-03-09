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

| Key                 | Type                | Required | Default        | Description                                                                          |
|---------------------|---------------------|----------|----------------|--------------------------------------------------------------------------------------|
| `:host`             | String              | Yes      | —              | The host address to bind the server to (e.g., `"0.0.0.0"`).                         |
| `:port`             | Integer             | Yes      | —              | The port number to listen on (e.g., `8080`).                                         |
| `:idle-timeout-ms`  | Integer             | No       | `30000`        | Jetty idle timeout in milliseconds. Connections idle beyond this duration are closed. |
| `:allowed-origins`  | Collection\<String> | No       | Allow all origins | A collection of allowed origin strings for CORS. When provided, only the specified origins are permitted. When omitted or empty, all origins are allowed. |

### Example

```clojure
{:service {:host            "0.0.0.0"
           :port            8080
           :idle-timeout-ms 60000                                     ;; 60 seconds
           :allowed-origins ["https://example.com" "https://app.example.com"]}}
```

> **Note:** If `:idle-timeout-ms` is not provided, a default of **30 seconds** (`30000` ms) is applied to prevent long-running or stalled requests from tying up server threads indefinitely.

> **Note:** If `:allowed-origins` is not provided or is empty, the server will accept requests from **any origin**. In production, it is recommended to explicitly list trusted origins to prevent unwanted cross-origin access.

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
