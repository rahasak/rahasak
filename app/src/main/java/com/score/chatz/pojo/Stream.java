package com.score.chatz.pojo;

/**
 * Created by eranga on 9/18/16.
 */
public class Stream {
    private boolean isActive;
    private String user;
    private StringBuffer streamBuffer;

    public Stream(boolean isActive, String user, StringBuffer streamBuffer) {
        this.isActive = isActive;
        this.user = user;
        this.streamBuffer = streamBuffer;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void appendStream(String data) {
        this.streamBuffer.append(data);
    }

    public String getStream() {
        return this.streamBuffer.toString();
    }

}
