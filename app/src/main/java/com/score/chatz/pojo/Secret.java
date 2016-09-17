package com.score.chatz.pojo;

/**
 * Created by Lakmal on 7/31/16.
 */

import com.score.senzc.pojos.User;
public class Secret {
    private String blob;
    private User who;
    private User receiver;
    private String type;
    private boolean isDelete;
    private boolean isDelivered;
    private Long timeStamp;
    private Long seenTimeStamp;
    private String id;
    private boolean isDeliveryFailed;

    public Secret(String blob, String type, User who) {
        this.blob = blob;
        this.who = who;
        this.type = type;
    }

    public String getBlob() {
        return blob;
    }

    public User getWho() {
        return who;
    }

    public String getType(){
        return type;
    }

    public void setReceiver(User rec){
        receiver = rec;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setDelete(boolean val){
        isDelete = val;
    }

    public void setDeliveryFailed(boolean val){
        isDeliveryFailed = val;
    }

    public void setIsDelivered(boolean val){
        isDelivered = val;
    }

    public void setTimeStamp(Long ts){
        timeStamp = ts;
    }

    public void setSeenTimeStamp(Long sts){
        seenTimeStamp = sts;
    }

    public boolean isDelivered(){
        return isDelivered;
    }

    public boolean isDeliveryFailed(){
        return isDeliveryFailed;
    }

    public Long getTimeStamp(){
        return timeStamp;
    }

    public Long getSeenTimeStamp(){
        return seenTimeStamp;
    }

    public void setID(String val){
        id = val;
    }

    public String getID(){
        return id;
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
