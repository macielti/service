# Change Log

All notable changes to this project will be documented in this file. This change log follows the conventions
of [keepachangelog.com](http://keepachangelog.com/).

## 0.4.0 - 2026-05-01

### Changed

- **Breaking:** Removed connector-level CORS configuration (`:allowed-origins` option is no longer supported). CORS and other default interceptors must now be configured per-route in the consuming application using `io.pedestal.connector/with-default-interceptors` or Pedestal route-level interceptors.

## 0.3.8 - 2026-05-01

### Added

- Configurable Jetty thread pool via `:min-threads` (default `8`), `:max-threads` (default `50`), and `:max-queue-size` (default `200`) options.
- Virtual thread support via `:use-virtual-threads` option (default `true`). On Java 21+, Jetty's `VirtualThreadPool` is used automatically, allowing thousands of concurrent requests at low memory cost. Falls back to platform threads on older JVMs.
- Bounded request queue using `BlockingArrayQueue` when running with platform threads — requests beyond `:max-queue-size` are rejected with HTTP 503 instead of silently filling heap.
- Documentation for thread pool and virtual thread configuration options in README.

### Fixed

- `:idle-timeout-ms` was previously silently ignored due to an incorrect Pedestal option key. It is now correctly applied to all Jetty connectors via the server configurator.

## 0.3.7 - 2026-03-08

### Added

- Configurable Jetty idle timeout via `:idle-timeout-ms` option (defaults to `30000` ms).
- Configurable CORS allowed origins via `:allowed-origins` option (defaults to allowing all origins).
- Documentation for service configuration options in README.

### Changed

- Upgraded `common-clj` dependency from `46.0.0` to `46.1.4`.
- Upgraded `lein-clojure-lsp` plugin from `2.0.13` to `2.0.14`.

## 0.3.6 - 2026-03-07

### Added

- `path-params-schema` interceptor for path params validation and coercion.
- `UUID` coercion support via `s/Uuid`.

## 0.1.0 - 2026-02-20

### Added

- Initial implementation of the `service` library.
