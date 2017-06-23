package com.score.rahasak.pojo;

import com.score.rahasak.enums.BlobType;
import com.score.rahasak.enums.DeliveryState;

public class Secret {
    private String id;
    private String blob;
    private BlobType blobType;
    private SecretUser user;
    private boolean isSender;
    private boolean isViewed;
    private boolean isSelected;
    private Long timeStamp;
    private Long viewedTimeStamp;
    private boolean isMissed;
    private boolean inOrder;
    private DeliveryState deliveryState;

    public Secret(String blob, BlobType blobType, SecretUser user, boolean isSender) {
        this.blob = blob;
        this.blobType = blobType;
        this.user = user;
        this.isSender = isSender;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBlob() {
        return blob;
    }

    public void setBlob(String blob) {
        this.blob = blob;
    }

    public BlobType getBlobType() {
        return blobType;
    }

    public void setBlobType(BlobType blobType) {
        this.blobType = blobType;
    }

    public SecretUser getUser() {
        return user;
    }

    public boolean isSender() {
        return isSender;
    }

    public void setSender(boolean sender) {
        isSender = sender;
    }

    public void setUser(SecretUser user) {
        this.user = user;
    }

    public boolean isMissed() {
        return isMissed;
    }

    public void setMissed(boolean missed) {
        isMissed = missed;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public Long getViewedTimeStamp() {
        return viewedTimeStamp;
    }

    public void setViewedTimeStamp(Long viewedTimeStamp) {
        this.viewedTimeStamp = viewedTimeStamp;
    }

    public boolean isViewed() {
        return isViewed;
    }

    public void setViewed(boolean viewed) {
        isViewed = viewed;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isInOrder() {
        return inOrder;
    }

    public void setInOrder(boolean inOrder) {
        this.inOrder = inOrder;
    }

    public DeliveryState getDeliveryState() {
        return deliveryState;
    }

    public void setDeliveryState(DeliveryState deliveryState) {
        this.deliveryState = deliveryState;
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
