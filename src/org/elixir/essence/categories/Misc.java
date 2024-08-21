/*
 * Copyright (C) 2016 AospExtended ROM Project
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
import android.os.Bundle;
import android.os.SystemProperties;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

public class Misc extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "Misc";
    private static final String KEY_GAMES_SPOOF = "use_games_spoof";
    private static final String SYS_GAMES_SPOOF = "persist.sys.pixelprops.games";

    private SwitchPreference mGamesSpoof;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.misc);

        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefSet = getPreferenceScreen();
        
        mGamesSpoof = (SwitchPreference) findPreference(KEY_GAMES_SPOOF);
        if (mGamesSpoof != null) {
            mGamesSpoof.setChecked(SystemProperties.getBoolean(SYS_GAMES_SPOOF, false));
            mGamesSpoof.setOnPreferenceChangeListener(this);
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
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (preference == mGamesSpoof) {
            boolean value = (Boolean) objValue;
            SystemProperties.set(SYS_GAMES_SPOOF, value ? "true" : "false");
            return true;
        }
        return false;
    }
}
