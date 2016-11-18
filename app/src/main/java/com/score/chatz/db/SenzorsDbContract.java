package com.score.chatz.db;

import android.provider.BaseColumns;

/**
 * Keep database table attributes
 *
 * @author erangaeb@gmail.com (eranga herath)
 */
class SenzorsDbContract {

    public SenzorsDbContract() {
    }

    /* Inner class that defines secret table */
    static abstract class Secret implements BaseColumns {
        static final String TABLE_NAME = "secret";
        static final String COLUMN_UNIQUE_ID = "uid";
        static final String COLUMN_TIMESTAMP = "timestamp";
        static final String COLUMN_NAME_USER = "user";
        // TODO change this to my_secret
        static final String COLUMN_NAME_IS_SENDER = "is_sender";
        static final String COLUMN_NAME_BLOB = "blob";
        static final String COLUMN_BLOB_TYPE = "blob_type";
        static final String COLUMN_NAME_VIEWED = "viewed";
        static final String COLUMN_NAME_VIEWED_TIMESTAMP = "view_timestamp";
        static final String COLUMN_NAME_MISSED = "missed";
        static final String DELIVERY_STATE = "delivery_state";
    }

    /* Inner class that defines the user table contents */
    public static abstract class User implements BaseColumns {
        static final String TABLE_NAME = "user";
        static final String COLUMN_UNIQUE_ID = "uid";
        static final String COLUMN_NAME_USERNAME = "username";
        static final String COLUMN_NAME_PHONE = "phone";
        static final String COLUMN_NAME_PUBKEY = "pubkey";
        static final String COLUMN_NAME_PUBKEY_HASH = "pubkey_hash";
        static final String COLUMN_NAME_IS_ACTIVE = "is_active";
        static final String COLUMN_NAME_IMAGE = "image";
        static final String COLUMN_NAME_GIVEN_PERM = "given_perm";
        static final String COLUMN_NAME_RECV_PERM = "recv_perm";
    }

    /* Inner class that defines permission control for the user
     * Add more permissions here in the future */
    public static abstract class Permission implements BaseColumns {
        static final String TABLE_NAME = "permission";
        static final String COLUMN_NAME_LOCATION = "loc";
        static final String COLUMN_NAME_CAMERA = "cam";
        static final String COLUMN_NAME_MIC = "mic";
        // is_given = true -> I have given this permission to other party
        // is_given = false -> Other party has given this permission to me
        // TODO change this to my_perm
        static final String COLUMN_NAME_IS_GIVEN = "is_given";
    }

}
