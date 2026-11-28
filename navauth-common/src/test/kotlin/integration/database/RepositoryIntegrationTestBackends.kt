/*
 * NavAuth
 * Copyright © 2026 Oliwier Fijas (Navio1430)
 *
 * NavAuth is free software; You can redistribute it and/or modify it under the terms of:
 * the GNU Affero General Public License version 3 as published by the Free Software Foundation.
 *
 * NavAuth is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with NavAuth. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 *
 */

package integration.database

import org.testcontainers.junit.jupiter.Testcontainers

// ---------------------------------------------------------------------------------------------
// H2 (no Docker required)
// ---------------------------------------------------------------------------------------------

class H2UserRepositoryTests : UserRepositoryTests() {
  override val backend = TestBackend.H2_MEM
}

class H2UserCredentialsRepositoryTests : UserCredentialsRepositoryTests() {
  override val backend = TestBackend.H2_MEM
}

// ---------------------------------------------------------------------------------------------
// SQLite (no Docker required)
// ---------------------------------------------------------------------------------------------

class SqliteUserRepositoryTests : UserRepositoryTests() {
  override val backend = TestBackend.SQLITE
}

class SqliteUserCredentialsRepositoryTests : UserCredentialsRepositoryTests() {
  override val backend = TestBackend.SQLITE
}

// ---------------------------------------------------------------------------------------------
// MySQL (Docker; skipped when Docker is not available)
// ---------------------------------------------------------------------------------------------

@Testcontainers(disabledWithoutDocker = true)
class MySqlUserRepositoryTests : UserRepositoryTests() {
  override val backend = TestBackend.MYSQL
}

@Testcontainers(disabledWithoutDocker = true)
class MySqlUserCredentialsRepositoryTests : UserCredentialsRepositoryTests() {
  override val backend = TestBackend.MYSQL
}

// ---------------------------------------------------------------------------------------------
// MariaDB (Docker; skipped when Docker is not available)
// ---------------------------------------------------------------------------------------------

@Testcontainers(disabledWithoutDocker = true)
class MariaDbUserRepositoryTests : UserRepositoryTests() {
  override val backend = TestBackend.MARIADB
}

@Testcontainers(disabledWithoutDocker = true)
class MariaDbUserCredentialsRepositoryTests : UserCredentialsRepositoryTests() {
  override val backend = TestBackend.MARIADB
}

// ---------------------------------------------------------------------------------------------
// PostgreSQL (Docker; skipped when Docker is not available)
// ---------------------------------------------------------------------------------------------

@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlUserRepositoryTests : UserRepositoryTests() {
  override val backend = TestBackend.POSTGRESQL
}

@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlUserCredentialsRepositoryTests : UserCredentialsRepositoryTests() {
  override val backend = TestBackend.POSTGRESQL
}

// ---------------------------------------------------------------------------------------------
// Schema installs and upgrades (H2 and SQLite in-process, the others in Docker)
// ---------------------------------------------------------------------------------------------

class H2SchemaMigrationTests : SchemaMigrationTests() {
  override val backend = TestBackend.H2_MEM
}

class SqliteSchemaMigrationTests : SchemaMigrationTests() {
  override val backend = TestBackend.SQLITE
}

@Testcontainers(disabledWithoutDocker = true)
class MySqlSchemaMigrationTests : SchemaMigrationTests() {
  override val backend = TestBackend.MYSQL
}

@Testcontainers(disabledWithoutDocker = true)
class MariaDbSchemaMigrationTests : SchemaMigrationTests() {
  override val backend = TestBackend.MARIADB
}

@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlSchemaMigrationTests : SchemaMigrationTests() {
  override val backend = TestBackend.POSTGRESQL
}
