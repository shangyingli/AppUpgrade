package com.transsion.appupgrade;

import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LogUtils {

    private static final int INDENT = 2;

    private static boolean debug = false;

    public static void setDebug(boolean debug) {
        LogUtils.debug = debug;
    }

    public static boolean getDebuggable() {
        return debug;
    }

    static String getTag() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        StackTraceElement element = elements[4];
        String threadName = "";
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            threadName = Thread.currentThread().getName();
        }
        return "[RxJave]" + threadName
                + "(" + element.getFileName()
                + ":" + element.getLineNumber() + ")." + element.getMethodName() + "()";
    }

    public static void d(String msg) {
        Log.d(getTag(), msg);
    }

    public static void e(String msg) {
        Log.e(getTag(), msg);
    }

    public static void w(String msg) {
        Log.w(getTag(), msg);
    }

    public static void i(String msg) {
        Log.i(getTag(), msg);
    }

    public static void json(String str) {
        if (TextUtils.isEmpty(str)) {
            d("json str is empty!");
            return;
        }

        try {
            str = str.trim();
            if (str.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(str);
                String jsonStr = jsonObject.toString(INDENT);
                Log.d(getTag(), jsonStr);
                return;
            }
            if (str.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(str);
                String jsonStr = jsonArray.toString(INDENT);
                Log.d(getTag(), jsonStr);
                return;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
