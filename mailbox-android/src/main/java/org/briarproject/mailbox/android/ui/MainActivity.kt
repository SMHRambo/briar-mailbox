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
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import org.briarproject.android.dontkillmelib.PowerUtils.needsDozeWhitelisting
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.dontkillme.DoNotKillMeFragmentDirections.actionDoNotKillMeFragmentToStartupFragment
import org.briarproject.mailbox.android.ui.InitFragmentDirections.actionInitFragmentToDoNotKillMeFragment
import org.briarproject.mailbox.android.ui.InitFragmentDirections.actionInitFragmentToStartupFragment
import org.briarproject.mailbox.android.ui.StatusFragmentDirections.actionStatusFragmentToWipeCompleteFragment

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ActivityResultCallback<ActivityResult> {

    private val viewModel: MailboxViewModel by viewModels()
    private val nav: NavController by lazy {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment
        navHostFragment.navController
    }

    private val startForResult = registerForActivityResult(StartActivityForResult(), this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel.doNotKillComplete.observe(this) { complete ->
            if (complete) nav.navigate(actionDoNotKillMeFragmentToStartupFragment())
        }

        viewModel.wipeComplete.observe(this) { complete ->
            if (complete) nav.navigate(actionStatusFragmentToWipeCompleteFragment())
        }

        if (savedInstanceState == null) {
            viewModel.hasDb.observe(this) { hasDb ->
                if (!hasDb) {
                    startForResult.launch(Intent(this, OnboardingActivity::class.java))
                } else {
                    nav.navigate(actionInitFragmentToStartupFragment())
                }
            }
        }
    }

    override fun onActivityResult(result: ActivityResult?) {
        if (viewModel.needToShowDoNotKillMeFragment) {
            nav.navigate(actionInitFragmentToDoNotKillMeFragment())
        } else {
            nav.navigate(actionInitFragmentToStartupFragment())
        }
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
            nav.navigate(actionInitFragmentToDoNotKillMeFragment())
            dialog.dismiss()
        }
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()

}
