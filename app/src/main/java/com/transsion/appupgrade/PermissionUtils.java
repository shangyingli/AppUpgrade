package com.transsion.appupgrade;

//*************************************************************************/
/*
 *SHENZHEN TRANSSION COMMUNICATION LIMITED  TOP SECRET.
 * Copyright (c) 2016-2036  SHENZHEN TRANSSION COMMUNICATION LIMITED
 *
 * Shenzhen Transsion Communication Limited owns the Proprietary rights in the
 * subject matter of this material.  The information contained in the subject matter are strictly private and confidential.
 * Without prior written permission of Shenzhen Transsion Communication Limited,
 * any reproduction use, modification or disclosure of the information contained in the
 * subject matter are not permitted.
 *
 * Description:
 * Author:
 * Version:
 * Date:
 * Modification:
 *
 */
//*************************************************************************/

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;

public class PermissionUtils {
    private static final String TAG = "PermissionUtil";

    public static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    public static final int REQUEST_READ_EXTERNAL_STORAGE = 2;
    public static final int REQUEST_WRITE_SETTINGS = 3;
    public static final int REQUEST_WRITE_SECURE_SETTINGS = 4;
    public static final int REQUEST_INSTALL_PACKAGE = 5;
    private static final String PACKAGE_URL_SCHEME = "package:";
    public static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    public static final String SETTINGS_CLASS_NAME = "com.itel.utils.PermissionWizardActivity";
    private final ArrayList<String> denyList;
    private Context mContext;
    private final PackageManager mPackageManager;
    private final Intent settingsIntent;
    public static final int OLD_REQUEST_PERMISSION_SETTING = 105;
    public static final int NEW_REQUEST_PERMISSION_SETTING = 106;
    private static PermissionUtils permissionsUtil;

    private PermissionUtils(Context context){
        this.mContext = context;
        mPackageManager = context.getPackageManager();
        settingsIntent = new Intent();
        settingsIntent.setClassName(SETTINGS_PACKAGE_NAME, SETTINGS_CLASS_NAME);
        denyList = new ArrayList<>();
    }

    public static PermissionUtils getInstance(Context context){
        if(permissionsUtil == null){
            synchronized(PermissionUtils.class){
                if(permissionsUtil == null){
                    permissionsUtil = new PermissionUtils(context.getApplicationContext());
                }
            }
        }
        return permissionsUtil;
    }
    public static boolean hasStorageWritePermission(Context context) {
        return (context.checkSelfPermission(Manifest.permission.
                WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean hasStorageReadPermission(Context context) {
        return (context.checkSelfPermission(Manifest.permission.
                READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean hasSettingsWritePermission(Context context) {
        return android.provider.Settings.System.canWrite(context);

    }

    public static boolean hasSecureSettingsWritePermission(Context context) {
        return (context.checkSelfPermission(Manifest.permission.
                WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean hasInstallPackagePermission(Context context) {
        if (BuildUtils.isAtLeast26Api()) {
            return context.getPackageManager().canRequestPackageInstalls();
        } else {
            return true;
        }
    }

    public static void requestPermission(Activity activity, String permission, int requestCode) {
        activity.requestPermissions(new String[]{permission}, requestCode);
    }

    public static boolean shouldShowRequestPermissionRationale(Activity activity, String permission) {
        return activity.shouldShowRequestPermissionRationale(permission);
    }

    public static void requestSettingsWritePermission(Activity activity) {
        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        activity.startActivityForResult(intent, REQUEST_WRITE_SETTINGS);
    }

    public static void requestInstallPackagePermission(Activity activity) {
        if (BuildUtils.isAtLeast26Api()) {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, REQUEST_INSTALL_PACKAGE);
        }
    }

    public static void startPermissionSettings(Activity activity, String packageName) {
        Intent intent = new Intent();
        //intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
        intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", packageName, null));
        activity.startActivity(intent);
    }

    public boolean showMissingPermissionDialog(Activity activity, @NonNull String[] permissions, @NonNull int[] grantResults){
        boolean granted = true;
        denyList.clear();
        for(int i = 0; i < grantResults.length; i++){
            boolean isTip = ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[i]);
            if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                if(!isTip){
                    denyList.add(permissions[i]);
                }
                granted = false;
            }
        }

        if(!granted){
            if(denyList.size() != 0){
                if(mPackageManager.resolveActivity(settingsIntent, 0) != null){
                    settingsIntent.putExtra("pkgName", activity.getPackageName());
                    settingsIntent.putStringArrayListExtra("permissionDenyList", denyList);
                    try {
                        activity.startActivityForResult(settingsIntent, NEW_REQUEST_PERMISSION_SETTING);
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    showDialog(activity);
                }
            } else {
                activity.finish();
            }
            return false;
        }
        return true;
    }

    public void showDialog(final Activity activity){
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle(R.string.help);
        builder.setCancelable(false);
        builder.setMessage(R.string.string_help_text);
        builder.setNegativeButton(R.string.quit, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
                //Toast.makeText(activity,tips,Toast.LENGTH_LONG).show();
                activity.finish();
            }
        });
        builder.setPositiveButton(R.string.settings, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
                startAppSettings(activity);
            }
        });
        builder.show();
    }

    private void startAppSettings(Activity activity){
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse(PACKAGE_URL_SCHEME + activity.getPackageName()));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivityForResult(intent, OLD_REQUEST_PERMISSION_SETTING);
    }

}