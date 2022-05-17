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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.StatusManager.MailboxStartupProgress
import org.briarproject.mailbox.android.StatusManager.StartedSettingUp
import org.briarproject.mailbox.android.StatusManager.StartedSetupComplete
import org.briarproject.mailbox.android.ui.QrCodeFragmentDirections.actionQrCodeFragmentToSetupCompleteFragment

@AndroidEntryPoint
class QrCodeFragment : Fragment() {

    private val viewModel: MailboxViewModel by activityViewModels()
    private lateinit var qrCodeView: ImageView
    private lateinit var buttonCancel: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_qr, container, false)
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        qrCodeView = v.findViewById(R.id.qrcode)
        buttonCancel = v.findViewById(R.id.buttonCancel)

        buttonCancel.setOnClickListener {
            viewModel.stopLifecycle()
            requireActivity().finishAffinity()
        }

        launchAndRepeatWhileStarted {
            viewModel.setupState.collect { onSetupStateChanged(it) }
        }
    }

    private fun onSetupStateChanged(setupComplete: MailboxStartupProgress) {
        when (setupComplete) {
            is StartedSettingUp -> qrCodeView.setImageBitmap(setupComplete.qrCode)
            is StartedSetupComplete -> findNavController().navigate(
                actionQrCodeFragmentToSetupCompleteFragment()
            )
            else -> error("Unexpected setup state: $setupComplete")
        }
    }

}
