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
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.MailboxService.Companion.EXTRA_START_RESULT
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.LIFECYCLE_REUSE
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.SERVICE_ERROR

@AndroidEntryPoint
class StartupFailureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_startup_failure)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent) {
        // show proper error message
        val errorRes = when (i.getSerializableExtra(EXTRA_START_RESULT) as StartResult) {
            SERVICE_ERROR -> R.string.startup_failed_service_error
            LIFECYCLE_REUSE -> R.string.startup_failed_lifecycle_reuse
            else -> throw IllegalArgumentException()
        }
        val f = ErrorFragment.newInstance(getString(errorRes))
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, f, ErrorFragment::class.java.name).commit()
    }

}
