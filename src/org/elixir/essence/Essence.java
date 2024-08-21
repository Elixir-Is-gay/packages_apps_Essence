/*
 * Copyright (C) 2014-2016 The Dirty Unicorns Project
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

package org.elixir.essence;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.ContentResolver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.viewpager.widget.ViewPager;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.SettingsPreferenceFragment;

import com.google.android.material.card.MaterialCardView;

import org.elixir.essence.categories.Lockscreen;
import org.elixir.essence.categories.StatusBar;
import org.elixir.essence.categories.Themes;
import org.elixir.essence.categories.Qs;
import org.elixir.essence.categories.About;
import org.elixir.essence.categories.Misc;
import org.elixir.essence.categories.Donate;
import org.elixir.essence.fragments.EssenceGestures;
import org.elixir.essence.utils.PreviewUtils;
import org.elixir.essence.utils.WorkspaceSurfaceHolderCallback;
import java.util.Random;

import com.android.settingslib.widget.LayoutPreference;

public class Essence extends SettingsPreferenceFragment implements   
       Preference.OnPreferenceChangeListener {

    private static final int MENU_HELP  = 0;
    private static final String KEY_ESSENCE_HOMEPAGE_HEADER = "essence_homepage_header";
    private static final String KEY_ESSENCE_DONATE_STATUS = "essence_donate_status";
	public static final String TAG = "Essence";
    private Preference mDonatePreference;
    private Boolean isExclusive = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();
        final Uri uri = Uri.parse("content://com.elixer.updater.IsExlusiveEnabled");
        Cursor cursor = resolver.query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            isExclusive = cursor.getInt(cursor.getColumnIndex("result")) == 1;
            cursor.close();
        } if (isExclusive) {
            addPreferencesFromResource(R.xml.essence_settings);
        } else {
            addPreferencesFromResource(R.xml.essence_settings_old);
        }
        if (isExclusive) {
            final LayoutPreference mEssenceHeader = getPreferenceScreen().findPreference(KEY_ESSENCE_HOMEPAGE_HEADER);
            LinearLayout gestureButton = (LinearLayout) mEssenceHeader.findViewById(R.id.essence_homepage_gesture_button);
            LinearLayout uiButton = (LinearLayout) mEssenceHeader.findViewById(R.id.essence_homepage_ui_button);
            LinearLayout miscButton = (LinearLayout) mEssenceHeader.findViewById(R.id.essence_homepage_three_dot_button);
            MaterialCardView wallpaperView = (MaterialCardView) mEssenceHeader.findViewById(R.id.essence_homepage_wallpaper);
            SurfaceView surfaceView = (SurfaceView) mEssenceHeader.findViewById(R.id.surfaceView);
            WorkspaceSurfaceHolderCallback mLockSurfaceCallback = new WorkspaceSurfaceHolderCallback(surfaceView, new PreviewUtils(getActivity(), null, "com.android.systemui.customization"));
            surfaceView.getHolder().addCallback(mLockSurfaceCallback);
            surfaceView.setZOrderOnTop(true);
            surfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
            gestureButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "onClick Received from mEssenceHeader!");
                    new SubSettingLauncher(getActivity())
                        .setDestination(EssenceGestures.class.getName())
                        .setSourceMetricsCategory(getMetricsCategory())
                        .setIsSecondLayerPage(true)
                        .launch();
                }
            });
            uiButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "onClick Received from mEssenceHeader!");
                    new SubSettingLauncher(getActivity())
                        .setDestination(Themes.class.getName())
                        .setSourceMetricsCategory(getMetricsCategory())
                        .setIsSecondLayerPage(true)
                        .launch();
                }
            });
            miscButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "onClick Received from mEssenceHeader!");
                    new SubSettingLauncher(getActivity())
                        .setDestination(Misc.class.getName())
                        .setSourceMetricsCategory(getMetricsCategory())
                        .setIsSecondLayerPage(true)
                        .launch();
                }
            });
            wallpaperView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "onClick Received from mEssenceHeader!");
                    final ComponentName name = new ComponentName("com.google.android.apps.wallpaper", "com.android.customization.picker.CustomizationPickerActivity");
                    final Intent intent = new Intent().setComponent(name).putExtra("com.android.wallpaper.LAUNCH_SOURCE", "app_launched_settings");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getActivity().startActivity(intent);
                }
            });
        }
        updateGreetings();
    }

    public void updateGreetings() {
        if (!isExclusive) {
            return;
        }
        mDonatePreference = (Preference) getPreferenceScreen().findPreference(KEY_ESSENCE_DONATE_STATUS);
        final String[] greetings = 
        {
            "Essence unleashed customize seamlessly",
            "Essence refined enjoy seamless customisation",
            "Essence unlocked now elevate your device",
            "Empower your Essence & personalize profoundly",
            "Essence enhanced now express your style",
            "Code Your Style",
            "Tech Tailored Tastes",
            "Customize, Innovate, Transform",
            "Personalized Pixel Power",
            "Your ROM, Your Rules"
        };
        if (mDonatePreference != null) {
            Random random = new Random();
            int index = random.nextInt(greetings.length);
            mDonatePreference.setTitle(getActivity().getResources().getString(R.string.essence_donate_status_donate_title));
            mDonatePreference.setSummary(getActivity().getResources().getString(R.string.essence_donate_status_donate_subtitle));
            mDonatePreference.setIcon(R.drawable.essence_pro_user);
            mDonatePreference.setSummary(greetings[index]);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CUSTOM_SETTINGS;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateGreetings();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        return true;
    }
}

