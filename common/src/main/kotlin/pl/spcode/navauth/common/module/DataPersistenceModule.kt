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

package pl.spcode.navauth.common.module

import com.google.inject.AbstractModule
import com.google.inject.Singleton
import kotlin.reflect.KClass
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserRepository
import pl.spcode.navauth.common.infra.database.DatabaseConfig
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.infra.database.EntitiesRegistrar
import pl.spcode.navauth.common.infra.repository.UserRepositoryImpl

class DataPersistenceModule(val databaseConfig: DatabaseConfig) : AbstractModule() {

  private data class Binding(val entity: KClass<*>, val repo: Class<*>, val impl: Class<*>)

  private val bindings =
    listOf(Binding(User::class, UserRepository::class.java, UserRepositoryImpl::class.java))

  override fun configure() {
    val manager = DatabaseManager(databaseConfig)

    bind(DatabaseManager::class.java).toInstance(manager)

    val entitiesRegistrar = EntitiesRegistrar()

    for ((entity, repo, impl) in bindings) {
      @Suppress("UNCHECKED_CAST") bind(repo as Class<Any>).to(impl).`in`(Singleton::class.java)

      entitiesRegistrar.registerEntity(entity)
    }

    manager.connectAndInit(entitiesRegistrar)
  }
}
