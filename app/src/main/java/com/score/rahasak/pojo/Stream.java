package com.score.rahasak.pojo;

/**
 * Created by eranga on 9/18/16.
 */
public class Stream {
    private String user;
    private StringBuffer streamBuffer;

    public Stream(String user) {
        this.user = user;
        this.streamBuffer = new StringBuffer();
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
