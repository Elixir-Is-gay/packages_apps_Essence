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

package org.elixir.essence.fragments;

import android.content.Context;
import android.content.ContentResolver;
import android.content.om.IOverlayManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.ServiceManager;
import android.provider.Settings;
import android.view.Display;
import android.view.SurfaceView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.SeekBar;
import static android.os.UserHandle.USER_SYSTEM;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.ListPreference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.util.custom.customUtils;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import static android.provider.Settings.Secure.LOCKSCREEN_CURRENT_CLOCK_OVERLAY;
import static android.provider.Settings.Secure.LOCKSCREEN_DEPTH_CLOCK_AUTO_STYLE;
import static android.provider.Settings.Secure.LSCLOCK_DEPTH_CLOCK_AUTO_MARGIN;
import static android.provider.Settings.Secure.LSCLOCK_DEPTH_CLOCK_AUTO_IMAGE_MARGIN;

import com.android.settingslib.display.DisplayDensityConfiguration;
import com.android.settingslib.widget.LayoutPreference;

import org.elixir.essence.preferences.CustomSeekBarPreference;
import org.elixir.essence.utils.PreviewUtils;
import org.elixir.essence.utils.WorkspaceSurfaceHolderCallback;

public class LSClockDepthFragment extends SettingsPreferenceFragment 
	implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "LSClockDepthFragment";
    private static final String KEY_SURFACE_VIEW_HEADER = "surface_view_header";
    private static final String KEY_CUSTOM_SEEKBAR = "lsclock_depth_clock_auto_margin";
    private static final String KEY_CUSTOM_IMAGE_MARGIN_SEEKBAR = "lsclock_depth_clock_auto_image_margin";
    private static final String KEY_DEPTH_CLOCK_STYLE = "android.customization.lsclock.depth.styles";
    private Context mContext;
    private PreferenceScreen screen;
    private ContentResolver resolver;
    private IOverlayManager mOverlayService;
    private ListPreference mAutoDepthClockStyle;
    private CustomSeekBarPreference mSeekBar;
    private CustomSeekBarPreference mOverlayMarginSeekBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lsclock_depth);

        mContext = getActivity();
        mOverlayService = IOverlayManager.Stub
                .asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));

        resolver = mContext.getContentResolver();
        screen = getPreferenceScreen();
        final Uri uri = Uri.parse("content://com.elixer.updater.IsExlusiveEnabled");
        Boolean isExclusive = false;
        Cursor cursor = resolver.query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            isExclusive = cursor.getInt(cursor.getColumnIndex("result")) == 1;
            cursor.close();
        }
        Log.w("Essence", "Status of exclusive :- " + isExclusive.toString());
        if (isExclusive) {
            mAutoDepthClockStyle = (ListPreference) findPreference(KEY_DEPTH_CLOCK_STYLE);
            if (mAutoDepthClockStyle != null) {
                mAutoDepthClockStyle.setValue(String.valueOf(Settings.Secure.getInt(resolver, LOCKSCREEN_DEPTH_CLOCK_AUTO_STYLE, 0)));
                mAutoDepthClockStyle.setSummary(mAutoDepthClockStyle.getEntry());
                mAutoDepthClockStyle.setOnPreferenceChangeListener(this);
            }
            mSeekBar = (CustomSeekBarPreference) findPreference(KEY_CUSTOM_SEEKBAR);
            if (mSeekBar != null) {
                mSeekBar.setValue(Settings.Secure.getInt(resolver, LSCLOCK_DEPTH_CLOCK_AUTO_MARGIN, 550));
                mSeekBar.setOnPreferenceChangeListener(this);
            }

            mOverlayMarginSeekBar = (CustomSeekBarPreference) findPreference(KEY_CUSTOM_IMAGE_MARGIN_SEEKBAR);
            if (mOverlayMarginSeekBar != null) {
                mOverlayMarginSeekBar.setValue(Settings.Secure.getInt(resolver, LSCLOCK_DEPTH_CLOCK_AUTO_IMAGE_MARGIN, 0));
                mOverlayMarginSeekBar.setOnPreferenceChangeListener(this);
            }
            setupSurfaceView();
        }
    }

    private void setupSurfaceView() {
        final LayoutPreference mSurfaceHeader = screen.findPreference(KEY_SURFACE_VIEW_HEADER);
        SurfaceView surfaceView = (SurfaceView) mSurfaceHeader.findViewById(R.id.surfaceView);
        WorkspaceSurfaceHolderCallback mLockSurfaceCallback = new WorkspaceSurfaceHolderCallback(surfaceView, new PreviewUtils(mContext, null, "com.android.systemui.customization"));
        surfaceView.getHolder().addCallback(mLockSurfaceCallback);
        surfaceView.setZOrderOnTop(true);
        surfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CUSTOM_SETTINGS;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mAutoDepthClockStyle) {
            int value = Integer.parseInt((String) newValue);
            mAutoDepthClockStyle.setValue(String.valueOf(value));
            mAutoDepthClockStyle.setSummary(mAutoDepthClockStyle.getEntry());
            Settings.Secure.putInt(resolver, LOCKSCREEN_DEPTH_CLOCK_AUTO_STYLE, value);
            reloadClock();
            return true;
        } else if (preference == mSeekBar) {
            Settings.Secure.putInt(resolver, LSCLOCK_DEPTH_CLOCK_AUTO_MARGIN, (int) newValue);
            mSeekBar.setValue((int) newValue);
            reloadClock();
            return true;
        } else if (preference == mOverlayMarginSeekBar) {
            Settings.Secure.putInt(resolver, LSCLOCK_DEPTH_CLOCK_AUTO_IMAGE_MARGIN, (int) newValue);
            mOverlayMarginSeekBar.setValue((int) newValue);
            reloadClock();
            return true;
        }
        return false;
    }

    private void RROManager(String name, boolean status) {
        Log.d(TAG,  status ? "Enabling" : "Disabling" + " Overlay Package :- " + name);
        try {
            mOverlayService.setEnabled(name, status, USER_SYSTEM);
        } catch (Exception re) {
            Log.e(TAG, String.valueOf(re));
        }
    }

    private String getCurrentClock() {
        return Settings.Secure.getString(resolver, LOCKSCREEN_CURRENT_CLOCK_OVERLAY);
    }

    private void applyScreenDpi(int newDpi) {
        try {
            final Resources res = mContext.getResources();
            final DisplayMetrics metrics = res.getDisplayMetrics();
            final int newSwDp = newDpi;
            final int minDimensionPx = Math.min(metrics.widthPixels, metrics.heightPixels);
            final int newDensity = DisplayMetrics.DENSITY_MEDIUM * minDimensionPx / newSwDp;
            final int densityDpi = Math.max(newDensity, 120);
            DisplayDensityConfiguration.setForcedDisplayDensity(Display.DEFAULT_DISPLAY, densityDpi);
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
    }

    private void reloadClock() {
        customUtils.refreshCustomClock(resolver);
    }
}
