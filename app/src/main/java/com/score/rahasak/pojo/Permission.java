package com.score.rahasak.pojo;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by eranga on 11/14/16.
 */
public class Permission implements Parcelable {
    private String id;
    private boolean loc;
    private boolean cam;
    private boolean isGiven;

    public Permission(String id, boolean isGiven) {
        this.id = id;
        this.isGiven = isGiven;
    }

    protected Permission(Parcel in) {
        id = in.readString();
        loc = in.readByte() != 0;
        cam = in.readByte() != 0;
        isGiven = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeByte((byte) (loc ? 1 : 0));
        dest.writeByte((byte) (cam ? 1 : 0));
        dest.writeByte((byte) (isGiven ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Permission> CREATOR = new Creator<Permission>() {
        @Override
        public Permission createFromParcel(Parcel in) {
            return new Permission(in);
        }

        @Override
        public Permission[] newArray(int size) {
            return new Permission[size];
        }
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isLoc() {
        return loc;
    }

    public void setLoc(boolean loc) {
        this.loc = loc;
    }

    public boolean isCam() {
        return cam;
    }

    public void setCam(boolean cam) {
        this.cam = cam;
    }

    public boolean isGiven() {
        return isGiven;
    }

    public void setGiven(boolean given) {
        isGiven = given;
    }
}
