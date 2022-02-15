package org.briarproject.mailbox.android.dontkillme;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.AttributeSet;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;

import static android.content.pm.PackageManager.MATCH_DEFAULT_ONLY;
import static android.os.Build.VERSION.SDK_INT;

@UiThread
public class HuaweiProtectedAppsView extends PowerView {

	private final static String PACKAGE_NAME = "com.huawei.systemmanager";
	private final static String CLASS_NAME =
			PACKAGE_NAME + ".optimize.process.ProtectActivity";

	public HuaweiProtectedAppsView(Context context) {
		this(context, null);
	}

	public HuaweiProtectedAppsView(Context context,
			@Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public HuaweiProtectedAppsView(Context context,
			@Nullable AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setText(org.briarproject.android.dontkillmelib.R.string.setup_huawei_text);
		setButtonText(
				org.briarproject.android.dontkillmelib.R.string.setup_huawei_button);
	}

	@Override
	public boolean needsToBeShown() {
		return needsToBeShown(getContext());
	}

	public static boolean needsToBeShown(Context context) {
		// "Protected apps" no longer exists on Huawei EMUI 5.0 (Android 7.0)
		if (SDK_INT >= 24) return false;
		PackageManager pm = context.getPackageManager();
		List<ResolveInfo> resolveInfos = pm.queryIntentActivities(getIntent(),
				MATCH_DEFAULT_ONLY);
		return !resolveInfos.isEmpty();
	}

	@Override
	@StringRes
	protected int getHelpText() {
		return org.briarproject.android.dontkillmelib.R.string.setup_huawei_help;
	}

	@Override
	protected void onButtonClick() {
		getContext().startActivity(getIntent());
		setChecked(true);
	}

	private static Intent getIntent() {
		Intent intent = new Intent();
		intent.setClassName(PACKAGE_NAME, CLASS_NAME);
		return intent;
	}

}
