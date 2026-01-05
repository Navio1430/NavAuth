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

package pl.spcode.navauth.velocity.command.admin

// We suspended use of unregister command because forceful password change is way better.
//
// @Command(name = "forceunregister")
// @Permission(Permissions.ADMIN_FORCE_UNREGISTER)
// class ForceUnregisterAdminCommand
// @Inject
// constructor(
//  val userService: UserService,
//  val userCredentialsService: UserCredentialsService,
//  val userArgumentResolver: UserArgumentResolver,
// ) {
//
//  @Async
//  @Execute
//  @Description(
//    "Force unregister specified user. Works like unregister command, but doesn't require
// password."
//  )
//  fun forceUnregister(
//    @Context sender: CommandSource,
//    @Arg(value = "username|uuid") usernameOrUuidRaw: UsernameOrUuidRaw,
//  ) {
//    val user = userArgumentResolver.resolve(usernameOrUuidRaw)
//
//    if (user.isPremium) {
//      sender.sendMessage(
//        Component.text(
//          "Can't execute the command! Account '${user.username}' is set to premium mode.",
//          TextColors.RED,
//        )
//      )
//      return
//    }
//
//    val userCredentials = userCredentialsService.findCredentials(user)
//    if (userCredentials == null) {
//      sender.sendMessage(Component.text("User is already unregistered.", TextColors.RED))
//      return
//    }
//
//    userCredentialsService.deleteUserCredentialsOnly(user)
//    sender.sendMessage(
//      Component.text("Success! User '${user.username}' credentials deleted.", TextColors.GREEN)
//    )
//  }
// }
