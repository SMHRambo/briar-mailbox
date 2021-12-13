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

package org.briarproject.mailbox.core.system;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.PowerManager;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;

import static android.content.Context.POWER_SERVICE;
import static android.os.PowerManager.PARTIAL_WAKE_LOCK;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

class AndroidWakeLockManagerImpl implements AndroidWakeLockManager {

	/**
	 * How often to replace the wake lock.
	 */
	private static final long LOCK_DURATION_MS = MINUTES.toMillis(1);

	/**
	 * Automatically release the lock this many milliseconds after it's due
	 * to have been replaced and released.
	 */
	private static final long SAFETY_MARGIN_MS = SECONDS.toMillis(30);

	private final SharedWakeLock sharedWakeLock;

	@Inject
	AndroidWakeLockManagerImpl(Application app,
			ScheduledExecutorService scheduledExecutorService) {
		PowerManager powerManager = (PowerManager)
				requireNonNull(app.getSystemService(POWER_SERVICE));
		String tag = getWakeLockTag(app);
		sharedWakeLock = new RenewableWakeLock(powerManager,
				scheduledExecutorService, PARTIAL_WAKE_LOCK, tag,
				LOCK_DURATION_MS, SAFETY_MARGIN_MS);
	}

	@Override
	public AndroidWakeLock createWakeLock(String tag) {
		return new AndroidWakeLockImpl(sharedWakeLock, tag);
	}

	@Override
	public void runWakefully(Runnable r, String tag) {
		AndroidWakeLock wakeLock = createWakeLock(tag);
		wakeLock.acquire();
		try {
			r.run();
		} finally {
			wakeLock.release();
		}
	}

	@Override
	public void executeWakefully(Runnable r, Executor executor, String tag) {
		AndroidWakeLock wakeLock = createWakeLock(tag);
		wakeLock.acquire();
		try {
			executor.execute(() -> {
				try {
					r.run();
				} finally {
					// Release the wake lock if the task throws an exception
					wakeLock.release();
				}
			});
		} catch (Exception e) {
			// Release the wake lock if the executor throws an exception when
			// we submit the task (in which case the release() call above won't
			// happen)
			wakeLock.release();
			throw e;
		}
	}

	@Override
	public void executeWakefully(Runnable r, String tag) {
		AndroidWakeLock wakeLock = createWakeLock(tag);
		wakeLock.acquire();
		try {
			new Thread(() -> {
				try {
					r.run();
				} finally {
					wakeLock.release();
				}
			}).start();
		} catch (Exception e) {
			wakeLock.release();
			throw e;
		}
	}

	private String getWakeLockTag(Context ctx) {
		PackageManager pm = ctx.getPackageManager();
		if (isInstalled(pm, "com.huawei.powergenie")) {
			return "LocationManagerService";
		} else if (isInstalled(pm, "com.evenwell.PowerMonitor")) {
			return "AudioIn";
		}
		return ctx.getPackageName();
	}

	private boolean isInstalled(PackageManager pm, String packageName) {
		try {
			pm.getPackageInfo(packageName, 0);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}

}
