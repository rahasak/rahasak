package com.score.chatz.pojo;

/**
 * Created by Lakmal on 7/31/16.
 */

import com.score.senzc.pojos.User;

public class Secret {
    private String blob;
    private User user;
    private User receiver;
    private String type;
    private boolean isViewed;
    private boolean isDelivered;
    private Long timeStamp;
    private Long seenTimeStamp;
    private String id;
    private boolean isDeliveryFailed;
    private boolean isSender;

    public Secret(String blob, String type, User user, boolean isSender) {
        this.blob = blob;
        this.user = user;
        this.type = type;
        this.isSender = isSender;
    }

    public String getBlob() {
        return blob;
    }

    public void setBlob(String blob) {
        this.blob = blob;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isViewed() {
        return isViewed;
    }

    public void setViewed(boolean viewed) {
        isViewed = viewed;
    }

    public boolean isDelivered() {
        return isDelivered;
    }

    public void setDelivered(boolean delivered) {
        isDelivered = delivered;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public Long getSeenTimeStamp() {
        return seenTimeStamp;
    }

    public void setSeenTimeStamp(Long seenTimeStamp) {
        this.seenTimeStamp = seenTimeStamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isDeliveryFailed() {
        return isDeliveryFailed;
    }

    public void setDeliveryFailed(boolean deliveryFailed) {
        isDeliveryFailed = deliveryFailed;
    }

    public boolean isSender() {
        return isSender;
    }

    public void setSender(boolean sender) {
        isSender = sender;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Secret)) {
            return false;
        }
        Secret that = (Secret) other;
        return this.id.equalsIgnoreCase(that.id);
    }

}
