package com.transsion.appupgrade;

import android.app.Application;

import android.app.Application;
import android.content.Intent;

/**
 * Created by shanshan.yang on 2018/9/25.
 */

public class UpgradeApp extends Application {
    private static final String TAG = "UpgradeApp";

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.d(TAG +  "<============     onCreate     ============");
        Intent intent = new Intent(this, UpgradeService.class);
        startService(intent);
        LogUtils.d(TAG + "--> onCreate");
    }
}