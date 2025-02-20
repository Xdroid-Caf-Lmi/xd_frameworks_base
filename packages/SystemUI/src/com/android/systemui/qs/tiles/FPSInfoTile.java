/*
 * Copyright (C) 2017-2021 crDroidAndroid Project
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

package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.provider.Settings;
import android.provider.Settings.System;
import android.service.quicksettings.Tile;

import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.SystemSetting;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.R;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import java.io.BufferedReader;
import java.io.FileReader;

import javax.inject.Inject;

/** Quick settings tile: FPSInfo overlay **/
public class FPSInfoTile extends QSTileImpl<BooleanState> {

    private final SystemSetting mSetting;
    private final Icon mIcon = ResourceIcon.get(R.drawable.ic_qs_fps_info);

    @Inject
    public FPSInfoTile(QSHost host) {
        super(host);

        mSetting = new SystemSetting(mContext, mHandler, System.SHOW_FPS_OVERLAY) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                handleRefreshState(value);
            }
        };
    }

    @Override
    public BooleanState newTileState() {
        BooleanState state = new BooleanState();
        state.handlesLongClick = false;
        return state;
    }

    @Override
    protected void handleClick() {
        mSetting.setValue(mState.value ? 0 : 1);
        refreshState();
        toggleState();
    }

    protected void toggleState() {
        Intent service = (new Intent())
                .setClassName("com.android.systemui",
                "com.android.systemui.FPSInfoService");
        if (mSetting.getValue() == 0) {
            mContext.stopService(service);
        } else {
            mContext.startService(service);
        }
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        if (mSetting == null) return;
        final int value = arg instanceof Integer ? (Integer)arg : mSetting.getValue();
        final boolean fpsInfoEnabled = value != 0;
        state.value = fpsInfoEnabled;
        state.label = mContext.getString(R.string.quick_settings_fpsinfo_label);
        state.icon = mIcon;
        state.contentDescription =  mContext.getString(
                R.string.quick_settings_fpsinfo_label);
        if (fpsInfoEnabled) {
            state.state = Tile.STATE_ACTIVE;
        } else {
            state.state = Tile.STATE_INACTIVE;
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_fpsinfo_label);
    }

    @Override
    protected String composeChangeAnnouncement() {
        return mContext.getString(R.string.quick_settings_fpsinfo_label);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.XD_ZONE;
    }

    @Override
    public void handleSetListening(boolean listening) {
        // Do nothing
    }

    @Override
    public boolean isAvailable() {
        return readOneLine(mContext.getResources().getString(R.string.config_fpsInfoSysNode));
    }

    private static boolean readOneLine(String fname) {
        BufferedReader br;
        String line = null;
        try {
            br = new BufferedReader(new FileReader(fname), 512);
            try {
                line = br.readLine();
            } finally {
                br.close();
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
