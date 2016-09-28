package com.score.chatz.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.score.chatz.pojo.Secret;
import com.score.chatz.pojo.UserPermission;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Do all database insertions, updated, deletions from here
 *
 * @author erangaeb@gmail.com (eranga herath)
 */
public class SenzorsDbSource {

    private static final String TAG = SenzorsDbSource.class.getName();

    private static Context context;

    /**
     * Init db helper
     *
     * @param context application context
     */
    public SenzorsDbSource(Context context) {
        Log.d(TAG, "Init: db source");
        this.context = context;
    }

    /**
     * Get user if exists in the database, other wise create user and return
     *
     * @param username username
     * @return user
     */
    public User getOrCreateUser(String username) {
        Log.d(TAG, "GetOrCreateUser: " + username);

        // get matching user if exists
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();
        Cursor cursor = db.query(SenzorsDbContract.User.TABLE_NAME, // table
                null, SenzorsDbContract.User.COLUMN_NAME_USERNAME + "=?", // constraint
                new String[]{username}, // prams
                null, // order by
                null, // group by
                null); // join

        if (cursor.moveToFirst()) {
            // have matching user
            // so get user data
            // we return id as password since we no storing users password in database
            String _id = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User._ID));
            String _username = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_USERNAME));
            String _image = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLOMN_NAME_IMAGE));

            // clear
            cursor.close();

            Log.d(TAG, "have user, so return it: " + username);
            return new User(_id, _username);
        } else {
            // no matching user
            // so create user
            ContentValues values = new ContentValues();
            values.put(SenzorsDbContract.User.COLUMN_NAME_USERNAME, username);

            // inset data
            long id = db.insert(SenzorsDbContract.User.TABLE_NAME, SenzorsDbContract.User.COLUMN_NAME_USERNAME, values);
            //db.close();

            Log.d(TAG, "no user, so user created: " + username + " " + id);
            return new User(Long.toString(id), username);
        }
    }

    /**
     * Update Location information of user
     *
     * @param user
     * @param value
     */
    public void updateSenz(User user, String value) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // content values to inset
        ContentValues values = new ContentValues();
        values.put(SenzorsDbContract.Location.COLUMN_NAME_VALUE, value);

        // update
        db.update(SenzorsDbContract.Location.TABLE_NAME,
                values,
                SenzorsDbContract.Location.COLUMN_NAME_USER + " = ?",
                new String[]{String.valueOf(user.getId())});
    }

    /**
     * Update permissions given to current user by others/Friends
     *
     * @param user    Other user or friend
     * @param camPerm Camera Permission given to current user by friend
     * @param locPerm Locations Permission given to current user by friend
     */
    public void updatePermissions(User user, String camPerm, String locPerm, String micPerm) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // initial permissions
        int _camPerm = 0;
        int _locPerm = 0;
        int _micPerm = 0;

        // Convert String value of boolean into an integer to be stored in sqlite (Since no support for boolean type)
        // update the inital variables with the new converted values
        if (camPerm != null) {
            _camPerm = camPerm.equalsIgnoreCase("on") ? 1 : 0;
        }
        if (locPerm != null) {
            _locPerm = locPerm.equalsIgnoreCase("on") ? 1 : 0;
        }
        if (micPerm != null) {
            _micPerm = micPerm.equalsIgnoreCase("on") ? 1 : 0;
        }

        // content values to inset
        ContentValues values = new ContentValues();
        if (camPerm != null) {
            values.put(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA, _camPerm);
        }
        if (locPerm != null) {
            values.put(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION, _locPerm);
        }
        if (micPerm != null) {
            values.put(SenzorsDbContract.Permission.COLUMN_NAME_MIC, _micPerm);
        }

        // update
        db.update(SenzorsDbContract.Permission.TABLE_NAME,
                values,
                SenzorsDbContract.Permission.COLOMN_NAME_USER + " = ?",
                new String[]{user.getUsername()});
    }

    /**
     * Update configurable permissions the current user gives to others/Friends
     * Configurable permissiona are used to persist the state of the switch the user can toggle
     *
     * @param user    Other user or friend
     * @param camPerm Camera Permission given by current user to a friend
     * @param locPerm Locations Permission given by current user to a friend
     */
    public void updateConfigurablePermissions(User user, String camPerm, String locPerm, String micPerm) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // intial permissions
        int _camPerm = 0;
        int _locPerm = 0;
        int _micPerm = 0;

        // Convert String value of boolean into an integer to be stored in sqlite (Since no support for boolean type)
        // update the inital variables with the new converted values
        if (camPerm != null) {
            _camPerm = camPerm.equalsIgnoreCase("on") ? 1 : 0;
        }
        if (locPerm != null) {
            _locPerm = locPerm.equalsIgnoreCase("on") ? 1 : 0;
        }
        if (micPerm != null) {
            _micPerm = micPerm.equalsIgnoreCase("on") ? 1 : 0;
        }

        // content values to inset
        ContentValues values = new ContentValues();
        if (camPerm != null) {
            values.put(SenzorsDbContract.PermissionConfiguration.COLUMN_NAME_CAMERA, _camPerm);
        }
        if (locPerm != null) {
            values.put(SenzorsDbContract.PermissionConfiguration.COLUMN_NAME_LOCATION, _locPerm);
        }
        if (micPerm != null) {
            values.put(SenzorsDbContract.Permission.COLUMN_NAME_MIC, _micPerm);
        }

        // update
        db.update(SenzorsDbContract.PermissionConfiguration.TABLE_NAME,
                values,
                SenzorsDbContract.PermissionConfiguration.COLOMN_NAME_USER + " = ?",
                new String[]{user.getUsername()});
    }

    /**
     * Add senz to the database
     *
     * @param senz senz object
     */
    public void createSenz(Senz senz) {
        Log.d(TAG, "AddSensor: adding senz from - " + senz.getSender());
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // content values to inset
        ContentValues values = new ContentValues();
        if (senz.getAttributes().containsKey("lat")) {
            values.put(SenzorsDbContract.Location.COLUMN_NAME_NAME, "Location");
        } else if (senz.getAttributes().containsKey("gpio13") || senz.getAttributes().containsKey("gpio15")) {
            // store json string in db
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("GPIO13", "OFF");
                jsonObject.put("GPIO15", "OFF");

                values.put(SenzorsDbContract.Location.COLUMN_NAME_NAME, "GPIO");
                values.put(SenzorsDbContract.Location.COLUMN_NAME_VALUE, jsonObject.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        values.put(SenzorsDbContract.Location.COLUMN_NAME_USER, senz.getSender().getId());

        // Insert the new row, if fails throw an error
        db.insertOrThrow(SenzorsDbContract.Location.TABLE_NAME, SenzorsDbContract.Location.COLUMN_NAME_NAME, values);
    }

    /**
     * Create Secret message or images
     *
     * @param secret
     */
    public void createSecret(Secret secret) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();
        // content values to inset
        ContentValues values = new ContentValues();
        values.put(SenzorsDbContract.Secret.COLUMN_BLOB_TYPE, secret.getType());
        values.put(SenzorsDbContract.Secret.COLUMN_NAME_BLOB, secret.getBlob());
        values.put(SenzorsDbContract.Secret.COLUMN_NAME_USER, secret.getUser().getUsername());
        values.put(SenzorsDbContract.Secret.COLUMN_NAME_IS_SENDER, secret.isSender() == true ? 1 : 0);
        values.put(SenzorsDbContract.Secret.COLUMN_UNIQUE_ID, secret.getID());
        values.put(SenzorsDbContract.Secret.COLUMN_NAME_DELIVERED, 0);
        values.put(SenzorsDbContract.Secret.COLUMN_NAME_DELIVERY_FAILED, 0);
        values.put(SenzorsDbContract.Secret.COLUMN_NAME_DELETE, 0);
        values.put(SenzorsDbContract.Secret.COLUMN_TIMESTAMP, secret.getTimeStamp());

        // Insert the new row, if fails throw an error
        db.insertOrThrow(SenzorsDbContract.Secret.TABLE_NAME, null, values);

        insertRecordIntoLatestChat(secret);

    }

    private void insertRecordIntoLatestChat(Secret secret) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // content values to inset
        ContentValues values = new ContentValues();
        values.put(SenzorsDbContract.LatestChat.COLUMN_USER, secret.getUser().getUsername());
        values.put(SenzorsDbContract.LatestChat.COLUMN_BLOB, secret.getBlob());
        values.put(SenzorsDbContract.LatestChat.COLUMN_TYPE, secret.getType());
        values.put(SenzorsDbContract.LatestChat.COLUMN_NAME_IS_SENDER, secret.isSender());
        values.put(SenzorsDbContract.LatestChat.COLUMN_TIMESTAMP, secret.getTimeStamp());

        //First update the table
        int rowCount = db.update(SenzorsDbContract.LatestChat.TABLE_NAME,
                values,
                SenzorsDbContract.LatestChat.COLUMN_USER + " =?",
                new String[]{secret.getUser().getUsername()});

        //If not rows were affected!!then insert
        if (rowCount == 0) {
            db.insert(SenzorsDbContract.LatestChat.TABLE_NAME, null, values);
        }
    }

    /**
     * Mark message as delivered
     *
     * @param uid unique identifier of message
     */
    public void markSecretDelievered(String uid) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();
        try {
            db.beginTransaction();

            // content values to inset
            ContentValues values = new ContentValues();
            values.put(SenzorsDbContract.Secret.COLUMN_NAME_DELIVERED, 1);

            // update
            db.update(SenzorsDbContract.Secret.TABLE_NAME,
                    values,
                    SenzorsDbContract.Secret.COLUMN_UNIQUE_ID + " =?",
                    new String[]{uid});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Mark message as delivery Failed
     *
     * @param uid unique identifier of message
     */
    public void markSecretDeliveryFailed(String uid) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();
        try {
            db.beginTransaction();

            // content values to inset
            ContentValues values = new ContentValues();
            values.put(SenzorsDbContract.Secret.COLUMN_NAME_DELIVERY_FAILED, 1);

            // update
            db.update(SenzorsDbContract.Secret.TABLE_NAME,
                    values,
                    SenzorsDbContract.Secret.COLUMN_UNIQUE_ID + " =?",
                    new String[]{uid});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Add permissions for senz user
     *
     * @param senz
     */
    public void createPermissionsForUser(Senz senz) {
        Log.d(TAG, "Add New Permission: adding permission from - " + senz.getSender());
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // content values to inset
        ContentValues values = new ContentValues();
        values.put(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA, 0);
        values.put(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION, 0);
        values.put(SenzorsDbContract.Permission.COLUMN_NAME_MIC, 0);
        values.put(SenzorsDbContract.Permission.COLOMN_NAME_USER, senz.getSender().getUsername());

        // Insert the new row, if fails throw an error
        db.insertOrThrow(SenzorsDbContract.Permission.TABLE_NAME, null, values);
    }

    /**
     * Add configurable permissions for senz user
     *
     * @param senz
     */
    public void createConfigurablePermissionsForUser(Senz senz) {
        Log.d(TAG, "Add New Permission: adding permission from - " + senz.getSender());
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // content values to inset
        ContentValues values = new ContentValues();

        values.put(SenzorsDbContract.PermissionConfiguration.COLUMN_UNIQUE_ID, senz.getAttributes().get("uid"));
        values.put(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA, 0);
        values.put(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION, 0);
        values.put(SenzorsDbContract.Permission.COLUMN_NAME_MIC, 0);
        values.put(SenzorsDbContract.PermissionConfiguration.COLOMN_NAME_USER, senz.getSender().getUsername());

        // Insert the new row, if fails throw an error
        db.insertOrThrow(SenzorsDbContract.PermissionConfiguration.TABLE_NAME, null, values);
    }

    /**
     * Get ALl secrets to be display in chat list
     *
     * @return sensor list
     */
    public ArrayList<Secret> getSecretz(User user) {
        ArrayList<Secret> secretList = new ArrayList();

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
        String query = "SELECT _id, uid, blob, type, user, is_sender, deleted, delivered, delivery_fail, timestamp, timestamp_seen " +
                "FROM secret WHERE user = ? ORDER BY _id ASC";
        Cursor cursor = db.rawQuery(query, new String[]{user.getUsername()});

        // secret attr
        String _secretId;
        String _secretBlob;
        String _secretBlobType;
        String _secretUser;
        int _secretIsSender;
        int _secretDelete;
        int _secretIsDelivered;
        int _secretDeliveryFailed;
        Long _secretTimestamp;
        Long _secretTimestampSeen;

        // extract attributes
        while (cursor.moveToNext()) {
            // get secret attributes
            _secretId = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_UNIQUE_ID));
            _secretBlob = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_BLOB));
            _secretBlobType = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_BLOB_TYPE));
            _secretUser = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_USER));
            _secretIsSender = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_IS_SENDER));
            _secretIsDelivered = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_DELIVERED));
            _secretDeliveryFailed = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_DELIVERY_FAILED));
            _secretDelete = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_DELETE));
            _secretTimestamp = cursor.getLong(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_TIMESTAMP));
            _secretTimestampSeen = cursor.getLong(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_TIMESTAMP_SEEN));

            // create secret
            Secret secret = new Secret(_secretBlob, _secretBlobType, new User("", _secretUser), _secretIsSender == 1 ? true : false);
            secret.setDelete(_secretDelete == 1 ? true : false);
            secret.setIsDelivered(_secretIsDelivered == 1 ? true : false);
            secret.setDeliveryFailed(_secretDeliveryFailed == 1 ? true : false);
            secret.setTimeStamp(_secretTimestamp);
            secret.setSeenTimeStamp(_secretTimestampSeen);
            secret.setID(_secretId);
            // fill secret list
            secretList.add(secret);
        }

        // clean
        cursor.close();

        Log.d(TAG, "GetSecretz: secrets count " + secretList.size());
        return secretList;
    }

    /**
     * Get secrets from give timestamp, used for lazy loading!!!
     *
     * @param user
     * @param timestamp
     * @return
     */
    public ArrayList<Secret> getSecretz(User user, Long timestamp) {
        ArrayList<Secret> secretList = new ArrayList();

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
        String query = "SELECT _id, uid, blob, type, user, is_sender, deleted, delivered, delivery_fail, timestamp, timestamp_seen " +
                "FROM secret WHERE user = ? AND timestamp > ? ORDER BY _id ASC";
        Cursor cursor = db.rawQuery(query, new String[]{user.getUsername(), timestamp.toString()});

        // secret attr
        String _secretId;
        String _secretBlob;
        String _secretBlobType;
        String _secretUser;
        int _secretIsSender;
        int _secretDelete;
        int _secretIsDelivered;
        int _secretDeliveryFailed;
        Long _secretTimestamp;
        Long _secretTimestampSeen;

        // extract attributes
        while (cursor.moveToNext()) {
            // get secret attributes
            _secretId = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_UNIQUE_ID));
            _secretBlob = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_BLOB));
            _secretBlobType = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_BLOB_TYPE));
            _secretUser = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_USER));
            _secretIsSender = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_IS_SENDER));
            _secretIsDelivered = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_DELIVERED));
            _secretDeliveryFailed = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_DELIVERY_FAILED));
            _secretDelete = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_DELETE));
            _secretTimestamp = cursor.getLong(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_TIMESTAMP));
            _secretTimestampSeen = cursor.getLong(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_TIMESTAMP_SEEN));


            // create secret
            Secret secret = new Secret(_secretBlob, _secretBlobType, new User("", _secretUser), _secretIsSender == 1 ? true : false);
            secret.setDelete(_secretDelete == 1 ? true : false);
            secret.setIsDelivered(_secretIsDelivered == 1 ? true : false);
            secret.setDeliveryFailed(_secretDeliveryFailed == 1 ? true : false);
            secret.setTimeStamp(_secretTimestamp);
            secret.setSeenTimeStamp(_secretTimestampSeen);
            secret.setID(_secretId);
            ;
            // fill secret list
            secretList.add(secret);
        }

        // clean
        cursor.close();

        Log.d(TAG, "GetSecretz: secrets count " + secretList.size());
        return secretList;
    }

    /**
     * GEt list of the lates chat messages!!!!
     *
     * @return
     */
    public ArrayList<Secret> getLatestChatMessages() {
        ArrayList<Secret> secretList = new ArrayList();

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
        String query = "SELECT MAX(_id), _id, blob, type, user, is_sender, timestamp FROM secret " +
                "GROUP BY user";

        Cursor cursor = db.rawQuery(query, null);

        // secret attr
        String _secretBlob;
        String _secretBlobType;
        String _secretUser;
        Long _secretTimestamp;
        int _secretIsSender;

        // extract attributes
        while (cursor.moveToNext()) {
            // get secret attributes
            _secretBlob = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.LatestChat.COLUMN_BLOB));
            _secretBlobType = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.LatestChat.COLUMN_TYPE));
            _secretUser = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.LatestChat.COLUMN_USER));
            _secretTimestamp = cursor.getLong(cursor.getColumnIndex(SenzorsDbContract.LatestChat.COLUMN_TIMESTAMP));
            _secretIsSender = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_IS_SENDER));


            // create secret
            User user = new User("", _secretUser);

            user.setUserImage(getImageFromDB(_secretUser));
            Secret secret = new Secret(_secretBlob, _secretBlobType, user, _secretIsSender == 1 ? true : false);
            secret.setTimeStamp(_secretTimestamp);
            // fill secret list
            secretList.add(secret);
        }

        // clean
        cursor.close();

        Log.d(TAG, "GetSecretz: secrets count " + secretList.size());
        return secretList;
    }

    /**
     * Get User information like image and username with theirs permissions
     * Inner Join happening under the hood to combine the user table with permissions table
     *
     * @param user
     * @return
     */
    public UserPermission getUserAndPermission(User user) {
        UserPermission userPerm = null;

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();

        // join query to retrieve data
        String query = "SELECT permission.camera, permission.location, permission.mic, user.image " +
                "FROM user " +
                "INNER JOIN permission " +
                "ON user.username = permission.user WHERE permission.user = ?";
        Cursor cursor = db.rawQuery(query, new String[]{user.getUsername()});

        // user attributes
        String _userimage;
        boolean _location;
        boolean _camera;
        boolean _mic;

        // extract attributes
        if (cursor.moveToFirst()) {

            // get permission attributes
            _camera = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA)) == 1 ? true : false;
            _location = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION)) == 1 ? true : false;
            _mic = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_MIC)) == 1 ? true : false;

            // create senz
            _userimage = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLOMN_NAME_IMAGE));
            user.setUserImage(_userimage);
            userPerm = new UserPermission(user, _camera, _location, _mic);
        }

        // clean
        cursor.close();

        return userPerm;
    }

    /**
     * Get only permissions for the given user.
     *
     * @param user
     * @return
     */
    public UserPermission getUserPermission(User user) {
        UserPermission userPerm = null;

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();

        // query
        String query = "SELECT permission.location, permission.camera, permission.mic " +
                "FROM permission " +
                "WHERE permission.user = ?";
        Cursor cursor = db.rawQuery(query, new String[]{user.getUsername()});

        // user attributes
        String _username;
        boolean _location;
        boolean _camera;
        boolean _mic;
        String _userId;

        // extract attributes
        if (cursor.moveToFirst()) {

            // get permission attributes
            _location = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION)) == 1 ? true : false;
            _camera = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA)) == 1 ? true : false;
            _mic = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_MIC)) == 1 ? true : false;

            // create senz
            userPerm = new UserPermission(user, _camera, _location, _mic);
        }

        // clean
        cursor.close();

        return userPerm;
    }

    /**
     * Get User configuarable permission only for the given user
     *
     * @param user
     * @return
     */
    public UserPermission getUserConfigPermission(User user) {
        UserPermission userPerm = null;

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();

        // join query to retrieve data
        String query = "SELECT permission_config.location, permission_config.camera, permission_config.mic " +
                "FROM permission_config " +
                "WHERE permission_config.user = ?";
        Cursor cursor = db.rawQuery(query, new String[]{user.getUsername()});

        // user attributes
        boolean _location;
        boolean _camera;
        boolean _mic;

        // extract attributes
        if (cursor.moveToFirst()) {

            // get permission attributes
            _location = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION)) == 1 ? true : false;
            _camera = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA)) == 1 ? true : false;
            _mic = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_MIC)) == 1 ? true : false;

            // create senz
            userPerm = new UserPermission(user, _camera, _location, _mic);
        }

        // clean
        cursor.close();

        return userPerm;
    }

    /**
     * Get list of all users/friends and also their permissions in a List
     *
     * @return
     */
    public List<UserPermission> getUsersAndTheirPermissions() {
        List<UserPermission> permissionList = new ArrayList();

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();

        // join query to retrieve data
        String query = "SELECT user._id, user.username, user.image, permission.location, permission.camera, permission.mic " +
                "FROM user " +
                "INNER JOIN permission " +
                "ON user.username = permission.user";
        Cursor cursor = db.rawQuery(query, null);

        // sensor/user attributes
        String _username;
        boolean _location;
        boolean _camera;
        boolean _mic;
        String _userId;
        String _userImage;

        // extract attributes
        while (cursor.moveToNext()) {

            // get permission attributes
            _location = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION)) == 1 ? true : false;
            _camera = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA)) == 1 ? true : false;
            _mic = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_MIC)) == 1 ? true : false;

            // get user attributes
            _userId = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User._ID));
            _username = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_USERNAME));
            _userImage = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLOMN_NAME_IMAGE));

            // create senz
            User user = new User(_userId, _username);
            user.setUserImage(_userImage);
            UserPermission userPerm = new UserPermission(user, _camera, _location, _mic);

            // fill senz list
            permissionList.add(userPerm);
        }

        // clean
        cursor.close();

        return permissionList;
    }

    /**
     * Delete sec from database,
     *
     * @param
     */
    public void deleteSecret(Secret secret) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // delete senz of given user
        db.delete(SenzorsDbContract.Secret.TABLE_NAME,
                SenzorsDbContract.Secret.COLUMN_UNIQUE_ID + "=?",
                new String[]{secret.getID()});

    }

    public void deleteAllSecretsExceptLast() {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();
        db.delete(SenzorsDbContract.Secret.TABLE_NAME,
                "uid NOT IN (SELECT uid FROM " + SenzorsDbContract.Secret.TABLE_NAME + " ORDER BY _id DESC LIMIT 1)",
                null);
    }

    public void deleteAllSecretsThatBelongToUser(User user) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // delete senz of given user
        db.delete(SenzorsDbContract.Secret.TABLE_NAME,
                SenzorsDbContract.Secret.COLUMN_NAME_USER + "=?",
                new String[]{user.getUsername()});
    }

    public void updateSeenTimestamp(Secret secret) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        try {
            db.beginTransaction();

            // content values to inset
            ContentValues values = new ContentValues();
            values.put(SenzorsDbContract.Secret.COLUMN_TIMESTAMP_SEEN, secret.getSeenTimeStamp());

            // update
            db.update(SenzorsDbContract.Secret.TABLE_NAME,
                    values,
                    SenzorsDbContract.Secret.COLUMN_UNIQUE_ID + "=?",
                    new String[]{secret.getID()});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void insertImageToDB(String username, String encodedImage) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        try {
            db.beginTransaction();
            Log.i(TAG, "USER IMAGE STORED TO DB : " + encodedImage);

            // content values to inset
            ContentValues values = new ContentValues();
            values.put(SenzorsDbContract.User.COLOMN_NAME_IMAGE, encodedImage);

            // update
            db.update(SenzorsDbContract.User.TABLE_NAME,
                    values,
                    SenzorsDbContract.User.COLUMN_NAME_USERNAME + " = ?",
                    new String[]{username});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public String getImageFromDB(String username) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();

        // query
        String query = "SELECT user.image " +
                "FROM user " +
                "WHERE user.username = ?";
        Cursor cursor = db.rawQuery(query, new String[]{username});

        // user attributes
        String _userImage = null;

        // extract attributes
        if (cursor.moveToFirst()) {
            _userImage = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLOMN_NAME_IMAGE));
        }

        Log.i(TAG, "USER IMAGE RETRIEVED FROM DB : " + _userImage);

        // clean
        cursor.close();

        return _userImage;

    }

}
