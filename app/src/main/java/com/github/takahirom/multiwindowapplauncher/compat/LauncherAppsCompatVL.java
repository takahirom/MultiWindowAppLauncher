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

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class LauncherAppsCompatVL extends LauncherAppsCompat {

    private LauncherApps launcherApps;

    private final Map<OnAppsChangedCallbackCompat, WrappedCallback> callbacks
            = new HashMap<>();

    LauncherAppsCompatVL(Context context) {
        super();
        launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
    }

    public List<LauncherActivityInfoCompat> getActivityList(String packageName,
                                                            UserHandleCompat user) {
        List<LauncherActivityInfo> list = launcherApps.getActivityList(packageName,
                user.getUser());
        if (list.size() == 0) {
            return Collections.emptyList();
        }
        ArrayList<LauncherActivityInfoCompat> compatList =
                new ArrayList<>(list.size());
        for (LauncherActivityInfo info : list) {
            compatList.add(new LauncherActivityInfoCompatVL(info));
        }
        return compatList;
    }

    public LauncherActivityInfoCompat resolveActivity(Intent intent, UserHandleCompat user) {
        LauncherActivityInfo activity = launcherApps.resolveActivity(intent, user.getUser());
        if (activity != null) {
            return new LauncherActivityInfoCompatVL(activity);
        } else {
            return null;
        }
    }

    public void startActivityForProfile(ComponentName component, UserHandleCompat user,
                                        Rect sourceBounds, Bundle opts) {
        launcherApps.startMainActivity(component, user.getUser(), sourceBounds, opts);
    }

    public void showAppDetailsForProfile(ComponentName component, UserHandleCompat user) {
        launcherApps.startAppDetailsActivity(component, user.getUser(), null, null);
    }

    public void addOnAppsChangedCallback(LauncherAppsCompat.OnAppsChangedCallbackCompat callback) {
        WrappedCallback wrappedCallback = new WrappedCallback(callback);
        synchronized (callbacks) {
            callbacks.put(callback, wrappedCallback);
        }
        launcherApps.registerCallback(wrappedCallback);
    }

    public void removeOnAppsChangedCallback(
            LauncherAppsCompat.OnAppsChangedCallbackCompat callback) {
        WrappedCallback wrappedCallback;
        synchronized (callbacks) {
            wrappedCallback = callbacks.remove(callback);
        }
        if (wrappedCallback != null) {
            launcherApps.unregisterCallback(wrappedCallback);
        }
    }

    public boolean isPackageEnabledForProfile(String packageName, UserHandleCompat user) {
        return launcherApps.isPackageEnabled(packageName, user.getUser());
    }

    public boolean isActivityEnabledForProfile(ComponentName component, UserHandleCompat user) {
        return launcherApps.isActivityEnabled(component, user.getUser());
    }

    private static class WrappedCallback extends LauncherApps.Callback {
        private LauncherAppsCompat.OnAppsChangedCallbackCompat callback;

        WrappedCallback(LauncherAppsCompat.OnAppsChangedCallbackCompat callback) {
            this.callback = callback;
        }

        public void onPackageRemoved(String packageName, UserHandle user) {
            callback.onPackageRemoved(packageName, UserHandleCompat.fromUser(user));
        }

        public void onPackageAdded(String packageName, UserHandle user) {
            callback.onPackageAdded(packageName, UserHandleCompat.fromUser(user));
        }

        public void onPackageChanged(String packageName, UserHandle user) {
            callback.onPackageChanged(packageName, UserHandleCompat.fromUser(user));
        }

        public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
            callback.onPackagesAvailable(packageNames, UserHandleCompat.fromUser(user), replacing);
        }

        public void onPackagesUnavailable(String[] packageNames, UserHandle user,
                                          boolean replacing) {
            callback.onPackagesUnavailable(packageNames, UserHandleCompat.fromUser(user),
                    replacing);
        }
    }
}

