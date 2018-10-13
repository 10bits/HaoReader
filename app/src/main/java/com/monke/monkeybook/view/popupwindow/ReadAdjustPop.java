//Copyright (c) 2017. 章钦豪. All rights reserved.
package com.monke.monkeybook.view.popupwindow;

import android.app.Activity;
import android.content.ContentResolver;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.monke.monkeybook.R;
import com.monke.monkeybook.help.ReadBookControl;
import com.monke.monkeybook.view.activity.ReadBookActivity;
import com.monke.monkeybook.widget.checkbox.SmoothCheckBox;
import com.monke.mprogressbar.MHorProgressBar;
import com.monke.mprogressbar.OnProgressListener;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ReadAdjustPop extends PopupWindow {
    @BindView(R.id.hpb_light)
    MHorProgressBar hpbLight;
    @BindView(R.id.scb_follow_sys)
    SmoothCheckBox scbFollowSys;
    @BindView(R.id.ll_follow_sys)
    LinearLayout llFollowSys;
    @BindView(R.id.ll_click)
    LinearLayout llClick;
    @BindView(R.id.hpb_click)
    MHorProgressBar hpbClick;
    @BindView(R.id.ll_tts_SpeechRate)
    LinearLayout llTtsSpeechRate;
    @BindView(R.id.hpb_tts_SpeechRate)
    MHorProgressBar hpbTtsSpeechRate;
    @BindView(R.id.scb_tts_follow_sys)
    SmoothCheckBox scbTtsFollowSys;
    @BindView(R.id.tv_auto_page)
    TextView tvAutoPage;

    private ReadBookActivity activity;
    private Boolean isFollowSys;
    private int light;
    private ReadBookControl readBookControl = ReadBookControl.getInstance();
    private OnAdjustListener adjustListener;

    public interface OnAdjustListener {
        void changeSpeechRate(int speechRate);

        void speechRateFollowSys();
    }

    public ReadAdjustPop(ReadBookActivity readBookActivity, OnAdjustListener adjustListener) {
        super(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.activity = readBookActivity;
        this.adjustListener = adjustListener;

        View view = LayoutInflater.from(activity).inflate(R.layout.pop_read_adjust, null);
        this.setContentView(view);
        ButterKnife.bind(this, view);
        initData();
        bindEvent();

        setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.shape_pop_checkaddshelf_bg));
        setFocusable(true);
        setTouchable(true);
        setAnimationStyle(R.style.anim_pop_windowlight);
    }

    private void initData() {
        isFollowSys = readBookControl.getLightIsFollowSys();
        light = readBookControl.getScreenLight(getScreenBrightness(activity));
    }

    private void bindEvent() {
        //亮度调节
        llFollowSys.setOnClickListener(v -> {
            if (scbFollowSys.isChecked()) {
                scbFollowSys.setChecked(false, true);
            } else {
                scbFollowSys.setChecked(true, true);
            }
        });
        scbFollowSys.setOnCheckedChangeListener((checkBox, isChecked) -> {
            isFollowSys = isChecked;
            if (isChecked) {
                //跟随系统
                hpbLight.setCanTouch(false);
                setScreenBrightness(activity);
            } else {
                //不跟随系统
                hpbLight.setCanTouch(true);
                hpbLight.setDurProgress(light);
            }
        });
        hpbLight.setProgressListener(new OnProgressListener() {
            @Override
            public void moveStartProgress(float dur) {

            }

            @Override
            public void durProgressChange(float dur) {
                if (!isFollowSys) {
                    light = (int) dur;
                    setScreenBrightness(activity, (int) dur);
                }
            }

            @Override
            public void moveStopProgress(float dur) {

            }

            @Override
            public void setDurProgress(float dur) {

            }
        });

        //自动翻页间隔
        hpbClick.setMaxProgress(180);

        hpbClick.setDurProgress(readBookControl.getClickSensitivity());
        hpbClick.setProgressListener(new OnProgressListener() {
            @Override
            public void moveStartProgress(float dur) {

            }

            @Override
            public void durProgressChange(float dur) {
                tvAutoPage.setText(String.format("%sS", (int) dur));
                readBookControl.setClickSensitivity((int) dur);
            }

            @Override
            public void moveStopProgress(float dur) {

            }

            @Override
            public void setDurProgress(float dur) {

            }
        });

        //朗读语速调节
        scbTtsFollowSys.setChecked(readBookControl.isSpeechRateFollowSys());
        if (readBookControl.isSpeechRateFollowSys()) {
            hpbTtsSpeechRate.setCanTouch(false);
        }
        llTtsSpeechRate.setOnClickListener(v -> {
            if (scbTtsFollowSys.isChecked()) {
                scbTtsFollowSys.setChecked(false, true);
                //不跟随系统
                hpbTtsSpeechRate.setCanTouch(true);
                readBookControl.setSpeechRateFollowSys(false);
                if (adjustListener != null) {
                    adjustListener.changeSpeechRate(readBookControl.getSpeechRate());
                }
            } else {
                scbTtsFollowSys.setChecked(true, true);
                //跟随系统
                hpbTtsSpeechRate.setCanTouch(false);
                readBookControl.setSpeechRateFollowSys(true);
                if (adjustListener != null) {
                    adjustListener.speechRateFollowSys();
                }
            }
        });
        hpbTtsSpeechRate.setDurProgress(readBookControl.getSpeechRate() - 5);
        hpbTtsSpeechRate.setProgressListener(new OnProgressListener() {
            @Override
            public void moveStartProgress(float dur) {

            }

            @Override
            public void durProgressChange(float dur) {

            }

            @Override
            public void moveStopProgress(float dur) {
                readBookControl.setSpeechRate((int) dur + 5);
                if (adjustListener != null) {
                    adjustListener.changeSpeechRate(readBookControl.getSpeechRate());
                }
            }

            @Override
            public void setDurProgress(float dur) {

            }
        });
    }

    @Override
    public void dismiss() {
        readBookControl.saveLight(light, isFollowSys);
        super.dismiss();
    }

    @Override
    public void showAtLocation(View parent, int gravity, int x, int y) {
        super.showAtLocation(parent, gravity, x, y);
        initData();
        hpbLight.setDurProgress(light);
        scbFollowSys.setChecked(isFollowSys);
    }

    public static void setScreenBrightness(Activity activity, int value) {
        WindowManager.LayoutParams params = (activity).getWindow().getAttributes();
        params.screenBrightness = value * 1.0f / 255f;
        (activity).getWindow().setAttributes(params);
    }

    public static void setScreenBrightness(Activity activity) {
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        activity.getWindow().setAttributes(params);
    }

    public static int getScreenBrightness(Activity activity) {
        int value = 1;
        ContentResolver cr = activity.getContentResolver();
        try {
            value = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return value;
    }
}
