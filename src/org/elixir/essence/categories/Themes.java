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

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class Themes extends SettingsPreferenceFragment 
	implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "Themes";
    private static final String KEY_EXCLUSIVE_CAT = "exclusive_clock";
    private PreferenceCategory mExclusiveBuild;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.themes);

        final PreferenceScreen screen = getPreferenceScreen();
        final Uri uri = Uri.parse("content://com.elixer.updater.IsExlusiveEnabled");
        Boolean isExclusive = false;
        Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            isExclusive = cursor.getInt(cursor.getColumnIndex("result")) == 1;
            cursor.close();
        }
        Log.w("Essence", "Status of exclusive :- " + isExclusive.toString());
        if (!isExclusive) {
            mExclusiveBuild = (PreferenceCategory) findPreference(KEY_EXCLUSIVE_CAT);
            screen.removePreference(mExclusiveBuild);
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

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

}
