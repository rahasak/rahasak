package com.score.chatz.pojo;

/**
 * Created by eranga on 11/11/16.
 */

public class SecretUser {
    String username;
    String phone;
    String pubKey;
    String pubKeyHash;
    String image;
    boolean isActive;

    public SecretUser(String username, String phone, String pubKey, String pubKeyHash, String image, boolean isActive) {
        this.username = username;
        this.phone = phone;
        this.pubKey = pubKey;
        this.pubKeyHash = pubKeyHash;
        this.image = image;
        this.isActive = isActive;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
