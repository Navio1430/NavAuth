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
import com.google.inject.Provides
import com.google.inject.Singleton
import org.jetbrains.exposed.v1.jdbc.Database
import pl.spcode.navauth.common.domain.credentials.UserCredentialsRepository
import pl.spcode.navauth.common.domain.user.UserRepository
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.infra.persistence.impl.credentials.UserCredentialsRepositoryImpl
import pl.spcode.navauth.common.infra.persistence.impl.user.UserRepositoryImpl

class DataPersistenceModule : AbstractModule() {

  override fun configure() {
    bind(UserRepository::class.java).to(UserRepositoryImpl::class.java).`in`(Singleton::class.java)
    bind(UserCredentialsRepository::class.java)
      .to(UserCredentialsRepositoryImpl::class.java)
      .`in`(Singleton::class.java)

    bind(DatabaseManager::class.java).`in`(Singleton::class.java)
  }

  @Provides
  @Singleton
  fun provideDatabase(databaseManager: DatabaseManager): Database = databaseManager.database
}
