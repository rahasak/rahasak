package com.score.chatz.pojo;

/**
 * Created by eranga on 11/14/16.
 */

public class Permission {
    String id;
    boolean loc;
    boolean cam;
    boolean mic;
    boolean isGiven;

    public Permission(String id, boolean isGiven) {
        this.id = id;
        this.isGiven = isGiven;
    }

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

    public boolean isMic() {
        return mic;
    }

    public void setMic(boolean mic) {
        this.mic = mic;
    }

    public boolean isGiven() {
        return isGiven;
    }

    public void setGiven(boolean given) {
        isGiven = given;
    }
}
