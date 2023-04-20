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

package org.briarproject.mailbox.android.ui.startup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.StatusManager.MailboxAppState
import org.briarproject.mailbox.android.StatusManager.Starting
import org.briarproject.mailbox.android.ui.MailboxViewModel
import org.briarproject.mailbox.android.ui.launchAndRepeatWhileStarted

@AndroidEntryPoint
class StartupFragment : Fragment() {

    private val viewModel: MailboxViewModel by activityViewModels()
    private lateinit var statusDetail: TextView
    private lateinit var cancelButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_startup, container, false)
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        statusDetail = v.findViewById(R.id.statusDetail)
        cancelButton = v.findViewById(R.id.button)

        launchAndRepeatWhileStarted {
            viewModel.appState.collect { onAppStateChanged(it) }
        }
        // FIXME Should this be the job of the fragment? What if I rotate the screen? Start twice?
        viewModel.startLifecycle()
    }

    private fun onAppStateChanged(state: MailboxAppState) {
        if (state is Starting) {
            statusDetail.text = state.status
            if (state.isCancelable) {
                cancelButton.visibility = VISIBLE
                cancelButton.setOnClickListener {
                    viewModel.stopLifecycle()
                    requireActivity().finishAffinity()
                }
            } else {
                cancelButton.visibility = GONE
            }
        }
    }

}
