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
import com.j256.ormlite.dao.Dao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.config.MigrationConfig
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm
import pl.spcode.navauth.common.domain.credentials.TwoFactorSecret
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.credentials.UserCredentialsRepository
import pl.spcode.navauth.common.domain.user.*
import pl.spcode.navauth.common.infra.crypto.HashedPassword
import pl.spcode.navauth.common.infra.crypto.PasswordHash
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.infra.database.EntitiesRegistrar
import pl.spcode.navauth.common.migrate.MigrationManager
import pl.spcode.navauth.common.migrate.error.SourceDatabaseConnectException
import pl.spcode.navauth.common.migrate.error.SourceTableNotFoundException
import pl.spcode.navauth.common.migrate.migrator.Migrator
import pl.spcode.navauth.common.shared.PluginDirectory
import java.util.UUID

class LibreLoginMigrator
@Inject
constructor(
  val userRepository: UserRepository,
  val userCredentialsRepository: UserCredentialsRepository,
  migrationConfig: MigrationConfig,
  pluginDirectory: PluginDirectory,
) : Migrator {

  val logger: Logger = LoggerFactory.getLogger(LibreLoginMigrator::class.java)

  lateinit var sourceDao: Dao<LibreLoginUser, UUID>

  val entitiesRegistrar = EntitiesRegistrar().registerEntity(LibreLoginUser::class)

  val sourceDatabaseManager =
    DatabaseManager(migrationConfig.sourceDatabaseConfig, entitiesRegistrar, pluginDirectory)

  override fun init() {
    try {
      sourceDatabaseManager.connect(MigrationManager.MIGRATION_SOURCE_POOL_NAME)
    } catch (e: Exception) {
      throw SourceDatabaseConnectException(e)
    }

    sourceDao = sourceDatabaseManager.getDao(LibreLoginUser::class)
    if (!sourceDao.isTableExists) {
      throw SourceTableNotFoundException(sourceDao.tableName)
    }
  }

  override fun getSourceRecordsCount(): Long {
    return sourceDao.countOf()
  }

  override fun migrateNext(offset: Long, limit: Long): Long {
    val qb = sourceDao.queryBuilder().orderBy("uuid", true).offset(offset).limit(limit).prepare()

    val libreUsers = sourceDao.query(qb)
    libreUsers.forEach { lUser ->
      val isPremium = lUser.premiumUuid != null
      val twoFactorEnabled = lUser.secret != null

      val username = Username(lUser.lastNickname!!)
      val userId = UserId(lUser.uuid!!)

      val targetUser =
        if (isPremium) {
          User.premium(userId, username, MojangId(lUser.premiumUuid!!), twoFactorEnabled)
        } else {
          User.nonPremium(userId, username)
        }

      val hashedPassword = getHashedPassword(lUser)
      if (!isPremium && hashedPassword == null) {
        logger.info(
          "Non-Premium user ${lUser.lastNickname}:${lUser.uuid} has no password which is an invalid record. Skipping record..."
        )
        return@forEach
      } else {
        val twoFactorSecret = lUser.secret?.let { TwoFactorSecret(it) }
        val credentials =
          UserCredentials.create(
            userId,
            hashedPassword!!.hash,
            hashedPassword.algo,
            twoFactorSecret,
          )
        userCredentialsRepository.save(credentials)
      }

      // todo in transaction
      userRepository.save(targetUser)
    }

    return libreUsers.size.toLong()
  }

  private fun getHashedPassword(libreUser: LibreLoginUser): HashedPassword? {
    val hashRaw = libreUser.passwordHash ?: return null
    val saltRaw = libreUser.passwordSalt ?: return null
    val algoRaw = libreUser.passwordAlgo ?: return null

    val algo: HashingAlgorithm =
      if (algoRaw.startsWith("BCrypt-")) {
        HashingAlgorithm.BCRYPT
      } else
        when (algoRaw) {
          "Argon-2ID" -> HashingAlgorithm.ARGON2
          "LOGIT-SHA-256" -> HashingAlgorithm.LOGITSHA256
          "SHA-256" -> HashingAlgorithm.SHA256
          "SHA-512" -> HashingAlgorithm.SHA512
          else ->
            throw IllegalStateException(
              "Unknown hashing algorithm: $algoRaw for user ${libreUser.lastNickname}:${libreUser.uuid}"
            )
        }

    val passwordHash =
      when (algo) {
        HashingAlgorithm.BCRYPT -> {
          PasswordHash(convertToBCryptFull(hashRaw, saltRaw, algoRaw))
        }
        HashingAlgorithm.ARGON2 -> {
          PasswordHash(convertToArgon2Full(hashRaw, saltRaw, algoRaw))
        }
        HashingAlgorithm.SHA256 -> TODO()
        HashingAlgorithm.SHA512 -> TODO()
        HashingAlgorithm.LOGITSHA256 -> TODO()
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
}
