package com.score.chatz.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Perform creating tables here
 *
 * @author erangaeb@gmail.com(eranga herath)
 */
class SenzorsDbHelper extends SQLiteOpenHelper {

    private static final String TAG = SenzorsDbHelper.class.getName();

    // we use singleton database
    private static SenzorsDbHelper senzorsDbHelper;

    // If you change the database schema, you must increment the database version
    private static final int DATABASE_VERSION = 29;
    private static final String DATABASE_NAME = "Rahasak.db";

    // data types, keywords and queries
    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";

    private static final String SQL_CREATE_SECRET =
            "CREATE TABLE " + SenzorsDbContract.Secret.TABLE_NAME + " (" +
                    SenzorsDbContract.Secret._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + ", " +
                    SenzorsDbContract.Secret.COLUMN_UNIQUE_ID + TEXT_TYPE + ", " +
                    SenzorsDbContract.Secret.COLUMN_TIMESTAMP + INT_TYPE + ", " +
                    SenzorsDbContract.Secret.COLUMN_NAME_USER + TEXT_TYPE + ", " +
                    SenzorsDbContract.Secret.COLUMN_NAME_IS_SENDER + INT_TYPE + ", " +
                    SenzorsDbContract.Secret.COLUMN_BLOB_TYPE + TEXT_TYPE + ", " +
                    SenzorsDbContract.Secret.COLUMN_NAME_BLOB + TEXT_TYPE + ", " +
                    SenzorsDbContract.Secret.COLUMN_NAME_VIEWED + INT_TYPE + ", " +
                    SenzorsDbContract.Secret.COLUMN_NAME_VIEWED_TIMESTAMP + INT_TYPE + ", " +
                    SenzorsDbContract.Secret.COLUMN_NAME_MISSED + INT_TYPE + ", " +
                    SenzorsDbContract.Secret.COLUMN_NAME_DELIVERED + INT_TYPE + ", " +
                    SenzorsDbContract.Secret.COLUMN_NAME_DISPATCHED + INT_TYPE +
                    " )";

    private static final String SQL_CREATE_USER =
            "CREATE TABLE " + SenzorsDbContract.User.TABLE_NAME + " (" +
                    SenzorsDbContract.User._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + "," +
                    SenzorsDbContract.User.COLUMN_NAME_USERNAME + TEXT_TYPE + " UNIQUE NOT NULL" + "," +
                    SenzorsDbContract.User.COLUMN_UNIQUE_ID + TEXT_TYPE + ", " +
                    SenzorsDbContract.User.COLUMN_NAME_PHONE + TEXT_TYPE + "," +
                    SenzorsDbContract.User.COLUMN_NAME_PUBKEY + TEXT_TYPE + "," +
                    SenzorsDbContract.User.COLUMN_NAME_PUBKEY_HASH + TEXT_TYPE + "," +
                    SenzorsDbContract.User.COLUMN_NAME_IS_ACTIVE + INT_TYPE + "," +
                    SenzorsDbContract.User.COLUMN_NAME_IMAGE + TEXT_TYPE + "," +
                    SenzorsDbContract.User.COLUMN_NAME_GIVEN_PERM + INT_TYPE + "," +
                    SenzorsDbContract.User.COLUMN_NAME_RECV_PERM + INT_TYPE +
                    " )";

    private static final String SQL_CREATE_PERMISSION =
            "CREATE TABLE " + SenzorsDbContract.Permission.TABLE_NAME + " (" +
                    SenzorsDbContract.Permission._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + ", " +
                    SenzorsDbContract.Permission.COLUMN_NAME_CAMERA + INT_TYPE + ", " +
                    SenzorsDbContract.Permission.COLUMN_NAME_LOCATION + INT_TYPE + ", " +
                    SenzorsDbContract.Permission.COLUMN_NAME_MIC + INT_TYPE + ", " +
                    SenzorsDbContract.Permission.COLUMN_NAME_IS_GIVEN + INT_TYPE +
                    " )";

    private static final String SQL_DELETE_USER =
            "DROP TABLE IF EXISTS " + SenzorsDbContract.User.TABLE_NAME;
    private static final String SQL_DELETE_SECRET =
            "DROP TABLE IF EXISTS " + SenzorsDbContract.Secret.TABLE_NAME;
    private static final String SQL_DELETE_PERMISSION =
            "DROP TABLE IF EXISTS " + SenzorsDbContract.Permission.TABLE_NAME;

    /**
     * Init context
     * Init database
     *
     * @param context application context
     */
    public SenzorsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * We are reusing one database instance in all over the app for better memory usage
     *
     * @param context application context
     * @return db helper instance
     */
    synchronized static SenzorsDbHelper getInstance(Context context) {
        if (senzorsDbHelper == null) {
            senzorsDbHelper = new SenzorsDbHelper(context.getApplicationContext());
        }
        return (senzorsDbHelper);
    }

    /**
     * {@inheritDoc}
     */
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "OnCreate: creating db helper, db version - " + DATABASE_VERSION);
        Log.d(TAG, SQL_CREATE_USER);
        Log.d(TAG, SQL_CREATE_SECRET);
        Log.d(TAG, SQL_CREATE_PERMISSION);

        db.execSQL(SQL_CREATE_SECRET);
        db.execSQL(SQL_CREATE_USER);
        db.execSQL(SQL_CREATE_PERMISSION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // enable foreign key constraint here
        Log.d(TAG, "OnConfigure: Enable foreign key constraint");
        db.setForeignKeyConstraintsEnabled(true);
    }

    /**
     * {@inheritDoc}
     */
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        Log.d(TAG, "OnUpgrade: updating db helper, db version - " + DATABASE_VERSION);
        db.execSQL(SQL_DELETE_USER);
        db.execSQL(SQL_DELETE_SECRET);
        db.execSQL(SQL_DELETE_PERMISSION);

        onCreate(db);
    }

    /**
     * {@inheritDoc}
     */
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

}
