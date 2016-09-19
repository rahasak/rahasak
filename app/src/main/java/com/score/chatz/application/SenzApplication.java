package com.score.chatz.application;

import android.app.Application;

import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.utils.PreferenceUtils;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

/**
 * Application class to hold shared attributes
 *
 * @author erangaeb@gmail.com (eranga herath)
 */
public class SenzApplication extends Application {

    private Senz senz;

    private User currentUser;

    private User activeFriend;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public Senz getSenz() {
        return senz;
    }

    public void setSenz(Senz senz) {
        this.senz = senz;
    }

    public void setCurrentUser(){
        try{
            currentUser = PreferenceUtils.getUser(getApplicationContext());
        }catch(NoUserException ex){
            ex.printStackTrace();
        }
    }

    public void setActiveFriend(User friend){
        activeFriend = friend;
    }

    public User getActiveFriend(){
        return activeFriend;
    }

}
