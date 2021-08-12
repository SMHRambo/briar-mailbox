package org.briarproject.mailbox.android

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.CoreModule
import org.briarproject.mailbox.core.tor.AndroidTorModule

@Module(
    includes = [
        CoreModule::class,
        AndroidTorModule::class,
    ]
)
@InstallIn(SingletonComponent::class)
internal class AppModule
