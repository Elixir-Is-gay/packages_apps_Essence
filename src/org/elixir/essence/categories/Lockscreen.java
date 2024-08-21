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

package org.elixir.essence.categories;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.custom.customUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import static android.provider.Settings.System.UDFPS_ANIM_RANDOMIZE;

public class Lockscreen extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    
    private static final String TAG = "Lockscreen";
    private FingerprintManager mFingerprintManager;

    private static final String KEY_UDFPS_ANIMATIONS = "udfps_cust_category";
    private static final String KEY_RANDOM_UDFPS_ANIMATIONS = "udfps_anim_randomize";
    private static final String KEY_UDFPS_ANIMATION_FRAG = "udfps_recognizing_animation_preview";

    private PreferenceCategory mUdfpsAnimations;
    private static final String FP_SUCCESS_VIBRATE = "fp_success_vibrate";
    private static final String FP_ERROR_VIBRATE = "fp_error_vibrate";

    private SwitchPreference mFingerprintSuccessVib;
    private SwitchPreference mFingerprintErrorVib;
    private SwitchPreference mRandomudfps;
    private Preference mUdfpsAnimationFragment;
    private PreferenceScreen prefScreen;
    private Boolean isExclusive = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lockscreen);

        ContentResolver resolver = getActivity().getContentResolver();

        prefScreen = getPreferenceScreen();
        final PackageManager mPm = getActivity().getPackageManager();
        
	    final PreferenceCategory perfCatRipple = (PreferenceCategory) prefScreen
                .findPreference("ripple_effect_category");
        
        mFingerprintManager = (FingerprintManager)
                getActivity().getSystemService(getActivity().FINGERPRINT_SERVICE);
        Resources resources = getResources();
        final Uri uri = Uri.parse("content://com.elixer.updater.IsExlusiveEnabled");
        Cursor cursor = resolver.query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            isExclusive = cursor.getInt(cursor.getColumnIndex("result")) == 1;
            cursor.close();
        }
        if (mFingerprintManager == null || !mFingerprintManager.isHardwareDetected()) {
            prefScreen.removePreference(perfCatRipple);
        }

        mUdfpsAnimations = (PreferenceCategory) findPreference(KEY_UDFPS_ANIMATIONS);
        mRandomudfps = (SwitchPreference) findPreference(KEY_RANDOM_UDFPS_ANIMATIONS);
        mUdfpsAnimationFragment = (Preference) findPreference(KEY_UDFPS_ANIMATION_FRAG);
        if (!isExclusive) {
            mUdfpsAnimations.removePreference(mRandomudfps);
            mUdfpsAnimationFragment.setLayoutResource(org.elixir.resources.cardlayout.R.layout.xd_pref_card_mid2);
        }
        if (!customUtils.isPackageInstalled(getContext(), "com.custom.udfps.resources")) {
                getPreferenceScreen().removePreference(mUdfpsAnimations);
        } else if (isExclusive) {
            mRandomudfps.setChecked(Settings.System.getIntForUser(resolver, UDFPS_ANIM_RANDOMIZE, 0, UserHandle.USER_CURRENT) != 0);
            mUdfpsAnimationFragment.setEnabled(!mRandomudfps.isChecked());
            mRandomudfps.setOnPreferenceChangeListener(this);
        }
        
	mFingerprintManager = (FingerprintManager) getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
        mFingerprintSuccessVib = (SwitchPreference) findPreference(FP_SUCCESS_VIBRATE);
        mFingerprintErrorVib = (SwitchPreference) findPreference(FP_ERROR_VIBRATE);
        if (mPm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) &&
                 mFingerprintManager != null) {
            if (!mFingerprintManager.isHardwareDetected()){
                prefScreen.removePreference(mFingerprintSuccessVib);
                prefScreen.removePreference(mFingerprintErrorVib);
            } else {
                mFingerprintSuccessVib.setChecked((Settings.System.getInt(getContentResolver(),
                        Settings.System.FP_SUCCESS_VIBRATE, 1) == 1));
                mFingerprintSuccessVib.setOnPreferenceChangeListener(this);
                mFingerprintErrorVib.setChecked((Settings.System.getInt(getContentResolver(),
                        Settings.System.FP_ERROR_VIBRATE, 1) == 1));
                mFingerprintErrorVib.setOnPreferenceChangeListener(this);
            }
        } else {
            prefScreen.removePreference(mFingerprintSuccessVib);
            prefScreen.removePreference(mFingerprintErrorVib);
        }
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

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        if (preference == mFingerprintSuccessVib) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.FP_SUCCESS_VIBRATE, value ? 1 : 0);
            return true;
        } else if (preference == mFingerprintErrorVib) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.FP_ERROR_VIBRATE, value ? 1 : 0);
            return true;
        } else if (preference == mRandomudfps) {
            boolean value = (Boolean) newValue;
            mUdfpsAnimationFragment.setEnabled(!value);
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.UDFPS_ANIM_STYLE, value ? 1 : 0);
            Settings.System.putIntForUser(getActivity().getContentResolver(), UDFPS_ANIM_RANDOMIZE, value ? 1 : 0, UserHandle.USER_CURRENT);
            customUtils.showSystemUiRestartDialog(getActivity());
        }
        return true;
    }

}
