package com.score.chatz.pojo;

import android.os.Parcelable;

import com.score.senzc.pojos.User;

/**
 * Created by eranga on 11/11/16.
 */

public class SecretUser extends User implements Parcelable {
    String phone;
    String image;
    String pubKey;
    String pubKeyHash;
    boolean isActive;
    Permission givenPermission;
    Permission recvPermission;

    public SecretUser(String id, String username) {
        super(id, username);
    }

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

}
