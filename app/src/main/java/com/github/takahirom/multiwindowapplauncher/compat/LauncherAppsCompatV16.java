/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.takahirom.multiwindowapplauncher.compat;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Version of {@link LauncherAppsCompat} for devices with API level 16.
 * Devices Pre-L don't support multiple profiles in one launcher so
 * user parameters are ignored and all methods operate on the current user.
 */
class LauncherAppsCompatV16 extends LauncherAppsCompat {

    private PackageManager pm;
    private Context context;
    private List<OnAppsChangedCallbackCompat> callbacks
            = new ArrayList<>();
    private PackageMonitor packageMonitor;

    LauncherAppsCompatV16(Context context) {
        pm = context.getPackageManager();
        this.context = context;
        packageMonitor = new PackageMonitor();
    }

    public List<LauncherActivityInfoCompat> getActivityList(String packageName,
                                                            UserHandleCompat user) {
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mainIntent.setPackage(packageName);
        List<ResolveInfo> infos = pm.queryIntentActivities(mainIntent, 0);
        List<LauncherActivityInfoCompat> list =
                new ArrayList<>(infos.size());
        for (ResolveInfo info : infos) {
            list.add(new LauncherActivityInfoCompatV16(context, info));
        }
        return list;
    }

    public LauncherActivityInfoCompat resolveActivity(Intent intent, UserHandleCompat user) {
        ResolveInfo info = pm.resolveActivity(intent, 0);
        if (info != null) {
            return new LauncherActivityInfoCompatV16(context, info);
        }
        return null;
    }

    public void startActivityForProfile(ComponentName component, UserHandleCompat user,
                                        Rect sourceBounds, Bundle opts) {
        Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setComponent(component);
        launchIntent.setSourceBounds(sourceBounds);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launchIntent, opts);
    }

    public void showAppDetailsForProfile(ComponentName component, UserHandleCompat user) {
        String packageName = component.getPackageName();
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(intent, null);
    }

    public synchronized void addOnAppsChangedCallback(OnAppsChangedCallbackCompat callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
            if (callbacks.size() == 1) {
                registerForPackageIntents();
            }
        }
    }

    public synchronized void removeOnAppsChangedCallback(OnAppsChangedCallbackCompat callback) {
        callbacks.remove(callback);
        if (callbacks.size() == 0) {
            unregisterForPackageIntents();
        }
    }

    public boolean isPackageEnabledForProfile(String packageName, UserHandleCompat user) {
        return isAppEnabled(pm, packageName, 0);
    }

    public boolean isActivityEnabledForProfile(ComponentName component, UserHandleCompat user) {
        try {
            ActivityInfo info = pm.getActivityInfo(component, 0);
            return info != null && info.isEnabled();
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    private void unregisterForPackageIntents() {
        context.unregisterReceiver(packageMonitor);
    }

    private void registerForPackageIntents() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        context.registerReceiver(packageMonitor, filter);
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        context.registerReceiver(packageMonitor, filter);
    }

    synchronized List<OnAppsChangedCallbackCompat> getCallbacks() {
        return new ArrayList<>(callbacks);
    }

    class PackageMonitor extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final UserHandleCompat user = UserHandleCompat.myUserHandle();

            if (Intent.ACTION_PACKAGE_CHANGED.equals(action)
                    || Intent.ACTION_PACKAGE_REMOVED.equals(action)
                    || Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                final String packageName = intent.getData().getSchemeSpecificPart();
                final boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

                if (packageName == null || packageName.length() == 0) {
                    // they sent us a bad intent
                    return;
                }
                if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
                    for (OnAppsChangedCallbackCompat callback : getCallbacks()) {
                        callback.onPackageChanged(packageName, user);
                    }
                } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                    if (!replacing) {
                        for (OnAppsChangedCallbackCompat callback : getCallbacks()) {
                            callback.onPackageRemoved(packageName, user);
                        }
                    }
                    // else, we are replacing the package, so a PACKAGE_ADDED will be sent
                    // later, we will update the package at this time
                } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                    if (!replacing) {
                        for (OnAppsChangedCallbackCompat callback : getCallbacks()) {
                            callback.onPackageAdded(packageName, user);
                        }
                    } else {
                        for (OnAppsChangedCallbackCompat callback : getCallbacks()) {
                            callback.onPackageChanged(packageName, user);
                        }
                    }
                }
            } else if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action)) {
                // EXTRA_REPLACING is available Kitkat onwards. For lower devices, it is broadcasted
                // when moving a package or mounting/un-mounting external storage. Assume that
                // it is a replacing operation.
                final boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING,
                        !Utilities.ATLEAST_KITKAT);
                String[] packages = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
                for (OnAppsChangedCallbackCompat callback : getCallbacks()) {
                    callback.onPackagesAvailable(packages, user, replacing);
                }
            } else if (Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE.equals(action)) {
                // This intent is broadcasted when moving a package or mounting/un-mounting
                // external storage.
                // However on Kitkat this is also sent when a package is being updated, and
                // contains an extra Intent.EXTRA_REPLACING=true for that case.
                // Using false as default for Intent.EXTRA_REPLACING gives correct value on
                // lower devices as the intent is not sent when the app is updating/replacing.
                final boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
                String[] packages = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
                for (OnAppsChangedCallbackCompat callback : getCallbacks()) {
                    callback.onPackagesUnavailable(packages, user, replacing);
                }
            }
        }
    }
}
