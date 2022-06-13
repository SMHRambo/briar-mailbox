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

package org.briarproject.mailbox.android.dontkillme

import android.content.Context
import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.briarproject.mailbox.R

object DoNotKillMeUtils {
    fun showOnboardingDialog(ctx: Context?, text: String?) {
        MaterialAlertDialogBuilder(ctx!!)
            .setMessage(text)
            .setNeutralButton(R.string.dnkm_got_it) { dialog: DialogInterface, _: Int ->
                dialog.cancel()
            }
            .show()
    }
}
