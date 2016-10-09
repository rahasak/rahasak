package com.score.chatz.db;

import android.provider.BaseColumns;

/**
 * Keep database table attributes
 *
 * @author erangaeb@gmail.com (eranga herath)
 */
public class SenzorsDbContract {

    public SenzorsDbContract() {
    }

    /* Inner class that defines secret table */
    public static abstract class Secret implements BaseColumns {
        public static final String TABLE_NAME = "secret";
        public static final String COLUMN_UNIQUE_ID = "uid";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_USER = "user";
        public static final String COLUMN_NAME_IS_SENDER = "is_sender";
        public static final String COLUMN_BLOB_TYPE = "type";
        public static final String COLUMN_NAME_BLOB = "blob";
        public static final String COLUMN_NAME_VIEWED = "viewed";
        public static final String COLUMN_NAME_MISSED = "missed";
        public static final String COLUMN_NAME_DELIVERED = "delivered";
        public static final String COLUMN_NAME_DISPATCHED = "dispatched";
    }

    /* Inner class that defines secret user mapping table */
    public static abstract class LatestChat implements BaseColumns {
        public static final String TABLE_NAME = "latest_chat";
        public static final String COLUMN_USER = "user";
        public static final String COLUMN_BLOB = "blob";
        public static final String COLUMN_TYPE = "type";
        public static final String COLUMN_NAME_IS_SENDER = "is_sender";
        public static final String COLUMN_TIMESTAMP = "timestamp";
    }

    /* Inner class that defines permission control for the user
     * Add more permissions here in the future */
    public static abstract class Permission implements BaseColumns {
        public static final String TABLE_NAME = "permission";
        public static final String COLUMN_NAME_LOCATION = "location";
        public static final String COLUMN_NAME_CAMERA = "camera";
        public static final String COLUMN_NAME_MIC = "mic";
        public static final String COLUMN_NAME_USER = "user";
    }

    /* Inner class that defines permission control for the user
     * Add more permissions here in the future */
    public static abstract class PermissionConfiguration implements BaseColumns {
        public static final String TABLE_NAME = "permission_config";
        public static final String COLUMN_UNIQUE_ID = "uid";
        public static final String COLUMN_NAME_LOCATION = "location";
        public static final String COLUMN_NAME_CAMERA = "camera";
        public static final String COLUMN_NAME_MIC = "mic";
        public static final String COLOMN_NAME_USER = "user";
    }

    /* Inner class that defines the user table contents */
    public static abstract class User implements BaseColumns {
        public static final String TABLE_NAME = "user";
        public static final String COLUMN_NAME_USERNAME = "username";
        public static final String COLOMN_NAME_IMAGE = "image";
    }
}
