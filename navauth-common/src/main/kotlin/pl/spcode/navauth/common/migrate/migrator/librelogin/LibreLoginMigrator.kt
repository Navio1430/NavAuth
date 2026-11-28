/*
 * NavAuth
 * Copyright © 2025 Oliwier Fijas (Navio1430)
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

package pl.spcode.navauth.common.migrate.migrator.librelogin

import com.google.inject.Inject
import java.util.UUID
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.application.validator.UsernameValidator
import pl.spcode.navauth.common.config.MigrationConfig
import pl.spcode.navauth.common.domain.common.TransactionService
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm
import pl.spcode.navauth.common.domain.credentials.UserCredentialsRepository
import pl.spcode.navauth.common.domain.user.UserRepository
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.infra.crypto.CryptoUtils
import pl.spcode.navauth.common.infra.crypto.HashedPassword
import pl.spcode.navauth.common.infra.crypto.PasswordHash
import pl.spcode.navauth.common.infra.crypto.hasher.SHACredentialsHasher
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.migrate.MigrationManager
import pl.spcode.navauth.common.migrate.error.SourceDatabaseConnectException
import pl.spcode.navauth.common.migrate.error.SourceTableNotFoundException
import pl.spcode.navauth.common.migrate.migrator.MigrationChunkResult
import pl.spcode.navauth.common.migrate.migrator.Migrator
import pl.spcode.navauth.common.shared.PluginDirectory

class LibreLoginMigrator
@Inject
constructor(
  val userRepository: UserRepository,
  val userCredentialsRepository: UserCredentialsRepository,
  val usernameValidator: UsernameValidator,
  val txService: TransactionService,
  migrationConfig: MigrationConfig,
  pluginDirectory: PluginDirectory,
) : Migrator {

  val logger: Logger = LoggerFactory.getLogger(LibreLoginMigrator::class.java)

  val sourceDatabaseManager = DatabaseManager(migrationConfig.sourceDatabaseConfig, pluginDirectory)

  private lateinit var sourceDatabase: Database

  /** A source row that cannot be imported; [reason] never contains password data. */
  private class InvalidRowException(val reason: String) : Exception(reason)

  override fun init() {
    try {
      sourceDatabaseManager.connect(MigrationManager.MIGRATION_SOURCE_POOL_NAME)
    } catch (e: Exception) {
      throw SourceDatabaseConnectException(e)
    }

    sourceDatabase = DatabaseManager.connectExposed(sourceDatabaseManager.dataSource)

    try {
      transaction(sourceDatabase) { LibreLoginUserTable.selectAll().limit(1).toList() }
    } catch (e: Exception) {
      throw SourceTableNotFoundException(LibreLoginUserTable.tableName)
    }
  }

  override fun getSourceUsersCount(): Long =
    transaction(sourceDatabase) { LibreLoginUserTable.selectAll().count() }

  override fun migrateNext(offset: Long, limit: Long): MigrationChunkResult {
    val rows =
      transaction(sourceDatabase) {
        LibreLoginUserTable.selectAll()
          .orderBy(LibreLoginUserTable.uuid, SortOrder.ASC)
          .limit(limit.toInt())
          .offset(offset)
          .map { it.toLibreLoginRow() }
      }

    // Rows are validated and converted before the target transaction, so a damaged row is skipped
    // and never rolls back the rest of the chunk.
    val converted = rows.mapNotNull { row -> convertOrSkip(row) }
    val migrated = txService.inTransaction { converted.count { storeOrSkip(it) } }.toLong()

    val read = rows.size.toLong()
    return MigrationChunkResult(read = read, migrated = migrated, skipped = read - migrated)
  }

  private fun convertOrSkip(row: LibreLoginRow): LibreLoginConvertedUser? {
    return try {
      convert(row)
    } catch (e: InvalidRowException) {
      logger.warn("Skipping LibreLogin user {} (uuid {}): {}", row.lastNickname, row.uuid, e.reason)
      null
    }
  }

  /** @return null for a cracked row without a password and a 2FA secret (nothing to log in with) */
  private fun convert(row: LibreLoginRow): LibreLoginConvertedUser? {
    val uuid =
      row.uuid?.let { parseUuid(it) } ?: throw InvalidRowException("missing or malformed uuid")
    val nickname = row.lastNickname
    if (nickname.isNullOrEmpty()) throw InvalidRowException("missing nickname")
    if (!usernameValidator.isValid(nickname)) {
      throw InvalidRowException("the nickname does not match usernamePattern")
    }
    val premiumUuid =
      row.premiumUuid?.let { parseUuid(it) ?: throw InvalidRowException("malformed premium_uuid") }

    val lUser =
      LibreLoginUser(
        uuid = uuid,
        premiumUuid = premiumUuid,
        passwordHash = row.passwordHash,
        passwordSalt = row.passwordSalt,
        passwordAlgo = row.passwordAlgo,
        lastNickname = nickname,
        secret = row.secret,
      )

    // premium players drop their password, so it is only converted for non-premium ones
    val hashedPassword = if (premiumUuid == null) getHashedPassword(lUser) else null
    val converted =
      try {
        convertLibreLoginUser(lUser, Username(nickname), hashedPassword)
      } catch (e: IllegalArgumentException) {
        throw InvalidRowException("invalid 2FA secret")
      }
    if (converted == null) {
      logger.info(
        "Skipping LibreLogin user {} (uuid {}): non-premium user without a password and a 2FA " +
          "secret, nothing to log in with",
        nickname,
        uuid,
      )
    }
    return converted
  }

  private fun parseUuid(value: String): UUID? =
    try {
      UUID.fromString(value.trim())
    } catch (e: IllegalArgumentException) {
      null
    }

  /**
   * Writes the account unless NavAuth already has it (same name in any letter case, same uuid or
   * same Mojang UUID); an existing account is never touched.
   *
   * @return true when the account was written
   */
  private fun storeOrSkip(records: LibreLoginConvertedUser): Boolean {
    val user = records.user
    val existingUser =
      userRepository.findByUsernameIgnoreCase(user.username.value)
        ?: userRepository.findByUserUuid(user.uuid)
        ?: user.mojangUuid?.let { userRepository.findByMojangUuid(it) }
    if (existingUser != null) {
      logger.info(
        "Skipping LibreLogin user {} (uuid {}): NavAuth already has this account ({})",
        user.username,
        user.uuid,
        existingUser,
      )
      return false
    }

    records.credentials?.let { userCredentialsRepository.save(it) }
    userRepository.save(user)
    return true
  }

  /**
   * @return null when the row stores no password
   * @throws InvalidRowException when the stored password cannot be converted
   */
  private fun getHashedPassword(libreUser: LibreLoginUser): HashedPassword? {
    val hashRaw = libreUser.passwordHash ?: return null
    val algoRaw =
      libreUser.passwordAlgo ?: throw InvalidRowException("password without a hashing algorithm")
    val saltRaw = libreUser.passwordSalt ?: throw InvalidRowException("password without a salt")

    val algo: HashingAlgorithm =
      if (algoRaw.startsWith("BCrypt-")) {
        HashingAlgorithm.BCRYPT
      } else
        when (algoRaw) {
          "Argon-2ID" -> HashingAlgorithm.ARGON2
          "LOGIT-SHA-256" -> HashingAlgorithm.SHA256
          "LOGIT-SHA-512" -> HashingAlgorithm.SHA512
          "SHA-256" -> HashingAlgorithm.LIBRELOGIN_SHA256
          "SHA-512" -> HashingAlgorithm.LIBRELOGIN_SHA512
          else -> throw InvalidRowException("unknown hashing algorithm '$algoRaw'")
        }

    val passwordHash =
      try {
        when (algo) {
          HashingAlgorithm.BCRYPT -> {
            PasswordHash(convertToBCryptFull(hashRaw, saltRaw, algoRaw))
          }
          HashingAlgorithm.ARGON2 -> {
            PasswordHash(convertToArgon2Full(hashRaw, saltRaw, algoRaw))
          }
          HashingAlgorithm.SHA256,
          HashingAlgorithm.SHA512 -> {
            PasswordHash(convertToShaFull(hashRaw, saltRaw, algo))
          }
          HashingAlgorithm.LIBRELOGIN_SHA256,
          HashingAlgorithm.LIBRELOGIN_SHA512 -> {
            PasswordHash(convertToLibreLoginShaFull(hashRaw, saltRaw, algo))
          }
        }
      } catch (e: Exception) {
        // the exception can carry parts of the hash, so only the algorithm is reported
        throw InvalidRowException("malformed $algoRaw password hash")
      }

    return HashedPassword(passwordHash, algo)
  }

  private fun convertToBCryptFull(hash: String, salt: String, algo: String): String {
    require(algo.startsWith("BCrypt-")) { "Only BCrypt algo supported" }

    val (cost, hashPart) = hash.split("$", limit = 2)
    val algoVersion = algo.drop(7).lowercase() // "BCrypt-2A" -> "2a"

    return "$$algoVersion$$cost$${salt}$hashPart"
  }

  private fun convertToArgon2Full(hash: String, salt: String, algo: String): String {
    if (algo != "Argon-2ID") {
      throw IllegalArgumentException("Only Argon-2ID algorithm is supported")
    }

    val (parameters, hashBase64) = hash.split("$")

    val split = parameters.split(",")

    val version = split[0].toInt()
    val iterations = split[1].toInt()
    val memory = split[2].toInt()

    // not provided by librelogin
    val parallelism = 1

    // trim base64 if needed
    val hashTrimmed = hashBase64.trimEnd('=')
    val saltTrimmed = salt.trimEnd('=')

    return $$"$argon2id$v=$$version$m=$$memory,t=$$iterations,p=$$parallelism$$$saltTrimmed$$$hashTrimmed"
  }

  private fun convertToShaFull(hashHex: String, saltHex: String, algo: HashingAlgorithm): String {
    val hashBytes = hashHex.hexToByteArray()
    val saltBytes = saltHex.hexToByteArray()

    val hashBase64 = CryptoUtils.base64EncodeToString(hashBytes)
    val saltBase64 = CryptoUtils.base64EncodeToString(saltBytes)

    val identifier =
      when (algo) {
        HashingAlgorithm.SHA256 -> SHACredentialsHasher.PBKDF2_SHA256
        HashingAlgorithm.SHA512 -> SHACredentialsHasher.PBKDF2_SHA512
        else -> throw IllegalStateException("Only SHA256 and SHA512 algorithms are supported")
      }

    return $$"$$identifier$$$saltBase64$$$hashBase64"
  }

  private fun convertToLibreLoginShaFull(
    hashHex: String,
    saltHex: String,
    algo: HashingAlgorithm,
  ): String {
    val baseAlgoIdentifier =
      when (algo) {
        HashingAlgorithm.LIBRELOGIN_SHA256 -> HashingAlgorithm.SHA256
        HashingAlgorithm.LIBRELOGIN_SHA512 -> HashingAlgorithm.SHA512
        else ->
          throw IllegalStateException(
            "Only LibreLogin_SHA256 and LibreLogin_SHA512 algorithms are supported"
          )
      }

    return $$"$$baseAlgoIdentifier$$$saltHex$$$hashHex"
  }
}
