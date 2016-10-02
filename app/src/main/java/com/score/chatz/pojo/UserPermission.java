package com.score.chatz.pojo;

import com.score.senzc.pojos.User;

/**
 * Created by Lakmal on 8/6/16.
 */
public class UserPermission {
    private User user;
    private boolean cameraPermission;
    private boolean locationPermission;
    private boolean micPermission;


    public UserPermission(User user, boolean cameraPermission, boolean locationPermission,  boolean micPermission) {
        this.user = user;
        this.cameraPermission = cameraPermission;
        this.locationPermission = locationPermission;
        this.micPermission = micPermission;
    }

    public User getUser() {
        return user;
    }

    public boolean getCamPerm() {
        return cameraPermission;
    }

    public boolean getLocPerm() {
        return locationPermission;
    }

    public boolean getMicPerm() {
        return micPermission;
    }

}
