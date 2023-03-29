/*
 *     Briar Mailbox
 *     Copyright (C) 2021-2022  The Briar Project
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package org.briarproject.mailbox.lib

import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import org.briarproject.mailbox.core.CoreEagerSingletons
import org.briarproject.mailbox.core.MailboxLibEagerSingletons
import org.briarproject.mailbox.core.db.TransactionManager
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.server.WebServerManager
import org.briarproject.mailbox.core.setup.QrCodeEncoder
import org.briarproject.mailbox.core.setup.SetupManager
import org.briarproject.mailbox.core.setup.WipeManager
import org.briarproject.mailbox.core.system.System
import org.briarproject.mailbox.core.tor.TorPlugin
import org.briarproject.mailbox.core.tor.TorPluginState
import org.briarproject.mailbox.core.util.LogUtils.info
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import java.io.File
import javax.inject.Inject

abstract class AbstractMailbox(protected val customDataDir: File? = null) {

    companion object {
        val LOG: Logger = getLogger(AbstractMailbox::class.java)
    }

    @Inject
    internal lateinit var coreEagerSingletons: CoreEagerSingletons

    @Inject
    internal lateinit var mailboxLibEagerSingletons: MailboxLibEagerSingletons

    @Inject
    internal lateinit var lifecycleManager: LifecycleManager

    @Inject
    internal lateinit var db: TransactionManager

    @Inject
    internal lateinit var setupManager: SetupManager

    @Inject
    internal lateinit var webserverManager: WebServerManager

    @Inject
    internal lateinit var wipeManager: WipeManager

    @Inject
    internal lateinit var torPlugin: TorPlugin

    @Inject
    internal lateinit var qrCodeEncoder: QrCodeEncoder

    @Inject
    internal lateinit var system: System

    fun wipeFilesOnly() {
        wipeManager.wipeFilesOnly()
        LOG.info { "Mailbox wiped successfully \\o/" }
    }

    fun startLifecycle() {
        LOG.info { "Starting lifecycle" }
        lifecycleManager.startServices()
        LOG.info { "Waiting for startup" }
        lifecycleManager.waitForStartup()
        LOG.info { "Startup finished" }
    }

    fun stopLifecycle(exitAfterStopping: Boolean) {
        LOG.info { "Stopping lifecycle" }
        lifecycleManager.stopServices(exitAfterStopping)
        LOG.info { "Waiting for shutdown" }
        lifecycleManager.waitForShutdown()
        LOG.info { "Shutdown finished" }
    }

    fun setSetupToken(token: String) {
        setupManager.setToken(token, null)
    }

    fun waitForTorPublished() {
        LOG.info { "Waiting for Tor to publish hidden service" }
        runBlocking {
            // wait until Tor becomes active and published the onion service
            torPlugin.state.takeWhile { state ->
                state != TorPluginState.Published
            }.collect { }
        }
        LOG.info { "Hidden service published" }
    }

    fun waitForShutdown() {
        lifecycleManager.waitForShutdown()
    }

    fun getSetupToken(): String? {
        return db.read { txn ->
            setupManager.getSetupToken(txn)
        }
    }

    fun getOwnerToken(): String? {
        return db.read { txn ->
            setupManager.getOwnerToken(txn)
        }
    }

    fun getQrCode(): BitMatrix? {
        return qrCodeEncoder.getQrCodeBitMatrix()
    }

    fun getLink(): String? {
        return qrCodeEncoder.getLink()
    }

    /**
     * The port, the webserver has bound to.
     * Accessing this will block the current thread until the port chosen by the webserver is known.
     */
    val port get() = webserverManager.port

    fun getSystem(): System = system
}
