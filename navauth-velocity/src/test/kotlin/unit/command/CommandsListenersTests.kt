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

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.RootCommandNode
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.event.command.CommandExecuteEvent
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent
import com.velocitypowered.api.proxy.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.UUID
import pl.spcode.navauth.common.application.auth.session.AuthSessionService
import pl.spcode.navauth.common.command.configurer.CommandConfig
import pl.spcode.navauth.common.config.CommandsConfig
import pl.spcode.navauth.common.domain.auth.session.AuthSession
import pl.spcode.navauth.velocity.infra.player.VelocityPlayerAdapter
import pl.spcode.navauth.velocity.listener.velocity.CommandsListeners

/**
 * A player who is not logged in may run only the auth commands (login, register, 2fa) under the
 * names and aliases `commands.yml` gives them.
 */
class CommandsListenersTests :
  FunSpec({
    fun player(): Player {
      val player = mockk<Player>(relaxed = true)
      every { player.username } returns "Player"
      every { player.uniqueId } returns UUID.randomUUID()
      every { player.remoteAddress } returns
        InetSocketAddress(InetAddress.getByName("192.0.2.1"), 54321)
      return player
    }

    fun config(vararg commands: Pair<String, CommandConfig>) =
      CommandsConfig().apply { this.commands = mutableMapOf(*commands) }

    /** Listeners for a player without an auth session (not logged in). */
    fun listeners(config: CommandsConfig) = CommandsListeners(AuthSessionService(), config)

    fun CommandsListeners.allows(command: String): Boolean {
      val event = CommandExecuteEvent(player(), command)
      onCommand(event)
      return event.result.isAllowed
    }

    test("default commands.yml allows login, register and 2fa with their aliases") {
      CommandsListeners.allowedCommandNames(CommandsConfig()) shouldBe
        setOf("login", "l", "register", "reg", "2fa")

      val listeners = listeners(CommandsConfig())
      listeners.allows("login secret").shouldBeTrue()
      listeners.allows("l secret").shouldBeTrue()
      listeners.allows("reg secret secret").shouldBeTrue()
      listeners.allows("2fa 123456").shouldBeTrue()
      listeners.allows("spawn").shouldBeFalse()
    }

    test("a renamed login command is allowed under its new name and aliases only") {
      val listeners =
        listeners(
          config(
            "login" to
              CommandConfig(name = "zaloguj", aliases = mutableListOf("zal", ""), enabled = true)
          )
        )

      listeners.allows("zaloguj secret").shouldBeTrue()
      listeners.allows("zal secret").shouldBeTrue()
      listeners.allows("login secret").shouldBeFalse()
      listeners.allows("l secret").shouldBeFalse()
      // commands without an entry keep their own name
      listeners.allows("register secret secret").shouldBeTrue()
      listeners.allows("2fa 123456").shouldBeTrue()
    }

    test("an entry without a name keeps the original name with the configured aliases") {
      val listeners =
        listeners(
          config("register" to CommandConfig(name = "", aliases = mutableListOf("rejestracja")))
        )

      listeners.allows("register secret secret").shouldBeTrue()
      listeners.allows("rejestracja secret secret").shouldBeTrue()
      listeners.allows("reg secret secret").shouldBeFalse()
    }

    test("a disabled auth command is not allowed") {
      val listeners =
        listeners(
          config(
            "register" to
              CommandConfig(name = "register", aliases = mutableListOf("reg"), enabled = false)
          )
        )

      listeners.allows("register secret secret").shouldBeFalse()
      listeners.allows("reg secret secret").shouldBeFalse()
      listeners.allows("login secret").shouldBeTrue()
    }

    test("command names match in any letter case, like Velocity matches them") {
      val listeners =
        listeners(config("login" to CommandConfig(name = "Zaloguj", aliases = mutableListOf())))

      listeners.allows("ZALOGUJ secret").shouldBeTrue()
      listeners.allows("zaloguj secret").shouldBeTrue()
    }

    test("the command list of a player who is not logged in shows only the auth commands") {
      val session = mockk<AuthSession<VelocityPlayerAdapter>>()
      every { session.isAuthenticated } returns false
      val authSessionService = mockk<AuthSessionService<VelocityPlayerAdapter>>()
      every { authSessionService.findSession(any()) } returns session
      val listeners =
        CommandsListeners(
          authSessionService,
          config("login" to CommandConfig(name = "zaloguj", aliases = mutableListOf("zal"))),
        )
      val root = RootCommandNode<CommandSource>()
      listOf("zaloguj", "zal", "login", "register", "spawn").forEach {
        root.addChild(LiteralArgumentBuilder.literal<CommandSource>(it).build())
      }

      listeners.onPlayerAvailableCommands(PlayerAvailableCommandsEvent(player(), root))

      root.children.map { it.name }.toSet() shouldBe setOf("zaloguj", "zal", "register")
    }
  })
