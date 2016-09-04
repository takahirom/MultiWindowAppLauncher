package com.github.takahirom.multiwindowapplauncher;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.github.takahirom.multiwindowapplauncher.compat.LauncherActivityInfoCompat;

public class MultiWindowAppLaunchActivity extends AppCompatActivity {

    public static final String EXTRA_APPLICATION_PACKAGE_NAME = "extra_application_package_name";
    public static final String EXTRA_APPLICATION_ACTIVITY = "extra_application_activity";
    public static final String EXTRA_CREATE_SHORTCUT = "extra_create_shortcut";
    private ComponentName componentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_window_app_launch);
        componentName = new ComponentName(getIntent().getStringExtra(EXTRA_APPLICATION_PACKAGE_NAME), getIntent().getStringExtra(EXTRA_APPLICATION_ACTIVITY));

        final boolean isCreateShortcut = getIntent().getBooleanExtra(EXTRA_CREATE_SHORTCUT, false);
        if (isCreateShortcut) {
            createShortCut();
        }
        if (isInMultiWindowMode()) {
            startApp(componentName);
        }
    }

    private void createShortCut() {
        final Intent launchIntent = getLaunchIntent(this, false, componentName);

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
        final Drawable icon;
        final ApplicationInfo applicationInfo;
        try {
            applicationInfo = getPackageManager().getApplicationInfo(componentName.getPackageName(), 0);
            icon = getPackageManager().getApplicationIcon(componentName.getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, "Fail create shortcut", Toast.LENGTH_SHORT).show();
            return;
        }
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "MW " + getPackageManager().getApplicationLabel(applicationInfo));
        Bitmap bitmap;
        if (icon instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) icon).getBitmap();
        } else {
            bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }
        bitmap = Bitmap.createScaledBitmap(bitmap, 192, 192, false);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);

        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
//        addIntent.putExtra("duplicate", false);  //may it's already there so don't duplicate
        getApplicationContext().sendBroadcast(addIntent);
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        if (isInMultiWindowMode) {
            startApp(componentName);
        }
    }

    private void startApp(ComponentName componentName) {
        ActivityManager am = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
        am.killBackgroundProcesses(componentName.getPackageName());
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setComponent(componentName);
        startActivityForResult(intent, 0);
        finish();
    }

    public static void start(Activity activity, LauncherActivityInfoCompat applicationInfo, boolean isCreateShortCut) {
        final ComponentName componentName = applicationInfo.getComponentName();
        final Intent intent = getLaunchIntent(activity, isCreateShortCut, componentName);
        activity.startActivity(intent);
    }

    @NonNull
    private static Intent getLaunchIntent(Activity activity, boolean isCreateShortCut, ComponentName componentName) {
        final Intent intent = new Intent(activity, MultiWindowAppLaunchActivity.class);
        intent.putExtra(EXTRA_APPLICATION_PACKAGE_NAME, componentName.getPackageName());
        intent.putExtra(EXTRA_APPLICATION_ACTIVITY, componentName.getClassName());
        intent.putExtra(EXTRA_CREATE_SHORTCUT, isCreateShortCut);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
}
