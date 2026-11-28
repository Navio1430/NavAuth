# OrmLite → Exposed field/type mapping (NavAuth)

Only the persistence layer changes. Domain models (`User`, `UserCredentials`,
`UserActivitySession`, `UserUuid`, `MojangId`, `Username`, `HashingAlgorithm`, `IPAddress`) and
the mappers stay; the mappers only need their table/import references updated.

## Global rules

- Use plain `Table` + explicit `PrimaryKey`. Do **not** use `UUIDTable`/`IdTable`: NavAuth PKs are
  supplied by the caller (the user UUID), never auto-generated.
- Keep physical column types aligned with the Flyway scripts. If Exposed's built-in type differs
  from the captured schema on a dialect, register a custom `ColumnType` rather than changing the
  schema silently.
- Name every column explicitly and match `V001_initial.sql` exactly.

## `navauth_users` → `UsersTable`

| OrmLite | Exposed | Notes |
| --- | --- | --- |
| `@DatabaseField(id = true) uuid: UUID` | `uuid("uuid")` + `PrimaryKey` | Caller-assigned. On SQLite/MySQL the physical type is `VARCHAR(36)`; verify `uuid()` maps to it, otherwise use a custom `UUIDColumnType` storing text. |
| `mojang_uuid: UUID?` (`index = true`) | `uuid("mojang_uuid").nullable()` + `index()` | Index is created by Flyway; do not create a duplicate in Exposed (see gotchas). |
| `username: String` (`index = true`) | `varchar("username", 255)` + `index()` | |
| `username_lowercase: String` (`index = true`) | `varchar("username_lowercase", 255)` + `index()` | Invariant enforced by the old record's `init`. Keep persisting `username.lowercase()` in the mapper/`upsert`. |
| `credentials_required: Boolean` | `bool("credentials_required")` | Dialect-specific physical type; matches Flyway. |

`createOrUpdate` semantics → `upsert(UsersTable.uuid) { ... }` (or update-then-insert). A plain
`insert` throws on an existing PK.

## `navauth_credentials` → `CredentialsTable`

| OrmLite | Exposed | Notes |
| --- | --- | --- |
| `uuid: UUID` (id, one-to-one with user) | `uuid("uuid")` + `PrimaryKey` | Same physical-type caveat as users. |
| `passwordHash: String?` | `varchar("password_hash", 255).nullable()` | |
| `algo: HashingAlgorithm?` | `enumerationByName("algo", 255).nullable()` | **Name**, not ordinal. `enumeration()` would break existing data. |
| `twoFactorSecret: String?` | `varchar("two_factor_secret", 255).nullable()` | |

`deleteByUser` → `deleteWhere { CredentialsTable.uuid eq user.uuid.value }`.

## `navauth_users_sessions` → `UserSessionsTable` (PK-less)

| OrmLite | Exposed | Notes |
| --- | --- | --- |
| `uuid: UUID` (`index = true`) | `uuid("uuid")` | No `PrimaryKey`. |
| `joined_at: Date?` (`DATE_LONG`) | `long("joined_at")` | Epoch millis. Convert with `joinedAt.time` / `Date(millis)`. |
| `left_at: Date?` (`DATE_LONG`, indexed) | `long("left_at")` + `index()` | Ordering key; read `left_at DESC`. |
| `ip: Long` | `long("ip")` | Packed IP; unchanged. |

Because there is no PK, `upsert` is unavailable. This table is append-only (`insert`) and read
with `orderBy(UserSessionsTable.leftAt, SortOrder.DESC).limit(1)` for "latest". The paginator
counts with `count()` and pages with `.limit().offset()`, preserving newest-first ordering.

## Dates

Do not pull in `exposed-java-time`/`exposed-kotlin-datetime` just for these fields. The ORM
stored epoch millis (`DATE_LONG`), so `long()` keeps the physical schema and the domain
`java.util.Date` conversions trivial. Only introduce a java-time module if a future column
actually needs a timestamp type.

## Mappers

`UserMapper`, `UserCredentialsMapper`, `UserActivitySessionMapper` become `ResultRow.toDomain()`
extensions (and `toStatement` helpers for upserts). They must not import `com.j256.ormlite.*`.
Keep the exact same conversion logic, including the `mojangUuid != null → premium` branch and the
`HashedPassword`/`TOTPSecret` construction.

## Exposed 1.5.0 notes

- `upsert` with a key selector is available; older blog snippets using `insertOrUpdate` may not
  compile.
- `selectAll().where { }`, `deleteWhere { }`, `batchInsert`, and `batchUpsert` are the DSL names
  to use.
- Prefer `transaction(db) { ... }` because NavAuth is blocking (no coroutine transaction API).
