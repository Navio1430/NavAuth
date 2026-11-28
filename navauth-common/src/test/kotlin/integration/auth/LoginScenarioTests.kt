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

package integration.auth

import com.google.inject.Inject
import extension.app.UsernameResolutionTestExtension
import fake.FakeProfileService
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import pl.spcode.navauth.common.application.auth.username.PostUsernameResolutionState
import pl.spcode.navauth.common.application.auth.username.UsernameResFailureReason
import pl.spcode.navauth.common.application.auth.username.UsernameResResult
import pl.spcode.navauth.common.application.auth.username.UsernameResolutionService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.application.user.UsernameAlreadyTakenException
import pl.spcode.navauth.common.domain.auth.handshake.AuthHandshakeSession
import pl.spcode.navauth.common.domain.auth.handshake.EncryptionType
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm
import pl.spcode.navauth.common.domain.credentials.TOTPSecret
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.credentials.UserCredentialsRepository
import pl.spcode.navauth.common.domain.mojang.MojangProfile
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserRepository
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.infra.crypto.HashedPassword
import pl.spcode.navauth.common.infra.crypto.PasswordHash
import utils.generateRandomString
import utils.invertCase

/**
 * End-to-end login scenarios: Mojang name changes, released and reclaimed names, premium and
 * non-premium switches. Each test drives [join], which mirrors what navauth-velocity does with one
 * connection, against the real services and an H2 database. The fake Mojang API can be updated (a
 * real rename) or left stale (a cached answer).
 */
@ExtendWith(UsernameResolutionTestExtension::class)
class LoginScenarioTests {

  @Inject private lateinit var mojangApi: FakeProfileService
  @Inject private lateinit var resolution: UsernameResolutionService
  @Inject private lateinit var userService: UserService
  @Inject private lateinit var userRepository: UserRepository
  @Inject private lateinit var credentialsRepository: UserCredentialsRepository

  @BeforeEach
  fun setUp() {
    mojangApi.reset()
  }

  /** What the proxy does with one connection. */
  sealed interface Outcome {
    /** Refused at PreLogin by username resolution. */
    data class Denied(val reason: UsernameResFailureReason) : Outcome

    /** Online mode was enforced and the client could not authenticate with Mojang (cracked). */
    data object MojangAuthFailed : Outcome

    /** Mojang authenticated another account than the stored one; the login is refused. */
    data object IdentityMismatch : Outcome

    /** A premium handshake reached the login without Mojang authentication; refused. */
    data object NotAuthenticatedByMojang : Outcome

    /** Premium auto-login into [user]; credentials are asked only if [User.credentialsRequired]. */
    data class PremiumLogin(val user: User) : Outcome

    /** Offline connection into an existing account: password (and 2FA) required. */
    data class PasswordLogin(val user: User) : Outcome

    /** Offline connection without an account: the player must register. */
    data object Register : Outcome
  }

  /**
   * Mirrors navauth-velocity's `LoginListeners`: PreLogin (username resolution), online-mode
   * authentication, GameProfileRequest (identity check), LoginEvent (denial) and PostLogin (premium
   * account creation).
   *
   * @param client the Mojang account the client really is; null for a cracked client. A non-null
   *   client must own [connUsername] on Mojang right now (the test is the source of truth).
   * @param proxyOnlineMode false when the proxy lets a premium handshake through without Mojang
   *   authentication (PreLogin failed while Velocity runs with online-mode = false, or another
   *   plugin forced offline mode): the connection then carries its offline UUID, whoever the client
   *   is.
   */
  private fun join(
    connUsername: String,
    client: MojangId?,
    proxyOnlineMode: Boolean = true,
  ): Outcome {
    val existing = userService.findUserByUsernameIgnoreCase(connUsername)
    val res = resolution.resolveUsernameConflicts(Username(connUsername), existing)
    if (res is UsernameResResult.Failure) return Outcome.Denied(res.reason)
    res as UsernameResResult.Success

    val sessionUser =
      if (res.postResolutionState != PostUsernameResolutionState.NO_CHANGE) {
        userService.findUserByUsernameIgnoreCase(connUsername)
      } else {
        existing
      }
    val handshake =
      AuthHandshakeSession(
        sessionUser,
        connUsername,
        res.requestedEncryption,
        res.postResolutionState,
      )

    return when (res.requestedEncryption) {
      EncryptionType.ENFORCE_PREMIUM -> {
        val profileId =
          if (proxyOnlineMode) client?.value ?: return Outcome.MojangAuthFailed
          else offlineUuid(connUsername)
        if (!handshake.verifyPremiumIdentity(proxyOnlineMode, profileId)) {
          return if (proxyOnlineMode) Outcome.IdentityMismatch else Outcome.NotAuthenticatedByMojang
        }
        val user =
          sessionUser?.let { userService.removeLeftoverPremiumPassword(it) }
            ?: User.premium(UserUuid(profileId), Username(connUsername), MojangId(profileId)).also {
              userService.storePremiumUser(it)
            }
        Outcome.PremiumLogin(user)
      }
      EncryptionType.NONE ->
        if (sessionUser != null) Outcome.PasswordLogin(sessionUser) else Outcome.Register
    }
  }

  // ---------------------------------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------------------------------

  /** The UUID Velocity gives an offline connection. */
  private fun offlineUuid(name: String): UUID =
    UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray(Charsets.UTF_8))

  /** Letters only, so a letter-case flip always changes the name. */
  private fun newName(): String = generateRandomString(12, withNumbers = false)

  /** Creates a Mojang account that currently owns [name]. */
  private fun mojangAccount(name: String): MojangId {
    val id = MojangId(UUID.randomUUID())
    mojangApi.addProfile(MojangProfile(id, Username(name)))
    return id
  }

  /** The Mojang account [id] changes its name from [from] to [to]; the API sees it at once. */
  private fun mojangRename(id: MojangId, from: String, to: String) {
    mojangApi.removeProfile(Username(from))
    mojangApi.addProfile(MojangProfile(id, Username(to)))
  }

  private fun password() = HashedPassword(PasswordHash("password-hash"), HashingAlgorithm.BCRYPT)

  private fun registerCracked(name: String): User {
    val user = User.nonPremium(UserUuid(UUID.randomUUID()), Username(name))
    userService.createAndStoreUserWithNewCredentials(user, password())
    return user
  }

  private fun premiumLogin(name: String, client: MojangId): User {
    val outcome = join(name, client)
    assertIs<Outcome.PremiumLogin>(outcome, "expected a premium login of $name")
    return outcome.user
  }

  private fun stored(name: String): User? = userService.findUserByUsernameIgnoreCase(name)

  private fun ownedByAnother(storedName: String) =
    Outcome.Denied(UsernameResFailureReason.PremiumUsernameOwnedByAnotherAccount(storedName))

  // ---------------------------------------------------------------------------------------------
  // new players
  // ---------------------------------------------------------------------------------------------

  @Test
  fun `new premium player is stored with their Mojang UUID and logs in without a password`() {
    val name = newName()
    val id = mojangAccount(name)

    val user = premiumLogin(name, id)

    assertEquals(UserUuid(id.value), user.uuid)
    assertEquals(id, user.mojangUuid)
    assertFalse(user.credentialsRequired)
    assertEquals(user.uuid, premiumLogin(name, id).uuid)
  }

  @Test
  fun `new cracked player must register`() {
    assertEquals(Outcome.Register, join(newName(), null))
  }

  @Test
  fun `cracked client cannot join with a premium name nobody registered yet`() {
    val name = newName()
    mojangAccount(name)

    assertEquals(Outcome.MojangAuthFailed, join(name, null))
    assertNull(stored(name))
  }

  @Test
  fun `new premium player must use the letter case of their Mojang name`() {
    val name = newName()
    val id = mojangAccount(name)

    assertEquals(
      Outcome.Denied(UsernameResFailureReason.PremiumUsernameNotIdentical(name)),
      join(invertCase(name), id),
    )
  }

  @Test
  fun `Mojang API failure refuses the login instead of guessing`() {
    val name = newName()
    val id = mojangAccount(name)
    premiumLogin(name, id)
    mojangApi.failing = true

    assertEquals(Outcome.Denied(UsernameResFailureReason.ProfileAPIFailure), join(name, id))
    assertEquals(Outcome.Denied(UsernameResFailureReason.ProfileAPIFailure), join(newName(), null))
  }

  // ---------------------------------------------------------------------------------------------
  // returning premium players and Mojang name changes
  // ---------------------------------------------------------------------------------------------

  @Test
  fun `returning premium player must keep the letter case of their account`() {
    val name = newName()
    val id = mojangAccount(name)
    premiumLogin(name, id)

    assertEquals(
      Outcome.Denied(UsernameResFailureReason.PremiumUsernameNotIdentical(name)),
      join(invertCase(name), id),
    )
  }

  @Test
  fun `premium player who renamed on Mojang keeps their account under the new name`() {
    val oldName = newName()
    val newName = newName()
    val id = mojangAccount(oldName)
    val account = premiumLogin(oldName, id)

    mojangRename(id, oldName, newName)
    val renamed = premiumLogin(newName, id)

    assertEquals(account.uuid, renamed.uuid)
    assertEquals(Username(newName), renamed.username)
    assertNull(stored(oldName))
  }

  @Test
  fun `Mojang letter case change renames the stored account`() {
    val name = newName()
    val id = mojangAccount(name)
    val account = premiumLogin(name, id)

    val newCase = invertCase(name)
    mojangRename(id, name, newCase)
    val renamed = premiumLogin(newCase, id)

    assertEquals(account.uuid, renamed.uuid)
    assertEquals(Username(newCase), stored(name)!!.username)
  }

  @Test
  fun `premium player who renamed onto a cracked account's name gets that account's password login`() {
    val oldName = newName()
    val takenName = newName()
    val id = mojangAccount(oldName)
    val account = premiumLogin(oldName, id)
    val cracked = registerCracked(takenName)

    mojangRename(id, oldName, takenName)

    // the cracked account stays protected by its password; the premium account is not renamed
    assertEquals(Outcome.PasswordLogin(cracked), join(takenName, id))
    assertEquals(account.uuid, stored(oldName)!!.uuid)
  }

  @Test
  fun `premium player with 2FA must still enter the code`() {
    val name = newName()
    val id = mojangAccount(name)
    userService.enableTwoFactorAuth(premiumLogin(name, id), TOTPSecret("JBSWY3DPEHPK3PXP"))

    val user = premiumLogin(name, id)

    assertTrue(user.credentialsRequired)
  }

  // ---------------------------------------------------------------------------------------------
  // a released premium name claimed by another Mojang account (account takeover attempts)
  // ---------------------------------------------------------------------------------------------

  @Test
  fun `new owner of a released premium name cannot log into the previous owner's account`() {
    val released = newName()
    val previousOwner = mojangAccount(released)
    val account = premiumLogin(released, previousOwner)

    mojangRename(previousOwner, released, newName()) // previous owner does not join afterwards
    val newOwner = mojangAccount(released)

    assertEquals(ownedByAnother(released), join(released, newOwner))
    val unchanged = stored(released)!!
    assertEquals(account.uuid, unchanged.uuid)
    assertEquals(previousOwner, unchanged.mojangUuid)
  }

  @Test
  fun `new owner of a released name in another letter case neither logs in nor renames the account`() {
    val released = newName()
    val previousOwner = mojangAccount(released)
    premiumLogin(released, previousOwner)

    mojangRename(previousOwner, released, newName())
    val newSpelling = invertCase(released)
    val newOwner = mojangAccount(newSpelling)

    assertEquals(ownedByAnother(released), join(newSpelling, newOwner))
    assertEquals(Username(released), stored(released)!!.username)
  }

  @Test
  fun `stale profile cache still naming the previous owner does not hand out the account`() {
    val released = newName()
    val previousOwner = mojangAccount(released)
    val account = premiumLogin(released, previousOwner)

    // the name changed hands on Mojang, but the cached API answer still names the previous owner
    val newOwner = MojangId(UUID.randomUUID())

    assertEquals(Outcome.IdentityMismatch, join(released, newOwner))
    assertEquals(account.uuid, stored(released)!!.uuid)
  }

  @Test
  fun `stale not-found cache for a reclaimed name does not hand out the account`() {
    val released = newName()
    val previousOwner = mojangAccount(released)
    premiumLogin(released, previousOwner)

    // the previous owner renamed; the cache still says the name has no Mojang account
    mojangApi.removeProfile(Username(released))
    val newOwner = MojangId(UUID.randomUUID())

    assertEquals(Outcome.IdentityMismatch, join(released, newOwner))
  }

  @Test
  fun `released name is freed once the previous owner joins with their new name`() {
    val released = newName()
    val laterName = newName()
    val previousOwner = mojangAccount(released)
    val previousAccount = premiumLogin(released, previousOwner)

    mojangRename(previousOwner, released, laterName)
    val newOwner = mojangAccount(released)
    assertEquals(ownedByAnother(released), join(released, newOwner))

    assertEquals(previousAccount.uuid, premiumLogin(laterName, previousOwner).uuid)
    val newAccount = premiumLogin(released, newOwner)

    assertNotEquals(previousAccount.uuid, newAccount.uuid)
    assertEquals(newOwner, newAccount.mojangUuid)
  }

  @Test
  fun `player with an existing account who claims a freed name keeps their own account`() {
    val released = newName()
    val claimantName = newName()
    val previousOwner = mojangAccount(released)
    val claimant = mojangAccount(claimantName)
    premiumLogin(released, previousOwner)
    val claimantAccount = premiumLogin(claimantName, claimant)

    val laterName = newName()
    mojangRename(previousOwner, released, laterName)
    premiumLogin(laterName, previousOwner)
    mojangRename(claimant, claimantName, released)

    val renamed = premiumLogin(released, claimant)
    assertEquals(claimantAccount.uuid, renamed.uuid)
  }

  @Test
  fun `two premium players who swapped names are both refused until one is resolved`() {
    val nameA = newName()
    val nameB = newName()
    val playerA = mojangAccount(nameA)
    val playerB = mojangAccount(nameB)
    premiumLogin(nameA, playerA)
    premiumLogin(nameB, playerB)

    mojangApi.addProfile(MojangProfile(playerB, Username(nameA)))
    mojangApi.addProfile(MojangProfile(playerA, Username(nameB)))

    assertEquals(ownedByAnother(nameA), join(nameA, playerB))
    assertEquals(ownedByAnother(nameB), join(nameB, playerA))
  }

  @Test
  fun `cracked client cannot use a premium account's name after its owner renamed`() {
    val released = newName()
    val previousOwner = mojangAccount(released)
    premiumLogin(released, previousOwner)
    mojangRename(previousOwner, released, newName())

    assertEquals(Outcome.MojangAuthFailed, join(released, null))
  }

  // ---------------------------------------------------------------------------------------------
  // a premium handshake the proxy let through offline (PreLogin failed with Velocity
  // online-mode = false, or another plugin forced offline mode)
  // ---------------------------------------------------------------------------------------------

  @Test
  fun `offline connection never logs into an existing premium account`() {
    val name = newName()
    val owner = mojangAccount(name)
    val account = premiumLogin(name, owner)

    assertEquals(Outcome.NotAuthenticatedByMojang, join(name, null, proxyOnlineMode = false))
    // without Mojang authentication not even the owner's own client is accepted
    assertEquals(Outcome.NotAuthenticatedByMojang, join(name, owner, proxyOnlineMode = false))

    val unchanged = stored(name)!!
    assertEquals(account.uuid, unchanged.uuid)
    assertEquals(Username(name), unchanged.username)
    assertEquals(owner, unchanged.mojangUuid)
    assertNull(userService.findUserByUuid(UserUuid(offlineUuid(name))))
    assertEquals(account.uuid, premiumLogin(name, owner).uuid)
  }

  @Test
  fun `offline connection with a new premium name creates no account`() {
    val name = newName()
    val owner = mojangAccount(name)

    assertEquals(Outcome.NotAuthenticatedByMojang, join(name, null, proxyOnlineMode = false))

    assertNull(stored(name))
    assertNull(userService.findUserByUuid(UserUuid(offlineUuid(name))))
    // the name stays free for its Mojang owner
    assertEquals(owner, premiumLogin(name, owner).mojangUuid)
  }

  // ---------------------------------------------------------------------------------------------
  // cracked accounts and premium names
  // ---------------------------------------------------------------------------------------------

  @Test
  fun `Mojang owner of a cracked account's name gets a password login, never an auto-login`() {
    val name = newName()
    val cracked = registerCracked(name)
    val mojangOwner = mojangAccount(name)

    assertEquals(Outcome.PasswordLogin(cracked), join(name, mojangOwner))
    assertEquals(Outcome.PasswordLogin(cracked), join(name, null))
  }

  @Test
  fun `cracked account whose name is premium in another letter case is refused`() {
    val name = newName()
    registerCracked(name)
    mojangAccount(invertCase(name))

    assertEquals(
      Outcome.Denied(UsernameResFailureReason.NonPremiumWithPremiumConflict(invertCase(name))),
      join(name, null),
    )
  }

  @Test
  fun `cracked player must keep the letter case of their account`() {
    val name = newName()
    registerCracked(name)

    assertEquals(
      Outcome.Denied(UsernameResFailureReason.NonPremiumUsernameNotIdentical(name)),
      join(invertCase(name), null),
    )
  }

  // ---------------------------------------------------------------------------------------------
  // switching between cracked and premium
  // ---------------------------------------------------------------------------------------------

  @Test
  fun `cracked player switching to premium keeps their UUID and loses the password`() {
    val name = newName()
    val cracked = registerCracked(name)
    val id = mojangAccount(name)

    userService.migrateToPremium(cracked, id)
    val user = premiumLogin(name, id)

    assertEquals(cracked.uuid, user.uuid)
    assertEquals(id, user.mojangUuid)
    assertFalse(user.credentialsRequired)
    assertNull(credentialsRepository.findByUser(user))
  }

  @Test
  fun `cracked player with 2FA switching to premium keeps only the 2FA secret`() {
    val name = newName()
    val cracked = registerCracked(name)
    userService.enableTwoFactorAuth(cracked, TOTPSecret("JBSWY3DPEHPK3PXP"))
    val id = mojangAccount(name)

    userService.migrateToPremium(stored(name)!!, id)
    val user = premiumLogin(name, id)

    assertTrue(user.credentialsRequired)
    val credentials = assertNotNull(credentialsRepository.findByUser(user))
    assertNull(credentials.hashedPassword)
    assertTrue(credentials.isTwoFactorEnabled)
  }

  @Test
  fun `premium player switched to cracked logs in with a password even from the Mojang account`() {
    val name = newName()
    val id = mojangAccount(name)
    val premium = premiumLogin(name, id)

    val cracked = userService.migrateToNonPremium(premium, password())

    assertEquals(premium.uuid, cracked.uuid)
    assertEquals(Outcome.PasswordLogin(cracked), join(name, null))
    assertEquals(Outcome.PasswordLogin(cracked), join(name, id))
  }

  // ---------------------------------------------------------------------------------------------
  // renames by commands and imperfect data
  // ---------------------------------------------------------------------------------------------

  @Test
  fun `renaming onto a name another account holds in any letter case is refused`() {
    val first = registerCracked(newName())
    val second = registerCracked(newName())

    assertFailsWith<UsernameAlreadyTakenException> {
      userService.migrateUsername(first, Username(invertCase(second.username.value)))
    }
  }

  @Test
  fun `renaming to the same name is refused, to another letter case is allowed`() {
    val user = registerCracked(newName())

    assertFailsWith<IllegalArgumentException> { userService.migrateUsername(user, user.username) }
    val newCase = Username(invertCase(user.username.value))
    assertEquals(newCase, userService.migrateUsername(user, newCase).username)
  }

  @Test
  fun `cracked data cannot be moved onto a premium name`() {
    val user = registerCracked(newName())
    val premiumName = newName()
    mojangAccount(premiumName)

    assertFailsWith<IllegalArgumentException> {
      userService.migrateData(user, Username(premiumName))
    }
  }

  @Test
  fun `duplicate rows for one name resolve to the lowest uuid and logins keep working`() {
    val name = newName()
    val id = mojangAccount(name)
    val low = UserUuid(UUID.fromString("00000000-0000-0000-0000-${randomHex(12)}"))
    val high = UserUuid(UUID.fromString("ffffffff-ffff-ffff-ffff-${randomHex(12)}"))
    userRepository.save(User.premium(high, Username(name), MojangId(UUID.randomUUID())))
    userRepository.save(User.premium(low, Username(name), id))

    assertEquals(low, stored(name)!!.uuid)
    assertEquals(low, premiumLogin(name, id).uuid)
  }

  @Test
  fun `renamed premium player joining with the wrong case of the new name is renamed but refused`() {
    val oldName = newName()
    val newName = newName()
    val id = mojangAccount(oldName)
    val account = premiumLogin(oldName, id)
    mojangRename(id, oldName, newName)

    assertEquals(
      Outcome.Denied(UsernameResFailureReason.PremiumUsernameNotIdentical(newName)),
      join(invertCase(newName), id),
    )
    // the rename to the Mojang spelling already happened; the right spelling logs in
    assertEquals(account.uuid, stored(newName)!!.uuid)
    assertEquals(account.uuid, premiumLogin(newName, id).uuid)
  }

  @Test
  fun `rename made from a stale profile answer grants nothing and is undone by the owner's next join`() {
    val ownerName = newName()
    val otherName = newName()
    val owner = mojangAccount(ownerName)
    val ownerAccount = premiumLogin(ownerName, owner)

    // stale cache: `otherName` still points to the owner, but another account owns it now
    mojangApi.addProfile(MojangProfile(owner, Username(otherName)))
    val newOwner = MojangId(UUID.randomUUID())
    assertEquals(Outcome.IdentityMismatch, join(otherName, newOwner))
    assertEquals(ownerAccount.uuid, stored(otherName)!!.uuid) // renamed before verification

    // the cache catches up
    mojangApi.addProfile(MojangProfile(owner, Username(ownerName)))
    mojangApi.addProfile(MojangProfile(newOwner, Username(otherName)))
    assertEquals(ownerAccount.uuid, premiumLogin(ownerName, owner).uuid)
    val newAccount = premiumLogin(otherName, newOwner)
    assertNotEquals(ownerAccount.uuid, newAccount.uuid)
  }

  @Test
  fun `premium player with 2FA switched to cracked keeps 2FA and gets the password`() {
    val name = newName()
    val id = mojangAccount(name)
    userService.enableTwoFactorAuth(premiumLogin(name, id), TOTPSecret("JBSWY3DPEHPK3PXP"))

    val cracked = userService.migrateToNonPremium(stored(name)!!, password())

    val credentials = assertNotNull(credentialsRepository.findByUser(cracked))
    assertEquals(password(), credentials.hashedPassword)
    assertTrue(credentials.isTwoFactorEnabled)
    assertEquals(Outcome.PasswordLogin(cracked), join(name, null))
  }

  @Test
  fun `2FA on a premium account requires credentials and disabling it removes them`() {
    val name = newName()
    val id = mojangAccount(name)
    val premium = premiumLogin(name, id)

    userService.enableTwoFactorAuth(premium, TOTPSecret("JBSWY3DPEHPK3PXP"))
    val withTwoFactor = stored(name)!!
    assertTrue(withTwoFactor.credentialsRequired)
    assertNull(assertNotNull(credentialsRepository.findByUser(withTwoFactor)).hashedPassword)

    userService.disableTwoFactorAuth(withTwoFactor)
    val without = stored(name)!!
    assertFalse(without.credentialsRequired)
    assertNull(credentialsRepository.findByUser(without))
    assertFalse(premiumLogin(name, id).credentialsRequired)
  }

  @Test
  fun `disabling 2FA on a cracked account keeps the password`() {
    val name = newName()
    val cracked = registerCracked(name)
    userService.enableTwoFactorAuth(cracked, TOTPSecret("JBSWY3DPEHPK3PXP"))

    userService.disableTwoFactorAuth(stored(name)!!)

    val credentials = assertNotNull(credentialsRepository.findByUser(cracked))
    assertEquals(password(), credentials.hashedPassword)
    assertFalse(credentials.isTwoFactorEnabled)
    assertTrue(stored(name)!!.credentialsRequired)
  }

  // ---------------------------------------------------------------------------------------------
  // passwords imports left on premium accounts (#46)
  // ---------------------------------------------------------------------------------------------

  /** A premium account as an older LibreLogin import stored it: password and credentials. */
  private fun importedPremiumWithPassword(name: String, id: MojangId, totp: String?): User {
    val user = User.premium(UserUuid(UUID.randomUUID()), Username(name), id, true)
    userRepository.save(user)
    credentialsRepository.save(
      UserCredentials.create(user, password(), totp?.let { TOTPSecret(it) })
    )
    return user
  }

  @Test
  fun `premium account an import left with a password logs in automatically and loses it`() {
    val name = newName()
    val id = mojangAccount(name)
    val imported = importedPremiumWithPassword(name, id, totp = null)

    val user = premiumLogin(name, id)

    assertEquals(imported.uuid, user.uuid)
    assertFalse(user.credentialsRequired)
    assertNull(credentialsRepository.findByUser(user))
  }

  @Test
  fun `premium account an import left with a password and 2FA keeps only 2FA`() {
    val name = newName()
    val id = mojangAccount(name)
    importedPremiumWithPassword(name, id, totp = "JBSWY3DPEHPK3PXP")

    val user = premiumLogin(name, id)

    assertTrue(user.credentialsRequired)
    val credentials = assertNotNull(credentialsRepository.findByUser(user))
    assertNull(credentials.hashedPassword)
    assertTrue(credentials.isTwoFactorEnabled)
  }

  @Test
  fun `leftover password stays until the owner is verified by Mojang`() {
    val name = newName()
    val id = mojangAccount(name)
    val imported = importedPremiumWithPassword(name, id, totp = null)

    assertEquals(Outcome.MojangAuthFailed, join(name, null))
    assertEquals(Outcome.NotAuthenticatedByMojang, join(name, null, proxyOnlineMode = false))
    assertEquals(Outcome.IdentityMismatch, join(name, MojangId(UUID.randomUUID())))

    assertTrue(stored(name)!!.credentialsRequired)
    assertNotNull(credentialsRepository.findByUser(imported)?.hashedPassword)
  }

  @Test
  fun `cracked account keeps its password on login`() {
    val name = newName()
    val cracked = registerCracked(name)

    assertEquals(Outcome.PasswordLogin(cracked), join(name, null))
    assertEquals(cracked, userService.removeLeftoverPremiumPassword(cracked))
    assertNotNull(credentialsRepository.findByUser(cracked)?.hashedPassword)
  }

  private fun randomHex(length: Int): String =
    (1..length).map { "0123456789abcdef".random() }.joinToString("")
}
