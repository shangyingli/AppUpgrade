package com.transsion.appupgrade;

import android.os.Build;

public class BuildUtils {

    public static boolean isAtLeast24Api() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? true : false;
    }
    public static boolean isAtLeast26Api() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? true : false;
    }

}
