/*
 * Copyright (C) 2024 Project Elixir
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.res.Resources;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.ServiceManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;
import androidx.preference.PreferenceViewHolder;
import android.view.ViewGroup.LayoutParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.RecyclerView;
import android.net.Uri;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.custom.customUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;

import com.bumptech.glide.Glide;

import com.android.internal.util.custom.ThemeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import org.json.JSONObject;
import org.json.JSONException;

import static android.os.UserHandle.USER_SYSTEM;
import static android.provider.Settings.Secure.LOCKSCREEN_CURRENT_CLOCK_OVERLAY;

public class LSClocks extends SettingsPreferenceFragment implements
        OnCheckedChangeListener {
    
    private SettingsMainSwitchBar switchBar;

    private RecyclerView mRecyclerView;
    private ThemeUtils mThemeUtils;
    private ContentResolver resolver;
    private String mCategory = "android.systemui.custom.lsclocks";
    private static String TAG = "Essence: LSClocks";
    private Adapter mAdapter;
    private boolean mEnabled;
    private IOverlayManager mOverlayService;

    private List<String> mPkgs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.ls_clock_elixir_customisation_title);

        mThemeUtils = new ThemeUtils(getActivity());
        resolver = getActivity().getContentResolver();
        mOverlayService = IOverlayManager.Stub
                .asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));
        mPkgs = mThemeUtils.getOverlayPackagesForCategory(mCategory, "com.android.systemui");
        Collections.sort(mPkgs);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.clock_view, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 3);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        mAdapter = new Adapter(getActivity());
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final SettingsActivity activity = (SettingsActivity) getActivity();
        switchBar = activity.getSwitchBar();
        mEnabled = Settings.Secure.getInt(resolver, Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK, 0) != 0;
        switchBar.setChecked(mEnabled);
        setEnabled(mEnabled);
        switchBar.setTitle(getActivity().getString(R.string.enable));
        switchBar.addOnSwitchChangeListener(this);
        switchBar.show();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CUSTOM_SETTINGS;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Settings.Secure.putInt(resolver, Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK, isChecked ? 1 : 0);
        if (isChecked) {
            customUtils.setAutomatedDepthClock(resolver, 0);
            RROManager("com.android.systemui.elixir.hide.smartspace", false);
            Settings.Secure.putInt(resolver, Settings.Secure.LOCKSCREEN_DEPTH_CLOCK, 0);
        }
        String currentClk = getCurrentClock();
        if (!"com.android.systemui".equals(currentClk) && !"".equals(currentClk)) {
            mThemeUtils.setOverlayDisabled(getCurrentClock());
        }
        reloadClock();
        switchBar.setChecked(isChecked);
        setEnabled(isChecked);
    }

    private void RROManager(String name, boolean status) {
        Log.d(TAG,  status ? "Enabling" : "Disabling" + " Overlay Package :- " + name);
        try {
            mOverlayService.setEnabled(name, status, USER_SYSTEM);
        } catch (Exception re) {
            Log.e(TAG, String.valueOf(re));
        }
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            mRecyclerView.setAdapter(mAdapter);
        } else {
            mRecyclerView.setAdapter(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public class Adapter extends RecyclerView.Adapter<Adapter.CustomViewHolder> {
        Context context;
        String mSelectedPkg;
        String mAppliedPkg;

        public Adapter(Context context) {
            this.context = context;
        }

        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.clock_option, parent, false);
            CustomViewHolder vh = new CustomViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(CustomViewHolder holder, final int position) {
            String clockPkg = mPkgs.get(position);

            holder.clock1.setBackgroundDrawable(getDrawable(holder.clock1.getContext(), clockPkg, "ic_elixir_clock_preview"));

            String currentPackageName = mThemeUtils.getOverlayInfos(mCategory, "com.android.systemui").stream()
                .filter(info -> info.isEnabled())
                .map(info -> info.packageName)
                .findFirst()
                .orElse("com.android.systemui");

            holder.name.setText("com.android.systemui".equals(clockPkg) ? "Default" : getLabel(holder.name.getContext(), clockPkg));

            if (currentPackageName.equals(clockPkg)) {
                mAppliedPkg = clockPkg;
                if (mSelectedPkg == null) {
                    mSelectedPkg = clockPkg;
                }
            }

            holder.itemView.setActivated(clockPkg == mSelectedPkg);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateActivatedStatus(mSelectedPkg, false);
                    updateActivatedStatus(clockPkg, true);
                    mSelectedPkg = clockPkg;
                    enableOverlays(position);
                    if (!"com.android.systemui".equals(mSelectedPkg)) {
                        mThemeUtils.setOverlayDisabled(getCurrentClock());
                        setCurrentClock(mSelectedPkg);
                    }
                    reloadClock();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mPkgs.size();
        }

        public class CustomViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            ImageView clock1;
            public CustomViewHolder(View itemView) {
                super(itemView);
                name = (TextView) itemView.findViewById(R.id.option_label);
                clock1 = (ImageView) itemView.findViewById(R.id.clock1);
            }
        }

        private void updateActivatedStatus(String pkg, boolean isActivated) {
            int index = mPkgs.indexOf(pkg);
            if (index < 0) {
                return;
            }
            RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(index);
            if (holder != null && holder.itemView != null) {
                holder.itemView.setActivated(isActivated);
            }
        }
    }

    // Clocks that doesnt have a built in date view
    private static final String[] mDateViewShowClocks = {
        "com.android.systemui.lsclock.nothing",
        "com.android.systemui.lsclock.hyperbox",
        "com.android.systemui.lsclock.oosalike"
    };

    private String getCurrentClock() {
        return Settings.Secure.getString(resolver, LOCKSCREEN_CURRENT_CLOCK_OVERLAY);
    }

    private void setCurrentClock(String pkg) {
        Settings.Secure.putString(resolver, LOCKSCREEN_CURRENT_CLOCK_OVERLAY, pkg);
    }

    public Drawable getDrawable(Context context, String pkg, String drawableName) {
        Log.i(TAG, "Getting drawable from package :- " + pkg);
        if (pkg.equals("com.android.systemui"))
            pkg = "com.android.settings";
        try {
            PackageManager pm = context.getPackageManager();
            Resources res = pm.getResourcesForApplication(pkg);
            int resId = res.getIdentifier(drawableName, "drawable", pkg);
            return res.getDrawable(resId);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource not found in pkg\n" + pkg + drawableName);
        }
        return null;
    }

    public String getLabel(Context context, String pkg) {
        PackageManager pm = context.getPackageManager();
        try {
            return pm.getApplicationInfo(pkg, 0)
                    .loadLabel(pm).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return pkg;
    }

    public void enableOverlays(int position) {
        mThemeUtils.setOverlayEnabled(mCategory, mPkgs.get(position), "com.android.systemui");
    }

    private void reloadClock() {
        customUtils.refreshCustomClock(resolver);
    }

}
