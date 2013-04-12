/*
 * Copyright (C) 2012 The SlimRoms Project
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

package com.android.systemui.quicksettings;

import com.android.systemui.R;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import android.widget.ImageView;

import com.android.systemui.statusbar.phone.QuickSettingsController;
import com.android.systemui.statusbar.phone.QuickSettingsContainerView;
import com.android.systemui.statusbar.phone.QuickSettingsTileView;

import android.provider.ContactsContract;
import android.provider.Settings;

public class CustomShortcutTile extends QuickSettingsTile
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Drawable mAvatar = null;
    private String mShortcutUri;
    private String mCustomIcon;
    private Resources mResources;
    private SharedPreferences mPrefs;
    private int mIconResourceId = 0;
    private int mIconDimens;

    public static QuickSettingsTile getInstance(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, final QuickSettingsController qsc, Handler handler, String id) {
        return new CustomShortcutTile(context, inflater, container, qsc, handler, id);
    }

    public CustomShortcutTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container,
            QuickSettingsController qsc, Handler handler, String id) {
        super(context, inflater, container, qsc);
        tileID = QuickSettingsController.TILE_CUSTOMSHORTCUT+"+"+id;
        mResources = mContext.getResources();
        mPrefs = context.getSharedPreferences("QuickSettingsTilesContent", 0);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mShortcutUri = mPrefs.getString(tileID, null);
        if (mShortcutUri != null) {
            setLabelAndIcon();
        }
        float iconDpDimens = (mContext.getResources()
            .getDimension(R.dimen.shortcut_picker_default_icon_size));
        mIconDimens = (int) ((iconDpDimens * mContext.getResources()
                .getDisplayMetrics().density) + 0.5);

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 if (mShortcutUri != null && mShortcutUri.length() > 0) {
                    try {
                        Intent i = Intent.parseUri(mShortcutUri, 0);
                        if (mShortcutUri.contains(ShortcutPickerActivity.CONTACT_TEMPLATE)) {
                            i = ContactsContract.QuickContact.composeQuickContactsIntent(
                                mContext, v, i.getData(), ContactsContract.QuickContact.MODE_LARGE, null);
                        }
                        startSettingsActivity(i);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
               }
            }
        };
        mOnLongClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent i=new Intent(mContext, ShortcutPickerActivity.class);
                i.putExtra("hashCode", tileID);
                i.putExtra("iconSize", QuickSettingsTile.mTileSize);
                startSettingsActivity(i);
                return true;
            }
        };
    }

    void setLabelAndIcon() {
        Intent i;
        try {
            mShortcutUri = mPrefs.getString(tileID, null);
            if (mShortcutUri == null) return;
            i = Intent.parseUri(mShortcutUri, 0);
            mCustomIcon = i.getStringExtra(ShortcutPickerActivity.ICON_FILE);
            if (mCustomIcon != null) {
                mAvatar = getDrawable(mCustomIcon);
            } else {
                PackageManager pm = mContext.getPackageManager();
                ActivityInfo aInfo = i.resolveActivityInfo(pm, PackageManager.GET_ACTIVITIES);
                if (aInfo != null) {
                    mAvatar = aInfo.loadIcon(pm).mutate();
                } else {
                    mAvatar = mContext.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
                }
            }
            if (ShortcutPickHelper.mContext == null) {
                ShortcutPickHelper.mContext = mContext;
            }
            name = ShortcutPickHelper.getFriendlyNameForUri(mShortcutUri);
            if (name.contains(":")) {
                name = name.substring(name.indexOf(":") + 2);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    void updateQuickSettings() {
        int shortCutType = 0;
        if (mCustomIcon != null && mCustomIcon.endsWith(".png")) {
            if (mShortcutUri != null &&
                    mShortcutUri.contains(ShortcutPickerActivity.SMS_TEMPLATE)) {
                shortCutType = 1;
            } else if (mShortcutUri != null &&
                    mShortcutUri.contains(ShortcutPickerActivity.PHONE_NUMBER_TEMPLATE)) {
                shortCutType = 2;
            } else if (mShortcutUri != null &&
                    mShortcutUri.contains(ShortcutPickerActivity.CONTACT_TEMPLATE)) {
                shortCutType = 3;
            }
        }
        inflateView(shortCutType > 0);

        if (mAvatar == null) {
            mAvatar = mResources.getDrawable(R.drawable.ic_qs_shortcut_andy);
        }
        TextView tv = (TextView) mTile.findViewById(R.id.tile_textview);

        if (shortCutType > 0) {
            ImageView iv = (ImageView) mTile.findViewById(R.id.tile_imageview);
            ImageView overlay = (ImageView) mTile.findViewById(R.id.tile_overlay);
            if (shortCutType == 1) {
                overlay.setImageDrawable(mResources.getDrawable(R.drawable.ic_qs_shortcut_sms));
                overlay.setVisibility(View.VISIBLE);
            } else if (shortCutType == 2) {
                overlay.setImageDrawable(mResources.getDrawable(R.drawable.ic_qs_shortcut_phone));
                overlay.setVisibility(View.VISIBLE);
            }
            iv.setImageDrawable(mAvatar);
        } else {
            tv.setCompoundDrawablesWithIntrinsicBounds(null,
                resize(mAvatar, mIconDimens, mIconDimens), null, null);
        }
        tv.setText(name == null || name.equals("") ? mResources.getString(R.string.qs_shortcut_long_press) : name);
        tv.setTextSize(1, mTileTextSize);
        if (mTileTextColor != -2) {
            tv.setTextColor(mTileTextColor);
        }
    }

    private void inflateView(boolean shortCutType) {
        mTile.removeAllViews();
        if (shortCutType) {
            mTileLayout = R.layout.quick_settings_tile_customshortcut;
        } else {
            mTileLayout = R.layout.quick_settings_tile_generic;
        }
        mTile.setContent(mTileLayout, mInflater);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(tileID)) {
            setLabelAndIcon();
            updateQuickSettings();
            mQsc.updateTilesContent();
        }
    }

    private Drawable getDrawable(String drawableName) {
        mIconResourceId = Resources.getSystem().getIdentifier(drawableName, "drawable", "android");
        if (mIconResourceId == 0) {
            Drawable d = Drawable.createFromPath(drawableName);
            return d;
        } else {
            return Resources.getSystem().getDrawable(mIconResourceId);
        }
    }

    private Drawable resize(Drawable image, int width, int height) {
        Bitmap d = ((BitmapDrawable)image).getBitmap();
        Bitmap bitmapOrig = Bitmap.createScaledBitmap(d, width, height, false);
        return new BitmapDrawable(bitmapOrig);
    }

}
