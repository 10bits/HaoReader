package com.monke.monkeybook.help.permission;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

/**
 * create on 2019/2/13
 *
 * by 咸鱼
 */
public class PermissionActivity extends AppCompatActivity {

    public static final String KEY_INPUT_REQUEST_TYPE = "KEY_INPUT_REQUEST_TYPE";
    public static final String KEY_INPUT_PERMISSIONS_CODE = "KEY_INPUT_PERMISSIONS_CODE";
    public static final String KEY_INPUT_PERMISSIONS = "KEY_INPUT_PERMISSIONS";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        int type = intent.getIntExtra(KEY_INPUT_REQUEST_TYPE, Request.TYPE_REQUEST_PERMISSION);

        switch (type) {
            case Request.TYPE_REQUEST_PERMISSION://权限请求
                int requestCode = intent.getIntExtra(KEY_INPUT_PERMISSIONS_CODE, 1000);
                String[] permissions = intent.getStringArrayExtra(KEY_INPUT_PERMISSIONS);
                if (permissions != null) {
                    ActivityCompat.requestPermissions(this, permissions, requestCode);
                } else {
                    finish();
                }
                break;
            case Request.TYPE_REQUEST_SETTING://跳转到设置界面
                Intent settingIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                settingIntent.setData(Uri.fromParts("package", getPackageName(), null));
                startActivityForResult(settingIntent, Request.TYPE_REQUEST_SETTING);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (RequestPlugins.sRequestCallback != null) {
            RequestPlugins.sRequestCallback.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (RequestPlugins.sRequestCallback != null) {
            RequestPlugins.sRequestCallback.onActivityResult(requestCode, resultCode, data);
        }
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
