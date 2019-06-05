package com.transsion.appupgrade;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by shanshan.yang on 2018/8/17.
 */

public class UpgradeService extends Service {
    private static String TAG = "UpgradeService";
    private IUpgradeRequestInterface mCallback;
    private Handler mRequestHandler;
    private Handler mHandler;
    private Map<String, Integer> mUpdateInfos = new HashMap<>();
    private String mPath;
    private static final int MODULE_UPLOAD = 20000;//发送上传文件请求
    private static final int MODULE_UPGRADE = 20001;//请求升级
    public static final int SERVER_ACTION = 10000;//服务器下发的消息指令
    public static final int SERVER_UPGRADE = 10001;//服务器下发的升级通知
    public static final int SERVER_UPGRADE_FILE = 10002;//AiCenter通知更新下载完成
    public static final String KEY_VERSION = "localversion";
    public final String TYPE_INSTALL_PACKAGE = "apk";
    private List<String> requestList = new ArrayList<String>();
    private static Context mContext;
    private static int mVersionCode;
    private static String mVersionName;
    private static String mFileName;
    private static ProgressDialog mProgressDialog;
    private static String mLocalFilepath;
    private static boolean hasCancel = false;
    // 如果是自动更新，那么就不跳出已经是最新版本的对话框。否则用户点击更新应用按钮，弹出已经是最新版本的提示框
    private static boolean mIsAuto = false;
    private static boolean mShowProgressDialog = false;
    private static final int NOTIFY_ID = 54;
    private static NotificationManager mNotificationManager;
    private static Notification mNotification;
    private String channelId = "notifyChanelId";
    NotificationChannel channel;
    private CharSequence chanelName = "CleanerChanelName";

    @Override
    public void onCreate() {
        LogUtils.d(  "onCreate()");
        super.onCreate();
        mHandler = new Handler();
        SharedPreferences sp = getSharedPreferences("appupgrade_sp", Context.MODE_PRIVATE);
        boolean isFirst = sp.getBoolean("isFirst", true);
        LogUtils.d(  "onCreate()  isFirst=" + isFirst);
        if (isFirst) {
            initUpdateInfo();
            sp.edit().putBoolean("isFirst", false).apply();
        }
        mContext = getApplicationContext();
        mNotificationManager = (NotificationManager) mContext
                .getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        channel = new NotificationChannel(channelId,
                chanelName, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setSound(sound, null);
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{200});
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d("onStartCommand()");
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public IBinder onBind(Intent intent) {
        LogUtils.d("onBind()");
        return stub;
    }

    private final IBinder stub = new IUpgradeSendInterface.Stub() {
        private boolean needUpgrade;

        @Override
        public void registerCallback(IUpgradeRequestInterface callback) throws RemoteException {
            LogUtils.d(  "registerCallback() callback=" + callback);
            if (callback != null) {
                mCallback = callback;
                mThread = new UpgradeService.MyThread();
                mThread.start();
                Message msg = new Message();
                msg.what = MODULE_UPGRADE;
            }

        }

        public void unregisterCallback(IUpgradeRequestInterface callback) throws RemoteException {
            LogUtils.d(  "unregisterCallback() callback=" + callback);
            if (mCallback == callback) {
                mCallback = null;
            }
        }

        @Override
        public String sendCommand(String command) throws RemoteException {
            try {
                LogUtils.d("sendCommand() command=" + command);
                if (command != null) {
                    Gson gson = new Gson();
                    CommandInfo cmdInfo = gson.fromJson(command, CommandInfo.class);
                    int version = cmdInfo.getVersion();
                    LogUtils.d("sendCommand() version=" + version);
                    int cmd = cmdInfo.getCmd();
                    String type = cmdInfo.getType();
                    if (cmd == SERVER_UPGRADE) {// 接收推送更新通知，执行更新升级
                        needUpgrade = checkUpgrade(type, version);
                    } else if (cmd == SERVER_UPGRADE_FILE) {// AiCenter通知更新下载完成
                        if (needUpgrade) {
                            String updateUrl = cmdInfo.getPath();
                            boolean copySuccess = copyFile(type, updateUrl, mPath);
                            if (copySuccess) {
                                LogUtils.d(  "sendCommand() updateUrl=" + updateUrl);
                                try {
                                    sendNotification(mPath);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {
                LogUtils.e(  "sendCommand() Exception:" + e.toString());
            }
            return generateResult(true);
        }
    };

    @Override
    public boolean onUnbind(Intent intent) {
        LogUtils.d( "onUnBind()");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        LogUtils.d(  "onDestroy");
        mHandler.removeCallbacks(mCountdownRunnable);
        super.onDestroy();
    }

    private String generateResult(boolean success) {
        LogUtils.d(  "generateResult");
        JSONObject json = new JSONObject();
        try {
            if (success) {
                json.put("code", 200);
                json.put("status", "ok");
                json.put("info", "");
            } else {
                json.put("code", 500);
                json.put("status", "fail");
                json.put("info", "");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    public boolean checkUpgrade(String type, int newVersion) {
        LogUtils.d(  "checkUpgrade() type=" + type + ",newVersionCode=" + newVersion);
        PackageManager pm = getPackageManager();
        SharedPreferences sp = getSharedPreferences("appupgrade_sp", Context.MODE_PRIVATE);
        try {
            List<PackageInfo> packageInfos = pm.getInstalledPackages(0);
            for (PackageInfo packageInfo : packageInfos) {//todo 語句耗費很大內存， 待修改
                String pkgName = packageInfo.packageName;
                //检查是否install the type package. or return false
                if (TextUtils.equals(pkgName, type)) {
                    mPath = getApplicationContext().getExternalCacheDir() + "/" + pkgName;
                    LogUtils.d( "checkUpgrade() mPath="+mPath);
                    int currentVersion = sp.getInt(type, -1);
                    if (currentVersion < newVersion) {
                        if (postInstall(type, newVersion, currentVersion))
                            return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        }
        return false;
    }

    private boolean postInstall(String type, int newVersion, int currentVersion) {
        if (mRequestHandler != null) {
            Message msg = new Message();
            msg.obj = buildRequestUpgradeCmd(currentVersion);
            msg.what = MODULE_UPGRADE;//检查需要进行升级，发送下载更新请求
            mRequestHandler.sendMessage(msg);
            return true;
        }
        mUpdateInfos.put(type, newVersion);
        return false;
    }

    private void initUpdateInfo() {
        LogUtils.d(  "initUpdateInfo()");
        PackageManager pm = getPackageManager();
        try {
            List<PackageInfo> packageInfos = pm.getInstalledPackages(0);
            for (PackageInfo packageInfo : packageInfos) {
                String pkgName = packageInfo.packageName;
                LogUtils.d(  "initUpdateInfo() pkgName=" + pkgName);
                mUpdateInfos.put(pkgName, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String buildRequestUpgradeCmd(int version) {
        LogUtils.d(  "buildRequestUpgradeCmd() version=" + version);
        JSONObject json = new JSONObject();
        try {
            json.put("cmd", MODULE_UPGRADE);
            json.put(KEY_VERSION, version);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    private String buildRequestUploadCmd(String path) {
        JSONObject json = new JSONObject();
        try {
            json.put("cmd", MODULE_UPLOAD);//发送上传文件请求
            json.put("filePath", path);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    String mZipName;
    String mZipParentPath;

    private boolean copyFile(String pkgName, String srcPath, String destPath) {
        LogUtils.d(  "copyFile() srcPath=" + srcPath + ",destPath=" + destPath);
        try {
            ZipInputStream zipInputStream = new ZipInputStream(new CheckedInputStream(new FileInputStream(srcPath), new CRC32()));
            ZipEntry zipEntry = null;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                File f = new File(destPath);
                if (zipEntry.getName().endsWith("/")) {
                    LogUtils.d(  "==============copyFile()11111===========" + zipEntry.getName());
                    continue;
                } else {
                    LogUtils.d(  "==============copyFile()22222===========");
                    if (!f.exists()) {
                        LogUtils.d(  "==============copyFile()33333===========");
                        f.createNewFile();
                    }
                }
                FileOutputStream fileOutputStream = new FileOutputStream(f);
                byte[] buf = new byte[1024];
                int len = -1;
                while ((len = zipInputStream.read(buf)) != -1) {
                    fileOutputStream.write(buf, 0, len);
                }
                fileOutputStream.flush();
                fileOutputStream.close();
                zipInputStream.closeEntry();
                String zip = zipEntry.getName();
                LogUtils.d(  "==============copyFile()===========zip=" + zip);
                mZipName = f.getName();
                long size = f.length();
                LogUtils.d(  "==============copyFile()===========size=" + size);
                mZipParentPath = f.getAbsolutePath();
                LogUtils.d(  "==============copyFile()===========zipName=" + mZipName);
                LogUtils.d(  "==============copyFile()===========mZipParentPath=" + mZipParentPath);
            }
            zipInputStream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void sendNotification(String localPath) {
        LogUtils.d( "==========sendNotification()=========localPath="+localPath);
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        if (BuildUtils.isAtLeast24Api()) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri contentUri = FileProvider.getUriForFile(mContext, "com.transsion.appupgrade.fileprovider", new File(localPath));
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            LogUtils.d( "==========sendNotification()=========contentUri="+contentUri);
            List<ResolveInfo> resolveInfos = mContext.getPackageManager().queryIntentActivities(
                    intent, PackageManager.MATCH_DEFAULT_ONLY);
            // Grant permissions for any app that can handle a file to access it
            if (resolveInfos != null && resolveInfos.size() > 0) {
                for (ResolveInfo resolveInfo : resolveInfos) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    mContext.grantUriPermission(packageName, contentUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }
        } else {
            intent.setDataAndType(Uri.fromFile(new File(localPath)), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(
                mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        int icon = R.drawable.ic_launcher_background;
        CharSequence tickerText = "下载完成";
        CharSequence contentText = "文件已下载完毕，点击进行安装。";
        long when = System.currentTimeMillis();

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager.createNotificationChannel(channel);
            builder = new Notification.Builder(mContext, channelId)
                    .setSmallIcon(icon)
                    .setTicker(tickerText)
                    .setContentText(contentText)
                    .setWhen(when)
                    .setContentIntent(contentIntent);
        } else {
            builder = new Notification.Builder(mContext)
                    .setSmallIcon(icon)
                    .setTicker(tickerText)
                    .setContentText(contentText)
                    .setWhen(when)
                    .setContentIntent(contentIntent);
        }
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.defaults |= Notification.DEFAULT_SOUND;
        mNotificationManager.notify(NOTIFY_ID, notification);
    }


    private MyThread mThread;

    class MyThread extends Thread {
        @Override
        public void run() {
            super.run();
            Looper.prepare();
            mRequestHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    switch (msg.what) {
                        case MODULE_UPGRADE:// 请求下载更新
                            if (mCallback != null) {
                                try {
                                    String command = (String) msg.obj;
                                    if (!isSuccess(mCallback.requestCommand(command))) {
                                        requestList.add(command);
                                    }
                                } catch (RemoteException e1) {
                                    e1.printStackTrace();
                                }
                            }
                            break;
                        default:
                            break;
                    }
                }
            };
            Looper.loop();
        }
    };

    private boolean isSuccess(String result) {
        LogUtils.d(  "isSuccess " + result);
        try {
            JSONObject json = new JSONObject(result);
            if (json.getInt("code") == 200) {
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void maybeStopService() {
        LogUtils.d(  "maybeStopService");
        if (requestList.isEmpty()) {
            LogUtils.d(  "stopSelf");
            stopSelf();
        }
    }

    private final Runnable mCountdownRunnable = new Runnable() {
        public void run() {
            maybeStopService();
        }
    };



    /**
     * 初始化
     *
     * @param context
     *            执行上下文
     * @param isauto
     *            指示是否是自动升级，当版本号没变时，true无响应，false弹出已是最新版的对话框。
     * @param showProgressDialog
     *            true弹出下载进度对话框，false为通知消息提示进度
     */
    public static void init(Context context, boolean isauto,
                            boolean showProgressDialog) {
        mIsAuto = isauto;
        mShowProgressDialog = showProgressDialog;
        mContext = context;
        mNotificationManager = (NotificationManager) context
                .getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        /*mVersionCode = getVerCode(mContext);
        mVersionName = getVerName(mContext);
        mAppVersion = newAv;
        mFileName = mAppVersion.getApkName();*/
        File sdDir = null;

        // 判断sd卡是否存在
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            // 获取根目录
            sdDir = Environment.getExternalStorageDirectory();
        }

        // 注意FileOutputStream不会自动创建路径，所以初始化的时候要主动创建路径。
        String dirpath;
        if (sdDir != null) {
            // AppName为你想保存的路径，一般为应用目录
            dirpath = sdDir.toString() + "/AppName/";
        } else {
            dirpath = "/AppName/";
        }
        File dir = new File(dirpath);
        if (!dir.exists()) {
            dir.mkdir();// 如果路径不存在就先创建路径
        }
        mLocalFilepath = dirpath + mFileName;
    }
}

