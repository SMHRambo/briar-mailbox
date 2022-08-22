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

package org.briarproject.mailbox.android.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.briarproject.android.dontkillmelib.DozeUtils.needsDozeWhitelisting
import org.briarproject.mailbox.NavMainDirections.actionGlobalClockSkewFragment
import org.briarproject.mailbox.NavMainDirections.actionGlobalDoNotKillMeFragment
import org.briarproject.mailbox.NavMainDirections.actionGlobalNoNetworkFragment
import org.briarproject.mailbox.NavMainDirections.actionGlobalOnboardingContainer
import org.briarproject.mailbox.NavMainDirections.actionGlobalQrCodeFragment
import org.briarproject.mailbox.NavMainDirections.actionGlobalSetupCompleteFragment
import org.briarproject.mailbox.NavMainDirections.actionGlobalStartupFragment
import org.briarproject.mailbox.NavMainDirections.actionGlobalStatusFragment
import org.briarproject.mailbox.NavMainDirections.actionGlobalStoppingFragment
import org.briarproject.mailbox.NavMainDirections.actionGlobalWipingFragment
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.StatusManager.ErrorClockSkew
import org.briarproject.mailbox.android.StatusManager.ErrorNoNetwork
import org.briarproject.mailbox.android.StatusManager.MailboxAppState
import org.briarproject.mailbox.android.StatusManager.NotStarted
import org.briarproject.mailbox.android.StatusManager.StartedSettingUp
import org.briarproject.mailbox.android.StatusManager.StartedSetupComplete
import org.briarproject.mailbox.android.StatusManager.Starting
import org.briarproject.mailbox.android.StatusManager.Stopped
import org.briarproject.mailbox.android.StatusManager.Stopping
import org.briarproject.mailbox.android.StatusManager.Wiping
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.NOT_STARTED
import org.briarproject.mailbox.core.util.LogUtils.info
import org.slf4j.LoggerFactory.getLogger

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        private val LOG = getLogger(MainActivity::class.java)

        const val BUNDLE_LIFECYCLE_BEYOND_NOT_STARTED = "LIFECYCLE_BEYOND_NOT_STARTED"
    }

    private val viewModel: MailboxViewModel by viewModels()
    private val nav: NavController by lazy {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment
        navHostFragment.navController
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LOG.info("onCreate()")
        setContentView(R.layout.activity_main)

        viewModel.onboardingComplete.observe(this) { complete ->
            if (complete && nav.currentDestination?.id == R.id.onboardingFragment) {
                if (viewModel.needToShowDoNotKillMeFragment) {
                    nav.navigate(actionGlobalDoNotKillMeFragment())
                } else {
                    nav.navigate(actionGlobalStartupFragment())
                }
            }
        }

        viewModel.doNotKillComplete.observe(this) { complete ->
            if (complete && nav.currentDestination?.id == R.id.doNotKillMeFragment) nav.navigate(
                actionGlobalStartupFragment()
            )
        }

        launchAndRepeatWhileStarted {
            viewModel.appState.collect { onAppStateChanged(it) }
        }

        LOG.info { "do we have a saved instance state? " + (savedInstanceState != null) }

        lifecycleScope.launch {
            val hasDb = viewModel.hasDb()
            LOG.info { "do we have a db? $hasDb" }
            onDbChecked(hasDb, savedInstanceState)
        }
    }

    private fun onAppStateChanged(state: MailboxAppState) {
        when (state) {
            NotStarted -> {} // do not navigate anywhere yet
            is Starting -> if (nav.currentDestination?.id != R.id.startupFragment)
                nav.navigate(actionGlobalStartupFragment())
            is StartedSettingUp -> if (nav.currentDestination?.id != R.id.qrCodeFragment)
                nav.navigate(actionGlobalQrCodeFragment())
            StartedSetupComplete -> if (nav.currentDestination?.id == R.id.qrCodeFragment)
                nav.navigate(actionGlobalSetupCompleteFragment())
            else if (nav.currentDestination?.id != R.id.statusFragment &&
                nav.currentDestination?.id != R.id.setupCompleteFragment
            )
                nav.navigate(actionGlobalStatusFragment())
            ErrorNoNetwork -> if (nav.currentDestination?.id != R.id.noNetworkFragment)
                nav.navigate(actionGlobalNoNetworkFragment())
            ErrorClockSkew -> if (nav.currentDestination?.id != R.id.clockSkewFragment)
                nav.navigate(actionGlobalClockSkewFragment())
            Stopping -> if (nav.currentDestination?.id != R.id.stoppingFragment)
                nav.navigate(actionGlobalStoppingFragment())
            Wiping -> if (nav.currentDestination?.id != R.id.wipingFragment)
                nav.navigate(actionGlobalWipingFragment())
            Stopped -> {} // nothing to do but needs to be exhaustive for Kotlin 1.7
        }
    }

    private fun onDbChecked(hasDb: Boolean, savedInstanceState: Bundle?) {
        if (lifecycle.currentState == DESTROYED) {
            return
        }
        if (savedInstanceState == null) {
            if (!hasDb) {
                nav.navigate(actionGlobalOnboardingContainer())
            } else if (needsDozeWhitelisting(this)) {
                nav.navigate(actionGlobalDoNotKillMeFragment())
            } else {
                nav.navigate(actionGlobalStartupFragment())
            }
        } else {
            // At this point, when we do not have a db, this can be either of two situations:
            // 1. We just came back from the onboarding activity and our MainActivity has been
            // destroyed in the meantime (can be forced using the do-not-keep-activities developer
            // option). In this case onSaveInstanceState() has written false to the bundle.
            // 2. We come back to the activity after a remote wipe has happened while the app was
            // in the background and gets restored from the recent app list after wiping and
            // stopping has already completed. In this case onSaveInstanceState() has written
            // true to the bundle.
            val savedBeyondNotStarted =
                savedInstanceState.getBoolean(BUNDLE_LIFECYCLE_BEYOND_NOT_STARTED)
            if (!hasDb && savedBeyondNotStarted) {
                finish()
                startActivity(Intent(this, WipeCompleteActivity::class.java))
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(
            BUNDLE_LIFECYCLE_BEYOND_NOT_STARTED,
            viewModel.lifecycleState.value != NOT_STARTED
        )
    }

    override fun onResume() {
        super.onResume()
        if (needsDozeWhitelisting(this) && viewModel.getAndResetDozeFlag()) {
            showDozeDialog()
        }
    }

    private fun showDozeDialog() = AlertDialog.Builder(this)
        .setMessage(R.string.warning_dozed)
        .setPositiveButton(R.string.fix) { dialog, _ ->
            nav.navigate(actionGlobalDoNotKillMeFragment())
            dialog.dismiss()
        }
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()

}
