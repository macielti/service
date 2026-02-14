# Change Log

All notable changes to this project will be documented in this file. This change log follows the conventions
of [keepachangelog.com](http://keepachangelog.com/).

## 7.4.4 - 2026-02-14

### Fixed

- Update Dependencies and fix version conflicts.

## 7.4.3 - 2026-01-11

### Changed

- Breaking Change: Error handling for request body validation has been updated.
    - The error code for schema validation failures is now `"invalid-request-body-payload"`.
    - Error messages for invalid request bodies are improved for clarity.
    - Error details in responses are now consistently stringified.
- Schema Coercion: String values in JSON request bodies are now automatically coerced to keywords when the schema
  expects a keyword, improving compatibility with `EqSchema`.

## 6.4.3 - 2026-01-02

### Added

- Add to interceptor `query-params-schema` coercion support from `String` to `LocalDate`.

## 6.4.2 - 2026-01-02

### Changed

- Renamed interceptor from `schema-body-in-interceptor` to `wire-in-body-schema`.

### Added

- Implement interceptor `query-params-schema` to coerce query params and validate against expected schema.

## 5.4.2 - 2025-12-25

### Changed

- **Major Pedestal Upgrade**: Migrated from Pedestal 0.7.x to 0.8.x, adopting the new connector-based API.
    - Refactored `service-component.core` to use `io.pedestal.connector` and `io.pedestal.http.jetty`, replacing the
      deprecated `io.pedestal.http` API.
    - Updated interceptors and test utilities to use modern namespaces like `io.pedestal.connector` and
      `io.pedestal.service.interceptors`.
- **Interceptors**: The default set of interceptors was updated to align with the new Pedestal 0.8 architecture and
  improve error handling.

### Dependencies

- Upgraded Pedestal to `0.8.1`.
- Upgraded Clojure to `1.12.4`.
- Upgraded Integrant to `1.0.1`.
- Updated various development and test dependencies to their latest versions.

## 4.4.2 - 2025-06-19

### Fixed

- Fixed `schema-body-in-interceptor` interceptor to update the request body with the coerced schema value.

## 3.4.2 - 2025-06-17

### Added

- Apply schema coercion with in `schema-body-in-interceptor`.

## 2.4.2 - 2024-11-17

### Fixed

- Use `:http-request-in-handle-timing-v2` metric.

## 1.4.2 - 2024-11-16

### Added

- Added `service-component.interceptors/http-request-in-handle-timing-interceptor` interceptor.

## 1.3.2 - 2024-11-10

### Added

- Bump Pedestal version to 0.7.2.

## 1.3.1 - 2024-11-09

### Fixed

- Added missing pedestal interceptor dependencies.

## 1.3.0 - 2024-11-09

### Fixed

- Fixed error handling interceptors.

## 0.3.0 - 2024-11-09

### Added

- Added body request schema validation.

## 0.2.0 - 2024-11-08

### Added

- Added default interceptors to the service component.

## 0.1.0 - 2024-11-08

### Added

- Initial implementation of the service-component library.
