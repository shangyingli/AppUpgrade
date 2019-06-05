package com.transsion.appupgrade;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * Created by shanshan.yang on 2018/9/11.
 */

public class CommandInfo implements Parcelable {
    @SerializedName("cmd")
    private int mCmd;
    @SerializedName("version")
    private int mVersion;//apk版本號
    @SerializedName("path")
    private String mPath;//aicenter保存文件的路徑
    @SerializedName("type")
    private String mType;//要升級的引用包名

    public CommandInfo() {
    }

    public CommandInfo(int cmd, String path, int version, String type){
        this.mCmd = cmd;
        this.mVersion = version;
        this.mPath = path;
        this.mType = type;
    }

    public int getCmd() {
        return mCmd;
    }

    public void setCmd(int cmd) {
        this.mCmd = cmd;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        this.mPath = path;
    }

    public int getVersion() {
        return mVersion;
    }

    public void setVersion(int version) {
        this.mVersion = version;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        this.mType = type;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(getCmd());
        out.writeString(getPath());
        out.writeInt(getVersion());
        out.writeString(getType());
    }

    public static final Parcelable.Creator<CommandInfo> CREATOR = new Parcelable.Creator<CommandInfo>() {
        @Override
        public CommandInfo[] newArray(int size) {
            return new CommandInfo[size];
        }

        @Override
        public CommandInfo createFromParcel(Parcel in) {
            CommandInfo cmdInfo = new CommandInfo();
            cmdInfo.setCmd(in.readInt());
            cmdInfo.setVersion(in.readInt());
            cmdInfo.setPath(in.readString());
            cmdInfo.setType(in.readString());
            return cmdInfo;
        }
    };

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.getClass().getSimpleName());
        builder.append("(");
        builder.append("cmd:").append(getCmd()).append(", ");
        builder.append("version:").append(getVersion()).append(", ");
        builder.append("updateUrl:").append(getVersion()).append(", ");
        builder.append("type:").append(getType()).append(", ");
        builder.append(")");
        return builder.toString();
    }
}