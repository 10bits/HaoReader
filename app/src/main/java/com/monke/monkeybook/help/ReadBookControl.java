//Copyright (c) 2017. 章钦豪. All rights reserved.
package com.monke.monkeybook.help;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.monke.monkeybook.MApplication;
import com.monke.monkeybook.utils.BitmapUtil;
import com.monke.monkeybook.widget.page.PageMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReadBookControl {
    private static final int DEFAULT_BG = 1;

    private List<Map<String, Integer>> textDrawable;
    private int speechRate;
    private boolean speechRateFollowSys;
    private int textSize;
    private int textColor;
    private boolean bgIsColor;
    private int bgColor;
    private int lineSpacing;
    private int paragraphSpacing;
    private int pageMode;
    private String bgPath;
    private Bitmap bgBitmap;

    private int textDrawableIndex = DEFAULT_BG;

    private Boolean hideStatusBar;
    private String fontPath;
    private int textConvert;
    private Boolean textBold;
    private Boolean canClickTurn;
    private Boolean canKeyTurn;
    private Boolean readAloudCanKeyTurn;
    private int clickSensitivity;
    private Boolean clickAllNext;
    private Boolean showTimeBattery;
    private String lastNoteUrl;
    private Boolean darkStatusIcon;
    private int screenTimeOut;
    private int paddingLeft;
    private int paddingTop;
    private int paddingRight;
    private int paddingBottom;

    private SharedPreferences readPreference;

    private volatile static ReadBookControl readBookControl;

    public static ReadBookControl getInstance() {
        if (readBookControl == null) {
            synchronized (ReadBookControl.class) {
                if (readBookControl == null) {
                    readBookControl = new ReadBookControl();
                }
            }
        }
        return readBookControl;
    }

    private ReadBookControl() {
        initTextDrawable();
        readPreference = MApplication.getInstance().getSharedPreferences("CONFIG", 0);
        this.hideStatusBar = readPreference.getBoolean("hide_status_bar", false);
        this.textSize = readPreference.getInt("textSize", 18);
        this.canClickTurn = readPreference.getBoolean("canClickTurn", true);
        this.canKeyTurn = readPreference.getBoolean("canKeyTurn", true);
        this.readAloudCanKeyTurn = readPreference.getBoolean("readAloudCanKeyTurn", true);
        this.lineSpacing = readPreference.getInt("lineSpacing", 4);
        this.paragraphSpacing = readPreference.getInt("paragraphSpacing",16);
        this.clickSensitivity = readPreference.getInt("clickSensitivity", 50) > 100
                ? 50 : readPreference.getInt("clickSensitivity", 50);
        this.clickAllNext = readPreference.getBoolean("clickAllNext", false);
        this.fontPath = readPreference.getString("fontPath", null);
        this.textConvert = readPreference.getInt("textConvertInt", 0);
        this.textBold = readPreference.getBoolean("textBold", false);
        this.speechRate = readPreference.getInt("speechRate", 10);
        this.speechRateFollowSys = readPreference.getBoolean("speechRateFollowSys", true);
        this.showTimeBattery = readPreference.getBoolean("showTimeBattery", true);
        this.lastNoteUrl = readPreference.getString("lastNoteUrl", "");
        this.screenTimeOut = readPreference.getInt("screenTimeOut", 0);
        this.paddingLeft = readPreference.getInt("paddingLeft", 24);
        this.paddingTop = readPreference.getInt("paddingTop", 16);
        this.paddingRight = readPreference.getInt("paddingRight", 24);
        this.paddingBottom = readPreference.getInt("paddingBottom", 0);
        this.pageMode = readPreference.getInt("pageMode", 0);

        initPageConfiguration();
    }

    //阅读背景
    private void initTextDrawable() {
        if (null == textDrawable) {
            textDrawable = new ArrayList<>();
            Map<String, Integer> temp1 = new HashMap<>();
            temp1.put("textColor", Color.parseColor("#3E3D3B"));
            temp1.put("bgIsColor", 1);
            temp1.put("textBackground", Color.parseColor("#F3F3F3"));
            temp1.put("darkStatusIcon", 1);
            textDrawable.add(temp1);

            Map<String, Integer> temp2 = new HashMap<>();
            temp2.put("textColor", Color.parseColor("#5E432E"));
            temp2.put("bgIsColor", 1);
            temp2.put("textBackground", Color.parseColor("#C6BAA1"));
            temp2.put("darkStatusIcon", 1);
            textDrawable.add(temp2);

            Map<String, Integer> temp3 = new HashMap<>();
            temp3.put("textColor", Color.parseColor("#22482C"));
            temp3.put("bgIsColor", 1);
            temp3.put("textBackground", Color.parseColor("#E1F1DA"));
            temp3.put("darkStatusIcon", 1);
            textDrawable.add(temp3);

            Map<String, Integer> temp4 = new HashMap<>();
            temp4.put("textColor", Color.parseColor("#FFFFFF"));
            temp4.put("bgIsColor", 1);
            temp4.put("textBackground", Color.parseColor("#015A86"));
            temp4.put("darkStatusIcon", 0);
            textDrawable.add(temp4);

            Map<String, Integer> temp5 = new HashMap<>();
            temp5.put("textColor", Color.parseColor("#a3a3a3"));
            temp5.put("bgIsColor", 1);
            temp5.put("textBackground", Color.parseColor("#2a2a2a"));
            temp5.put("darkStatusIcon", 0);
            textDrawable.add(temp5);
        }
    }

    public void initPageConfiguration() {
        if (getIsNightTheme()) {
            textDrawableIndex = readPreference.getInt("textDrawableIndexNight", 4);
        } else {
            textDrawableIndex = readPreference.getInt("textDrawableIndex", DEFAULT_BG);
        }
        if (textDrawableIndex < 0) {
            textDrawableIndex = DEFAULT_BG;
        }
        setPageStyle();

        setTextDrawable();
    }

    private void setPageStyle() {
        try {
            bgColor = textDrawable.get(textDrawableIndex).get("textBackground");
            if (getBgCustom(textDrawableIndex) == 2 && getBgPath(textDrawableIndex) != null) {
                bgIsColor = false;
                bgPath = getBgPath(textDrawableIndex);
                bgBitmap = BitmapFactory.decodeFile(bgPath);
                bgBitmap = BitmapUtil.fitBitmap(bgBitmap, 600);
                return;
            } else if (getBgCustom(textDrawableIndex) == 1) {
                bgIsColor = true;
                bgColor = getBgColor(textDrawableIndex);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        bgIsColor = true;
        bgColor = textDrawable.get(textDrawableIndex).get("textBackground");
    }

    private void setTextDrawable() {
        darkStatusIcon = getDarkStatusIcon(textDrawableIndex);
        textColor = getTextColor(textDrawableIndex);
    }

    public int getTextColor(int textDrawableIndex) {
        if (readPreference.getInt("textColor" + textDrawableIndex, 0) != 0) {
            return readPreference.getInt("textColor" + textDrawableIndex, 0);
        } else {
            return getDefaultTextColor(textDrawableIndex);
        }
    }

    public void setTextColor(int textDrawableIndex, int textColor) {
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putInt("textColor" + textDrawableIndex, textColor);
        editor.apply();
    }

    public Drawable getBgDrawable(int textDrawableIndex, Context context) {
        try {
            switch (getBgCustom(textDrawableIndex)) {
                case 2:
                    Bitmap bitmap = BitmapFactory.decodeFile(getBgPath(textDrawableIndex));
                    if (bitmap != null) {
                        return new BitmapDrawable(context.getResources(), bitmap);
                    }
                    break;
                case 1:
                    return new ColorDrawable(getBgColor(textDrawableIndex));
            }
            if (textDrawable.get(textDrawableIndex).get("bgIsColor") != 0) {
                return new ColorDrawable(textDrawable.get(textDrawableIndex).get("textBackground"));
            } else {
                return getDefaultBgDrawable(textDrawableIndex, context);
            }
        } catch (Exception e) {
            if (textDrawable.get(textDrawableIndex).get("bgIsColor") != 0) {
                return new ColorDrawable(textDrawable.get(textDrawableIndex).get("textBackground"));
            } else {
                return getDefaultBgDrawable(textDrawableIndex, context);
            }
        }
    }

    public Drawable getDefaultBgDrawable(int textDrawableIndex, Context context) {
        if (textDrawable.get(textDrawableIndex).get("bgIsColor") != 0) {
            return new ColorDrawable(textDrawable.get(textDrawableIndex).get("textBackground"));
        } else {
            return context.getResources().getDrawable(getDefaultBg(textDrawableIndex));
        }
    }

    public Drawable getBgDrawable(Context context) {
        return getBgDrawable(textDrawableIndex, context);
    }

    public int getBgCustom(int textDrawableIndex) {
        return readPreference.getInt("bgCustom" + textDrawableIndex, 0);
    }

    public void setBgCustom(int textDrawableIndex, int bgCustom) {
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putInt("bgCustom" + textDrawableIndex, bgCustom);
        editor.apply();
    }

    public String getBgPath(int textDrawableIndex) {
        return readPreference.getString("bgPath" + textDrawableIndex, null);
    }

    public void setBgPath(int textDrawableIndex, String bgUri) {
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putString("bgPath" + textDrawableIndex, bgUri);
        editor.apply();
    }

    public int getDefaultTextColor(int textDrawableIndex) {
        return textDrawable.get(textDrawableIndex).get("textColor");
    }

    private int getDefaultBg(int textDrawableIndex) {
        return textDrawable.get(textDrawableIndex).get("textBackground");
    }

    public int getBgColor(int index) {
        return readPreference.getInt("bgColor" + index, Color.parseColor("#C6BAA1"));
    }

    public void setBgColor(int index, int bgColor) {
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putInt("bgColor" + index, bgColor);
        editor.apply();
    }

    public boolean getIsNightTheme() {
        return readPreference.getBoolean("nightTheme", false);
    }

    public boolean getImmersionStatusBar() {
        return readPreference.getBoolean("immersionStatusBar", false);
    }

    public void setImmersionStatusBar(boolean immersionStatusBar) {
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putBoolean("immersionStatusBar", immersionStatusBar);
        editor.apply();
    }

    public String getLastNoteUrl() {
        return lastNoteUrl;
    }

    public void setLastNoteUrl(String lastNoteUrl) {
        this.lastNoteUrl = lastNoteUrl;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putString("lastNoteUrl", lastNoteUrl);
        editor.apply();
    }

    public int getTextSize() {
        return textSize;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putInt("textSize", textSize);
        editor.apply();
    }

    public int getTextColor() {
        return textColor;
    }

    public boolean bgIsColor() {
        return bgIsColor;
    }

    public int getDefaultBgColor() {
        return getDefaultBg(textDrawableIndex);
    }

    public int getBgColor() {
        return bgColor;
    }

    public String getBgPath() {
        return bgPath;
    }

    public Bitmap getBgBitmap() {
        return bgBitmap.copy(Bitmap.Config.RGB_565, true);
    }

    public int getTextDrawableIndex() {
        return textDrawableIndex;
    }

    public void setTextDrawableIndex(int textDrawableIndex) {
        this.textDrawableIndex = textDrawableIndex;
        SharedPreferences.Editor editor = readPreference.edit();
        if (getIsNightTheme()) {
            editor.putInt("textDrawableIndexNight", textDrawableIndex);
        } else {
            editor.putInt("textDrawableIndex", textDrawableIndex);
        }
        editor.apply();
        setTextDrawable();
    }

    public void setTextConvert(int textConvert) {
        this.textConvert = textConvert;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putInt("textConvertInt", textConvert);
        editor.apply();
    }

    public void setTextBold(boolean textBold) {
        this.textBold = textBold;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putBoolean("textBold", textBold);
        editor.apply();
    }

    public void setReadBookFont(String fontPath) {
        this.fontPath = fontPath;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putString("fontPath", fontPath);
        editor.apply();
    }

    public String getFontPath() {
        return fontPath;
    }

    public int getTextConvert() {
        return textConvert == -1 ? 2 : textConvert;
    }

    public Boolean getTextBold() {
        return textBold;
    }

    public List<Map<String, Integer>> getTextDrawable() {
        return textDrawable;
    }

    public Boolean getCanKeyTurn(Boolean isPlay) {
        if (!canKeyTurn) {
            return false;
        } else if (readAloudCanKeyTurn) {
            return true;
        } else {
            return !isPlay;
        }
    }

    public Boolean getCanKeyTurn() {
        return canKeyTurn;
    }

    public void setCanKeyTurn(Boolean canKeyTurn) {
        this.canKeyTurn = canKeyTurn;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putBoolean("canKeyTurn", canKeyTurn);
        editor.apply();
    }

    public Boolean getAloudCanKeyTurn() {
        return readAloudCanKeyTurn;
    }

    public void setAloudCanKeyTurn(Boolean canAloudKeyTurn) {
        this.readAloudCanKeyTurn = canAloudKeyTurn;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putBoolean("readAloudCanKeyTurn", canAloudKeyTurn);
        editor.apply();
    }

    public Boolean getCanClickTurn() {
        return canClickTurn;
    }

    public void setCanClickTurn(Boolean canClickTurn) {
        this.canClickTurn = canClickTurn;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putBoolean("canClickTurn", canClickTurn);
        editor.apply();
    }

    public float getLineSpacing() {
        return lineSpacing;
    }

    public void setLineSpacing(int lineSpacing) {
        this.lineSpacing = lineSpacing;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putInt("lineSpacing", lineSpacing);
        editor.apply();
    }

    public float getParagraphSpacing() {
        return paragraphSpacing;
    }

    public void setParagraphSpacing(int paragraphSpacing) {
        this.paragraphSpacing = paragraphSpacing;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putInt("paragraphSpacing", paragraphSpacing);
        editor.apply();
    }

    public int getClickSensitivity() {
        return clickSensitivity;
    }

    public void setClickSensitivity(int clickSensitivity) {
        this.clickSensitivity = clickSensitivity;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putInt("clickSensitivity", clickSensitivity);
        editor.apply();
    }

    public Boolean getClickAllNext() {
        return clickAllNext;
    }

    public void setClickAllNext(Boolean clickAllNext) {
        this.clickAllNext = clickAllNext;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putBoolean("clickAllNext", clickAllNext);
        editor.apply();
    }

    public int getSpeechRate() {
        return speechRate;
    }

    public void setSpeechRate(int speechRate) {
        this.speechRate = speechRate;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putInt("speechRate", speechRate);
        editor.apply();
    }

    public boolean isSpeechRateFollowSys() {
        return speechRateFollowSys;
    }

    public void setSpeechRateFollowSys(boolean speechRateFollowSys) {
        this.speechRateFollowSys = speechRateFollowSys;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putBoolean("speechRateFollowSys", speechRateFollowSys);
        editor.apply();
    }

    public Boolean getShowTimeBattery() {
        return showTimeBattery;
    }

    public void setShowTimeBattery(Boolean showTimeBattery) {
        this.showTimeBattery = showTimeBattery;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putBoolean("showTimeBattery", showTimeBattery);
        editor.apply();
    }

    public Boolean getHideStatusBar() {
        return hideStatusBar;
    }

    public void setHideStatusBar(Boolean hideStatusBar) {
        this.hideStatusBar = hideStatusBar;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putBoolean("hide_status_bar", hideStatusBar);
        editor.apply();
    }

    public boolean getDarkStatusIcon() {
        return darkStatusIcon;
    }

    public boolean getDarkStatusIcon(int textDrawableIndex) {
        return readPreference.getBoolean("darkStatusIcon" + textDrawableIndex, textDrawable.get(textDrawableIndex).get("darkStatusIcon") != 0);
    }

    public void setDarkStatusIcon(int textDrawableIndex, Boolean darkStatusIcon) {
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putBoolean("darkStatusIcon" + textDrawableIndex, darkStatusIcon);
        editor.apply();
    }

    public int getScreenTimeOut() {
        return screenTimeOut;
    }

    public void setScreenTimeOut(int screenTimeOut) {
        this.screenTimeOut = screenTimeOut;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putInt("screenTimeOut", screenTimeOut);
        editor.apply();
    }

    public int getPaddingLeft() {
        return paddingLeft;
    }

    public void setPaddingLeft(int paddingLeft) {
        this.paddingLeft = paddingLeft;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putInt("paddingLeft", paddingLeft);
        editor.apply();
    }

    public int getPaddingTop() {
        return paddingTop;
    }

    public void setPaddingTop(int paddingTop) {
        this.paddingTop = paddingTop;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putInt("paddingTop", paddingTop);
        editor.apply();
    }

    public int getPaddingRight() {
        return paddingRight;
    }

    public void setPaddingRight(int paddingRight) {
        this.paddingRight = paddingRight;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putInt("paddingRight", paddingRight);
        editor.apply();
    }

    public int getPaddingBottom() {
        return paddingBottom;
    }

    public void setPaddingBottom(int paddingBottom) {
        this.paddingBottom = paddingBottom;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putInt("paddingBottom", paddingBottom);
        editor.apply();
    }

    public int getPageMode() {
        return pageMode;
    }

    public PageMode getPageMode(int pageMode) {
        switch (pageMode) {
            case 0:
                return PageMode.COVER;
            case 1:
                return PageMode.SIMULATION;
            case 2:
                return PageMode.SLIDE;
            case 3:
                return PageMode.NONE;
            default:
                return PageMode.COVER;
        }
    }

    public void setPageMode(int pageMode) {
        this.pageMode = pageMode;
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putInt("pageMode", pageMode);
        editor.apply();
    }


    public void saveLight(int light, boolean isFollowSys) {
        SharedPreferences.Editor editor = readPreference.edit();
        editor.putInt("light", light);
        editor.putBoolean("isfollowsys", isFollowSys);
        editor.apply();
    }

    public int getScreenLight(int defaultVal) {
        return readPreference.getInt("light", defaultVal);
    }

    public boolean getLightIsFollowSys() {
        return readPreference.getBoolean("isfollowsys", true);
    }
}
