# PMS Modulith

Spring Boot / Spring Modulith port of the Laravel `property-management-system` app.
It's a **modular monolith**: one deployable Spring Boot application, internally split
into independently-testable business modules with enforced boundaries, communicating
either through small public Java interfaces or through Spring application events.
This gives most of the benefits of a microservices decomposition (clear boundaries,
independent domain models, no accidental coupling) without the operational cost of
running/deploying ten separate services — and each module can be extracted into its
own microservice later with comparatively little rework, since the boundaries are
already explicit and enforced by a test.

## Why Modulith instead of separate microservices

Real microservices would mean a separate database, deployment, and network hop per
domain (bookings, billing, housekeeping...) for an app whose modules are heavily
transactional with each other (a booking touches rooms, guests, billing and
housekeeping in the same business operation). Spring Modulith keeps that consistency
inside one process/database/transaction while still forcing the same discipline a
microservices split would: modules only talk to each other's public API, never their
internals, and the boundary is checked by a test (`ModularityTests`) on every build.

## Module map

Each module is a top-level package under `com.pms.hotel`. Its root package is the
module's public API (interfaces + records); everything under `.internal` is private
implementation (JPA entities, repositories, `@RestController`s) that other modules
are not allowed to import — `ModularityTests` fails the build if they do.

| Module | Owns | Depends on (via public API) |
|---|---|---|
| `shared` | Base entity, JWT/security config, error handling, paging DTO. Declared `OPEN` — usable from every module. | — |
| `guest` | Guests | — |
| `room` | Room types, rooms, room status history | — |
| `booking` | Bookings, multi-room pivot, anti-overbooking, daily arrivals/departures | `guest`, `room` |
| `housekeeping` | Housekeeping/maintenance tasks | `room` |
| `pos` | Restaurant/spa/bar charges billed to a room | `booking`, `room` |
| `billing` | Invoices, invoice lines, payments, the aggregated "booking billing view" | `booking`, `pos`, `guest` |
| `channelmanager` | OTA (Booking.com/Expedia/...) webhook ingestion | `booking`, `guest`, `room` |
| `reporting` | Dashboard stats, revenue report | `booking`, `room` |
| `settings` | Hotel-wide key/value settings | — |

The dependency graph is intentionally a DAG (no cycles): `billing` sits above
`booking`/`pos` because it aggregates their data, so `booking`/`pos` never need to
know billing exists. That's also why the booking billing view
(`GET /api/v1/bookings/{id}/billing`) is implemented in the `billing` module even
though the URL still starts with `/bookings` — the REST path is independent from
which module's package the controller lives in.

One real cross-module **event** is used, as a demonstration of the pattern and
because it's genuinely a fire-and-forget notification in the original app too: when
`pos` bills a charge to a room, it publishes `ExtraChargeAddedEvent`; `billing`
listens (`@ApplicationModuleListener`) and, if an invoice already exists for that
booking, bumps its totals. Spring Modulith persists the event publication (see the
`event_publication` table) so a listener failure can be retried instead of silently
losing the update.

## Identity is external

This service does **not** manage users, roles or permissions — that's a different
service's job. It only validates JWTs issued elsewhere (`spring-boot-starter-oauth2-resource-server`,
HMAC-signed, shared secret via `pms.security.jwt.secret`) and reads `roles`/`permissions`
claims into Spring Security authorities:

```json
{
  "sub": "42",
  "roles": ["admin"],
  "permissions": ["manage rooms", "manage housekeeping", "settings.update"]
}
```

`roles` become `ROLE_ADMIN`-style authorities (`hasRole("ADMIN")`), `permissions` are
used as-is (`hasAuthority("manage rooms")`, matching the original Spatie permission
names). `sub` is read as the numeric user id wherever the app needs to attribute an
action to a user (`CurrentUser` bean) — e.g. `assigned_to` on a housekeeping task, or
`updated_by` on a room status log. Those are plain `BIGINT` columns, not foreign keys
to a local `users` table.

## Running it

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # H2 in-memory, http://localhost:8080
```

- `dev` profile: H2 (`/h2-console` enabled), auto-active by default.
- `prod` profile: PostgreSQL — set `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`.
- `JWT_SECRET`, `CHANNEL_MANAGER_WEBHOOK_TOKEN` should be overridden outside `dev`.
- API docs: `http://localhost:8080/swagger-ui.html`.

Every endpoint requires `Authorization: Bearer <jwt>` except
`POST /api/v1/channel-manager/bookings` (authenticated instead by the
`X-Channex-Webhook-Token` header, since the caller is the OTA aggregator, not a user).

## Tests

```bash
./mvnw test
```

- `PmsModulithApplicationTests` — the full Spring context boots against H2.
- `ModularityTests` — `ApplicationModules.verify()` fails the build if a module
  reaches into another module's `.internal` package or a dependency cycle appears;
  it also regenerates the PlantUML module diagrams under `target/spring-modulith-docs`.

## Deliberate differences from the Laravel source

- **No local user/auth/permission management** — moved to an external identity
  service (see above); the Laravel `UserManagementController` / `AuthController` /
  `PermissionMatrixController` have no equivalent here.
- **Dead schema dropped**: `room_rates`, `hotel_services`, `room_availabilities` and
  `booking_guests` existed as Laravel migrations/models but were never read or
  written by any controller. Left out here rather than carried over as inert tables.
- **`Invoice.amountPaid` at generation time** is computed from payments with status
  `completed` (the value the app's own `refreshStatus()` uses) rather than the
  Laravel accessor's `status === 'paid'` filter, which can never match since no
  code ever sets that status — apparent bug in the source, fixed here.
- **Cross-module full-text guest search** (filtering bookings by guest name/email)
  is not implemented: the `booking` module only stores `guestId`, and reaching into
  `guest`'s database from a listing query would break the module boundary. A real
  microservices split would solve this with a read-model/search index; out of scope
  here. Booking listing still supports filtering by `status`.
- **Booking "show"** returns booking + rooms only; extra charges/invoice/payments
  moved to the dedicated billing view endpoint (`/bookings/{id}/billing`), for the
  same module-boundary reason.
- Money fields use `NUMERIC(10,2)`/`BigDecimal` throughout instead of PHP floats.
