package com.score.chatz.interfaces;

import android.content.Context;

import com.score.chatz.db.SenzorsDbSource;
import com.score.senz.ISenzService;
import com.score.senzc.pojos.Senz;

/**
 * Created by lakmal.caldera on 9/16/2016.
 */
public interface IDataPermissionSenzHandler {
    void onNewPermission(Senz senz, ISenzService senzService, SenzorsDbSource dbSource, Context context);
    void onPermissionSharingDone(Senz senz, ISenzService senzService, SenzorsDbSource dbSource, Context context);
}
