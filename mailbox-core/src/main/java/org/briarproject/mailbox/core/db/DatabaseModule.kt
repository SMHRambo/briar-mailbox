package org.briarproject.mailbox.core.db

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.system.Clock
import java.sql.Connection
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(config: DatabaseConfig, clock: Clock): Database<Connection> {
        return H2Database(config, clock)
    }

    @Provides
    @Singleton
    fun provideDatabaseComponent(db: Database<Connection>): DatabaseComponent {
        return DatabaseComponentImpl(db)
    }

}
