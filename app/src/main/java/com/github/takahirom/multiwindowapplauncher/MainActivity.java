package com.github.takahirom.multiwindowapplauncher;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.github.takahirom.multiwindowapplauncher.compat.LauncherActivityInfoCompat;
import com.github.takahirom.multiwindowapplauncher.compat.LauncherAppsCompat;
import com.github.takahirom.multiwindowapplauncher.compat.UserHandleCompat;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rx.AsyncEmitter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog progressDialog;
    private PackageManager packageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final FloatingActionButton button = (FloatingActionButton) findViewById(R.id.button_select_app);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLoading();
            }
        });
    }

    static class App {
        String packageName;
        String name;
        Bitmap icon;

        public App(Context context, String packageName, String title) {
            this.packageName = packageName;
        }

        public String getPackageName() {
            if (packageName == null) {

            }
            return packageName;
        }
    }

    private void startLoading() {
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.show();
        this.packageManager = getPackageManager();

        Observable
                .fromAsync(new Action1<AsyncEmitter<List<LauncherActivityInfoCompat>>>() {
                    @Override
                    public void call(final AsyncEmitter<List<LauncherActivityInfoCompat>> emitter) {
                        final List<LauncherActivityInfoCompat> activityList = LauncherAppsCompat.getInstance(MainActivity.this).getActivityList(null, UserHandleCompat.myUserHandle());
                        emitter.onNext(activityList);
                    }
                }, AsyncEmitter.BackpressureMode.BUFFER)
                .map(new Func1<List<LauncherActivityInfoCompat>, List<LauncherActivityInfoCompat>>() {
                    @Override
                    public List<LauncherActivityInfoCompat> call(List<LauncherActivityInfoCompat> launcherActivityInfoCompats) {
                        Collections.sort(launcherActivityInfoCompats, new Comparator<LauncherActivityInfoCompat>() {
                            @Override
                            public int compare(LauncherActivityInfoCompat launcherActivityInfoCompat, LauncherActivityInfoCompat t1) {
                                return launcherActivityInfoCompat.getLabel().toString().compareTo(t1.getLabel().toString());
                            }
                        });
                        return launcherActivityInfoCompats;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<LauncherActivityInfoCompat>>() {
                    @Override
                    public void call(final List<LauncherActivityInfoCompat> applicationInfos) {


                        onLoadFinish(applicationInfos);
                    }
                });

    }

    private void onLoadFinish(final List<LauncherActivityInfoCompat> applicationInfos) {
        progressDialog.hide();
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final View view = super.getView(position, convertView, parent);
                final TextView textView = view instanceof TextView ? ((TextView) view) : null;
                if (textView != null) {
                    final Drawable drawable = applicationInfos.get(position).getIcon(0);
                    final int size = getResources().getDimensionPixelSize(android.R.dimen.app_icon_size);
                    drawable.setBounds(0, 0, size, size);
                    textView.setCompoundDrawables(drawable, null, null, null);
                }
                return view;
            }
        };
        for (LauncherActivityInfoCompat applicationInfo : applicationInfos) {
            adapter.add(applicationInfo.getLabel().toString());
        }
        new AlertDialog
                .Builder(MainActivity.this)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final SwitchCompat switchButton = (SwitchCompat) MainActivity.this.findViewById(R.id.switch_create_shortcut);
                        MultiWindowAppLaunchActivity.start(MainActivity.this, applicationInfos.get(i), switchButton.isChecked());
                    }
                }).show();
    }
}
