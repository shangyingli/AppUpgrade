package com.transsion.appupgrade;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button mButton;
    boolean hasInstallPackagePermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButton = (Button) findViewById(R.id.start);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File filePath = getApplicationContext().getExternalCacheDir();
                File file = new File(filePath, "TsFileManager.apk");
                LogUtils.d("===========onClick==========" + "file=" + file + ",filePath=" + filePath);
                if (!file.exists()) {
                    LogUtils.d(" file not exist!");
                    return;
                }
                LogUtils.d("===========onClick==========" + "file=" + file);
                try {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Uri contentUri = FileProvider.getUriForFile(MainActivity.this, "com.transsion.appupgrade.fileprovider", file);
                    intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
                    LogUtils.d("===========onClick==========" + "contentUri=" + contentUri);
                    List<ResolveInfo> resolveInfos = MainActivity.this.getPackageManager().queryIntentActivities(
                            intent, PackageManager.MATCH_DEFAULT_ONLY);
                    // Grant permissions for any app that can handle a file to access it
                    if (resolveInfos != null && resolveInfos.size() > 0) {
                        for (ResolveInfo resolveInfo : resolveInfos) {
                            String packageName = resolveInfo.activityInfo.packageName;
                            LogUtils.d("===========onClick==========" + "packageName=" + packageName);
                            MainActivity.this.grantUriPermission(packageName, contentUri,
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }
                    }
                    LogUtils.d("===========onClick==========" + "intent=" + intent);
                    startActivity(intent);
                } catch (Exception e) {
                    LogUtils.e("===========onClick==========" + "installApkDefaul :" + e.toString());
                }

            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtils.REQUEST_INSTALL_PACKAGE) {
            boolean hasInstallPackagePermission = PermissionUtils.hasInstallPackagePermission(this);
            LogUtils.w("===========onRequestPermissionsResult==========" + "[onActivityResult] hasInstallPackagePermission=" + hasInstallPackagePermission);
            if (!hasInstallPackagePermission) {
                PermissionUtils.requestInstallPackagePermission(this);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PermissionUtils.REQUEST_INSTALL_PACKAGE) {
            hasInstallPackagePermission = PermissionUtils.hasInstallPackagePermission(this);
            LogUtils.w("===========onRequestPermissionsResult==========" + "[onActivityResult] hasInstallPackagePermission=" + hasInstallPackagePermission);
            if (!hasInstallPackagePermission) {
                PermissionUtils.requestInstallPackagePermission(this);
            } else {
                hasInstallPackagePermission = true;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
