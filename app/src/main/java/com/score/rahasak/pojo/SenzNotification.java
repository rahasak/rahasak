package com.score.rahasak.pojo;

import com.score.rahasak.enums.NotificationType;

public class SenzNotification {

    private int icon;
    private String title;
    private String message;
    private String sender;
    private String senderPhone;
    private NotificationType notificationType;

    public SenzNotification(int icon, String title, String message, String sender, NotificationType notificationType) {
        this.icon = icon;
        this.title = title;
        this.message = message;
        this.sender = sender;
        this.notificationType = notificationType;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public void setSenderPhone(String senderPhone) {
        this.senderPhone = senderPhone;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }
}
