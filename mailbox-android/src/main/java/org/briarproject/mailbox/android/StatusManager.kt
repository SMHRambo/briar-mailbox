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

package org.briarproject.mailbox.android

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.briarproject.android.dontkillmelib.DozeHelper
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.StatusManager.DbState.DB_DOES_NOT_EXIST
import org.briarproject.mailbox.android.StatusManager.DbState.DB_EXISTS
import org.briarproject.mailbox.android.StatusManager.DbState.DB_UNKNOWN
import org.briarproject.mailbox.android.StatusManager.DozeExemptionState.DOES_NOT_NEED_DOZE_EXEMPTION
import org.briarproject.mailbox.android.StatusManager.DozeExemptionState.NEEDS_DOZE_EXEMPTION
import org.briarproject.mailbox.android.StatusManager.DozeExemptionState.NEEDS_DOZE_EXEMPTION_UNKNOWN
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState
import org.briarproject.mailbox.core.setup.QrCodeEncoder
import org.briarproject.mailbox.core.setup.SetupComplete
import org.briarproject.mailbox.core.setup.SetupManager
import org.briarproject.mailbox.core.tor.TorPlugin
import org.briarproject.mailbox.core.tor.TorState
import org.briarproject.mailbox.core.util.LogUtils.info
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class StatusManager @Inject constructor(
    @ApplicationContext private val context: Context,
    lifecycleManager: LifecycleManager,
    private val notificationManager: MailboxNotificationManager,
    setupManager: SetupManager,
    private val qrCodeEncoder: QrCodeEncoder,
    torPlugin: TorPlugin,
    dozeHelper: DozeHelper,
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(StatusManager::class.java)
    }

    private enum class DbState {
        DB_UNKNOWN,
        DB_EXISTS,
        DB_DOES_NOT_EXIST
    }

    private enum class DozeExemptionState {
        NEEDS_DOZE_EXEMPTION_UNKNOWN,
        NEEDS_DOZE_EXEMPTION,
        DOES_NOT_NEED_DOZE_EXEMPTION
    }

    private var dbState = DB_UNKNOWN
    private var needsDozeExemption = NEEDS_DOZE_EXEMPTION_UNKNOWN
    private var onboardingDone = false

    private val lifecycleState: StateFlow<LifecycleState> = lifecycleManager.lifecycleStateFlow
    private val torPluginState: StateFlow<TorState> = torPlugin.state
    private val setupComplete: StateFlow<SetupComplete> = setupManager.setupComplete

    fun setDoesNotNeedDozeExemption() {
        needsDozeExemption = DOES_NOT_NEED_DOZE_EXEMPTION
        runBlocking { updateAppState() }
    }

    fun setNeedsDozeExemption() {
        needsDozeExemption = NEEDS_DOZE_EXEMPTION
        runBlocking { updateAppState() }
    }

    fun setOnboardingDone() {
        onboardingDone = true
        runBlocking { updateAppState() }
    }

    /**
     * Possible values for [appState]
     */
    sealed class MailboxAppState(val hasNotification: Boolean)
    object Undecided : MailboxAppState(false)
    object NeedOnboarding : MailboxAppState(false)
    object NeedsDozeExemption : MailboxAppState(false)
    object NotStarted : MailboxAppState(false)
    data class Starting(val status: String) : MailboxAppState(true)
    data class StartedSettingUp(val qrCode: Bitmap) : MailboxAppState(true)
    object StartedSetupComplete : MailboxAppState(true)
    object ErrorClockSkew : MailboxAppState(true)
    object ErrorNoNetwork : MailboxAppState(true)
    object Wiping : MailboxAppState(false)
    object Stopping : MailboxAppState(false)
    object Stopped : MailboxAppState(false)

    private val _appState: MutableStateFlow<MailboxAppState> = MutableStateFlow(Undecided)
    val appState: Flow<MailboxAppState> = _appState.onEach { state ->
        LOG.info { "state: ${state.javaClass.simpleName}" }
    }

    init {
        GlobalScope.launch(Main) { lifecycleState.collect { updateAppState() } }
        GlobalScope.launch(Main) { torPluginState.collect { updateAppState() } }
        GlobalScope.launch(Main) { setupComplete.collect { updateAppState() } }
        GlobalScope.launch(IO) {
            dbState = if (setupManager.hasDb) DB_EXISTS else DB_DOES_NOT_EXIST
            withContext(Main) {
                // TODO consider to make this dependent on DB state to not re-show with each launch
                needsDozeExemption = if (dozeHelper.needToShowDoNotKillMeFragment(context))
                    NEEDS_DOZE_EXEMPTION else DOES_NOT_NEED_DOZE_EXEMPTION
                updateAppState()
            }
        }
    }

    private suspend fun updateAppState() {
        val state = deriveAppState()
        _appState.value = state
        if (state.hasNotification) notificationManager.onMailboxAppStateChanged(state)
    }

    private fun deriveAppState(): MailboxAppState {
        val ls = lifecycleState.value
        val tor = torPluginState.value
        val setup = setupComplete.value
        LOG.info(
            "combining: $dbState, ls: $ls, $needsDozeExemption, onboarding done? $onboardingDone," +
                " tor: ${tor.javaClass.simpleName}, setup: $setup"
        )
        return when {
            dbState == DB_UNKNOWN -> Undecided
            ls == LifecycleState.NOT_STARTED && dbState == DB_DOES_NOT_EXIST &&
                !onboardingDone -> NeedOnboarding
            needsDozeExemption == NEEDS_DOZE_EXEMPTION -> NeedsDozeExemption
            ls == LifecycleState.NOT_STARTED -> NotStarted
            ls == LifecycleState.WIPING -> Wiping
            ls == LifecycleState.STOPPING -> Stopping
            ls == LifecycleState.STOPPED -> Stopped
            ls != LifecycleState.RUNNING -> Starting(getString(R.string.startup_starting_services))
            // RUNNING
            tor != TorState.Published -> when (tor) {
                TorState.StartingStopping -> Starting(getString(R.string.startup_starting_tor))
                is TorState.Enabling -> Starting(
                    getString(R.string.startup_bootstrapping_tor, tor.percent)
                )
                TorState.ClockSkewed -> ErrorClockSkew
                TorState.Inactive -> ErrorNoNetwork
                else -> Starting(getString(R.string.startup_publishing_onion_service))
            }
            setup == SetupComplete.FALSE -> {
                val dm = Resources.getSystem().displayMetrics
                val size = min(dm.widthPixels, dm.heightPixels)
                val bitMatrix = qrCodeEncoder.getQrCodeBitMatrix(size)
                StartedSettingUp(
                    bitMatrix?.let { it -> QrCodeUtils.renderQrCode(it) }
                        ?: error("The QR code bit matrix is expected to be non-null here")
                )
            }
            setup == SetupComplete.TRUE -> StartedSetupComplete
            // else means setup == SetupComplete.UNKNOWN
            else -> error("Expected setup completion to be known at this point")
        }
    }

    private fun getString(@StringRes resId: Int, vararg formatArgs: Any?): String {
        return context.getString(resId, *formatArgs)
    }
}
