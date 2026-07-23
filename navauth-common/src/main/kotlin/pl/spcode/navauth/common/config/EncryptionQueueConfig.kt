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

package pl.spcode.navauth.common.config

import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.Comment

class EncryptionQueueConfig : OkaeriConfig() {

  @Comment(
    "Minimum (core) number of threads kept alive in the encryption thread pool even when idle.",
    "These threads handle password hashing and verification.",
    "(Bcrypt and Argon2 are very memory-intensive. 2-4 is a safe range.)",
  )
  var encryptionQueueMinThreads: Int = 2

  @Comment(
    "Maximum number of threads the encryption pool can grow to under load.",
    "Extra threads are created only when the work queue is full.",
    "Set higher if you have a powerful server with many concurrent registrations/logins.",
  )
  var encryptionQueueMaxThreads: Int = 4
}
