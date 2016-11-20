package com.score.chatz.pojo;

import android.os.Parcel;
import android.os.Parcelable;

import com.score.senzc.pojos.User;

/**
 * Created by eranga on 11/11/16
 */
public class SecretUser extends User implements Parcelable {
    private String phone;
    private String image;
    private String pubKey;
    private String pubKeyHash;
    private String sessionKey;
    private Permission givenPermission;
    private Permission recvPermission;
    private boolean isSMSRequester;
    private boolean isActive;

    public SecretUser(String id, String username) {
        super(id, username);
    }

    /**
     * Use when reconstructing User object from parcel
     * This will be used only by the 'CREATOR'
     *
     * @param in a parcel to read this object
     */
    public SecretUser(Parcel in) {
        super(in);
        this.phone = in.readString();
        this.image = in.readString();
        this.pubKey = in.readString();
        this.pubKeyHash = in.readString();
        this.sessionKey = in.readString();
        this.isActive = in.readByte() != 0;
        this.isSMSRequester = in.readByte() != 0;
        this.givenPermission = in.readParcelable(Permission.class.getClassLoader());
        this.recvPermission = in.readParcelable(Permission.class.getClassLoader());
    }

    /**
     * Define the kind of object that you gonna parcel,
     * You can use hashCode() here
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Actual object serialization happens here, Write object content
     * to parcel one by one, reading should be done according to this write order
     *
     * @param dest  parcel
     * @param flags Additional flags about how the object should be written
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(phone);
        dest.writeString(image);
        dest.writeString(pubKey);
        dest.writeString(pubKeyHash);
        dest.writeString(sessionKey);
        dest.writeByte((byte) (isActive ? 1 : 0));
        dest.writeByte((byte) (isSMSRequester ? 1 : 0));
        dest.writeParcelable(givenPermission, flags);
        dest.writeParcelable(recvPermission, flags);
    }

    /**
     * This field is needed for Android to be able to
     * create new objects, individually or as arrays
     * <p>
     * If you don’t do that, Android framework will through exception
     * Parcelable protocol requires a Parcelable.Creator object called CREATOR
     */
    public static final Creator<SecretUser> CREATOR = new Creator<SecretUser>() {
        public SecretUser createFromParcel(Parcel in) {
            return new SecretUser(in);
        }

        public SecretUser[] newArray(int size) {
            return new SecretUser[size];
        }
    };

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getPubKey() {
        return pubKey;
    }

    public void setPubKey(String pubKey) {
        this.pubKey = pubKey;
    }

    public String getPubKeyHash() {
        return pubKeyHash;
    }

    public void setPubKeyHash(String pubKeyHash) {
        this.pubKeyHash = pubKeyHash;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Permission getGivenPermission() {
        return givenPermission;
    }

    public void setGivenPermission(Permission givenPermission) {
        this.givenPermission = givenPermission;
    }

    public Permission getRecvPermission() {
        return recvPermission;
    }

    public void setRecvPermission(Permission recvPermission) {
        this.recvPermission = recvPermission;
    }

    public boolean isSMSRequester() {
        return isSMSRequester;
    }

    public void setSMSRequester(boolean SMSRequester) {
        isSMSRequester = SMSRequester;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }
}
