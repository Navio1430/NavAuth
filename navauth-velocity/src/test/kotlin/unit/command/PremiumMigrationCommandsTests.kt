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

package unit.command

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.permission.Tristate
import com.velocitypowered.api.proxy.Player
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import pl.spcode.navauth.common.application.mojang.ProfileService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.command.user.UserArgumentResolver
import pl.spcode.navauth.common.command.user.UsernameOrUuidRaw
import pl.spcode.navauth.common.domain.mojang.MojangProfile
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.velocity.command.admin.ForcePremiumAdminCommand
import pl.spcode.navauth.velocity.command.user.PremiumAccountCommand
import pl.spcode.navauth.velocity.multification.VelocityMultification

/**
 * `/premium` and `/forcepremium` never attach a Mojang account that another account already holds.
 */
class PremiumMigrationCommandsTests :
  FunSpec({
    val mojangId = MojangId(UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6"))
    val profile = MojangProfile(mojangId, Username("Notch"))

    lateinit var userService: UserService
    lateinit var profileService: ProfileService
    lateinit var multification: VelocityMultification
    lateinit var user: User

    beforeTest {
      userService = mockk(relaxed = true)
      profileService = mockk()
      multification = mockk(relaxed = true)
      user = User.nonPremium(UserUuid(UUID.randomUUID()), Username("Notch"))
      every { profileService.fetchProfileInfo(Username("Notch"), any()) } returns profile
      every { userService.findUserByExactUsername("Notch") } returns user
    }

    fun player(): Player {
      val player = mockk<Player>(relaxed = true)
      every { player.username } returns "Notch"
      every { player.getPermissionValue(any()) } returns Tristate.UNDEFINED
      return player
    }

    fun otherAccount(): User =
      User.premium(UserUuid(UUID.randomUUID()), Username("Other"), mojangId)

    fun forcePremium(): ForcePremiumAdminCommand {
      val resolver = mockk<UserArgumentResolver>()
      every { resolver.resolve(UsernameOrUuidRaw("Notch")) } returns user
      return ForcePremiumAdminCommand(userService, profileService, resolver, multification)
    }

    test("/premium refuses a Mojang account linked to another account") {
      every { userService.findUserByMojangUuid(mojangId) } returns otherAccount()

      PremiumAccountCommand(userService, profileService, multification)
        .changeToPremiumAccount(player())

      verify(exactly = 0) { userService.migrateToPremium(user, mojangId) }
    }

    test("/premium migrates when no other account holds the Mojang account") {
      every { userService.findUserByMojangUuid(mojangId) } returns null

      PremiumAccountCommand(userService, profileService, multification)
        .changeToPremiumAccount(player())

      verify(exactly = 1) { userService.migrateToPremium(user, mojangId) }
    }

    test("/forcepremium refuses a Mojang account linked to another account") {
      every { userService.findUserByMojangUuid(mojangId) } returns otherAccount()

      forcePremium()
        .forcePremiumMode(mockk<CommandSource>(relaxed = true), UsernameOrUuidRaw("Notch"))

      verify(exactly = 0) { userService.migrateToPremium(user, mojangId) }
    }

    test("/forcepremium migrates when no other account holds the Mojang account") {
      every { userService.findUserByMojangUuid(mojangId) } returns null

      forcePremium()
        .forcePremiumMode(mockk<CommandSource>(relaxed = true), UsernameOrUuidRaw("Notch"))

      verify(exactly = 1) { userService.migrateToPremium(user, mojangId) }
    }
  })
