package org.briarproject.mailbox.core

import org.briarproject.mailbox.core.system.TaskScheduler
import org.briarproject.mailbox.core.tor.JavaTorPlugin
import javax.inject.Inject

@Suppress("unused")
internal class JavaCliEagerSingletons @Inject constructor(
    val taskScheduler: TaskScheduler,
    val javaTorPlugin: JavaTorPlugin,
)
