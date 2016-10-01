package com.score.chatz.pojo;

import com.score.chatz.enums.NotificationType;

public class SenzNotification {

    int icon;
    String title;
    String message;
    String sender;
    NotificationType notificationType;

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

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }
}
