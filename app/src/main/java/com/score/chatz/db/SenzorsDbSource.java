package com.score.chatz.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.score.chatz.pojo.Permission;
import com.score.chatz.pojo.Secret;
import com.score.chatz.pojo.SecretUser;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

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

//    /**
//     * Get user if exists in the database, other wise create user and return
//     *
//     * @param username username
//     * @return user
//     */
//    public User getOrCreateUser(String username) {
//        Log.d(TAG, "GetOrCreateUser: " + username);
//
//        // get matching user if exists
//        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();
//        Cursor cursor = db.query(SenzorsDbContract.User.TABLE_NAME, // table
//                null, SenzorsDbContract.User.COLUMN_NAME_USERNAME + "=?", // constraint
//                new String[]{username}, // prams
//                null, // order by
//                null, // group by
//                null); // join
//
//        if (cursor.moveToFirst()) {
//            // have matching user
//            // so get user data
//            // we return id as password since we no storing users password in database
//            String _id = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User._ID));
//            String _username = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_USERNAME));
//            String _phoneNumber = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_PHONE));
//
//            // clear
//            cursor.close();
//
//            Log.d(TAG, "have user, so return it: " + username);
//            User user = new User(_id, _username);
//            user.setPhoneNumber(_phoneNumber);
//            return user;
//        } else {
//            // no matching user
//            // so create user
//            ContentValues values = new ContentValues();
//            values.put(SenzorsDbContract.User.COLUMN_NAME_USERNAME, username);
//
//            // inset data
//            long id = db.insert(SenzorsDbContract.User.TABLE_NAME, SenzorsDbContract.User.COLUMN_NAME_USERNAME, values);
//            //db.close();
//
//            Log.d(TAG, "no user, so user created: " + username + " " + id);
//            return new User(Long.toString(id), username);
//        }
//    }
//
//    public boolean isAddedUser(String username) {
//        // get matching user if exists
//        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();
//        Cursor cursor = db.query(SenzorsDbContract.User.TABLE_NAME, // table
//                null, SenzorsDbContract.User.COLUMN_NAME_USERNAME + "=?", // constraint
//                new String[]{username}, // prams
//                null, // order by
//                null, // group by
//                null); // join
//
//        if (cursor.moveToFirst()) {
//            // clear
//            cursor.close();
//            return true;
//        } else {
//            return false;
//        }
//    }

//    /**
//     * Update permissions given to current user by others/Friends
//     *
//     * @param user    Other user or friend
//     * @param camPerm Camera Permission given to current user by friend
//     * @param locPerm Locations Permission given to current user by friend
//     */
//    public void updatePermissions(User user, String camPerm, String locPerm, String micPerm) {
//        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();
//
//        // initial permissions
//        int _camPerm = 0;
//        int _locPerm = 0;
//        int _micPerm = 0;
//
//        // Convert String value of boolean into an integer to be stored in sqlite (Since no support for boolean type)
//        // update the inital variables with the new converted values
//        if (camPerm != null) {
//            _camPerm = camPerm.equalsIgnoreCase("on") ? 1 : 0;
//        }
//        if (locPerm != null) {
//            _locPerm = locPerm.equalsIgnoreCase("on") ? 1 : 0;
//        }
//        if (micPerm != null) {
//            _micPerm = micPerm.equalsIgnoreCase("on") ? 1 : 0;
//        }
//
//        // content values to inset
//        ContentValues values = new ContentValues();
//        if (camPerm != null) {
//            values.put(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA, _camPerm);
//        }
//        if (locPerm != null) {
//            values.put(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION, _locPerm);
//        }
//        if (micPerm != null) {
//            values.put(SenzorsDbContract.Permission.COLUMN_NAME_MIC, _micPerm);
//        }
//
//        // update
//        db.update(SenzorsDbContract.Permission.TABLE_NAME,
//                values,
//                SenzorsDbContract.Permission.COLUMN_NAME_USER + " = ?",
//                new String[]{user.getUsername()});
//    }
//
//    /**
//     * Update configurable permissions the current user gives to others/Friends
//     * Configurable permissiona are used to persist the state of the switch the user can toggle
//     *
//     * @param user    Other user or friend
//     * @param camPerm Camera Permission given by current user to a friend
//     * @param locPerm Locations Permission given by current user to a friend
//     */
//    public void updateConfigurablePermissions(User user, String camPerm, String locPerm, String micPerm) {
//        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();
//
//        // intial permissions
//        int _camPerm = 0;
//        int _locPerm = 0;
//        int _micPerm = 0;
//
//        // Convert String value of boolean into an integer to be stored in sqlite (Since no support for boolean type)
//        // update the inital variables with the new converted values
//        if (camPerm != null) {
//            _camPerm = camPerm.equalsIgnoreCase("on") ? 1 : 0;
//        }
//        if (locPerm != null) {
//            _locPerm = locPerm.equalsIgnoreCase("on") ? 1 : 0;
//        }
//        if (micPerm != null) {
//            _micPerm = micPerm.equalsIgnoreCase("on") ? 1 : 0;
//        }
//
//        // content values to inset
//        ContentValues values = new ContentValues();
//        if (camPerm != null) {
//            values.put(SenzorsDbContract.PermissionConfiguration.COLUMN_NAME_CAMERA, _camPerm);
//        }
//        if (locPerm != null) {
//            values.put(SenzorsDbContract.PermissionConfiguration.COLUMN_NAME_LOCATION, _locPerm);
//        }
//        if (micPerm != null) {
//            values.put(SenzorsDbContract.Permission.COLUMN_NAME_MIC, _micPerm);
//        }
//
//        // update
//        db.update(SenzorsDbContract.PermissionConfiguration.TABLE_NAME,
//                values,
//                SenzorsDbContract.PermissionConfiguration.COLOMN_NAME_USER + " = ?",
//                new String[]{user.getUsername()});
//    }


    /**********************************************************************************************/
    /* BEGIN user related functions */

    /**********************************************************************************************/

    public void createSecretUser(SecretUser secretUser) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // content values to inset
        ContentValues values = new ContentValues();
        values.put(SenzorsDbContract.User.COLUMN_NAME_USERNAME, secretUser.getUsername());
        if (secretUser.getPhone() != null && secretUser.getPhone().isEmpty())
            values.put(SenzorsDbContract.User.COLUMN_NAME_PHONE, secretUser.getPhone());
        if (secretUser.getPubKey() != null && secretUser.getPubKey().isEmpty())
            values.put(SenzorsDbContract.User.COLUMN_NAME_PUBKEY, secretUser.getPubKey());
        if (secretUser.getPubKeyHash() != null && secretUser.getPubKeyHash().isEmpty())
            values.put(SenzorsDbContract.User.COLUMN_NAME_PUBKEY_HASH, secretUser.getPubKeyHash());
        if (secretUser.getImage() != null && secretUser.getImage().isEmpty())
            values.put(SenzorsDbContract.User.COLUMN_NAME_IMAGE, secretUser.getImage());
        values.put(SenzorsDbContract.User.COLUMN_NAME_IS_ACTIVE, secretUser.isActive() ? 1 : 0);

        // Insert the new row, if fails throw an error
        // fails means user already exists
        db.insertOrThrow(SenzorsDbContract.User.TABLE_NAME, null, values);
    }

    public void updateSecretUser(String username, String key, String value) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // content values to inset
        ContentValues values = new ContentValues();
        if (key.equalsIgnoreCase("phone")) {
            values.put(SenzorsDbContract.User.COLUMN_NAME_PHONE, value);
        } else if (key.equalsIgnoreCase("pubkey")) {
            values.put(SenzorsDbContract.User.COLUMN_NAME_PUBKEY, value);
        } else if (key.equalsIgnoreCase("pubkey_hash")) {
            values.put(SenzorsDbContract.User.COLUMN_NAME_PUBKEY_HASH, value);
        } else if (key.equalsIgnoreCase("image")) {
            values.put(SenzorsDbContract.User.COLUMN_NAME_IMAGE, value);
        }

        // update
        db.update(SenzorsDbContract.User.TABLE_NAME,
                values,
                SenzorsDbContract.User.COLUMN_NAME_USERNAME + " = ?",
                new String[]{username});
    }

    public void setSecretUserActive(String username, boolean isActive) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // content values to inset
        ContentValues values = new ContentValues();
        values.put(SenzorsDbContract.User.COLUMN_NAME_IS_ACTIVE, isActive ? 1 : 0);

        // update
        db.update(SenzorsDbContract.User.TABLE_NAME,
                values,
                SenzorsDbContract.User.COLUMN_NAME_USERNAME + " = ?",
                new String[]{username});
    }

    public SecretUser getSecretUser(String username) {

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
        String query = "SELECT * " +
                "FROM user " +
                "INNER JOIN permission " +
                "ON user.username = permission.user " +
                "WHERE user = ?";
        Cursor cursor = db.rawQuery(query, new String[]{username});

        if (cursor.moveToFirst()) {
            // have matching user
            // so get user data
            // we return id as password since we no storing users password in database
            String _userID = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User._ID));
            String _username = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_USERNAME));
            String _phone = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_PHONE));
            String _pubKey = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_PUBKEY));
            String _pubKeyHash = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_PUBKEY_HASH));
            int _isActive = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_IS_ACTIVE));
            String _image = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_IMAGE));

            String _permID = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Permission._ID));
            boolean _cameraPerm = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA)) == 1;
            boolean _locationPerm = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION)) == 1;
            boolean _micPerm = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_MIC)) == 1;
            boolean _isGiven = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_IS_GIVEN)) == 1;

            // clear
            cursor.close();

            SecretUser secretUser = new SecretUser(_userID, _username);
            secretUser.setPhone(_phone);
            secretUser.setPhone(_pubKey);
            secretUser.setPhone(_pubKeyHash);
            secretUser.setImage(_image);
            secretUser.setActive(_isActive == 1);

            Permission permission = new Permission(_permID, _username, _isGiven);
            permission.setCam(_cameraPerm);
            permission.setLoc(_locationPerm);
            permission.setMic(_micPerm);

            List<Permission> permList = new ArrayList<>();
            permList.add(permission);

            secretUser.setPermissions(permList);

            return secretUser;
        }

        return null;
    }

    public ArrayList<SecretUser> getSecretUserList() {

        // Two permission variables - isGiven and !isGiven
        final int NO_PERMISSION_VARIABLES = 2;

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
        String query = "SELECT * " +
                "FROM user " +
                "INNER JOIN permission " +
                "ON user.username = permission.user";
        Cursor cursor = db.rawQuery(query, null);

        ArrayList<SecretUser> secretUserList = new ArrayList<>();

        while(cursor.moveToNext()) {

            String _userID = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User._ID));
            String _username = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_USERNAME));
            String _phone = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_PHONE));
            String _pubKey = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_PUBKEY));
            String _pubKeyHash = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_PUBKEY_HASH));
            int _isActive = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_IS_ACTIVE));
            String _image = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_IMAGE));

            SecretUser secretUser = new SecretUser(_userID, _username);
            secretUser.setPhone(_phone);
            secretUser.setPhone(_pubKey);
            secretUser.setPhone(_pubKeyHash);
            secretUser.setImage(_image);
            secretUser.setActive(_isActive == 1);

            List<Permission> permList = new ArrayList<>();
            for(int i = 0; i < NO_PERMISSION_VARIABLES; i++){
                // Create Permissiosn
                String _permID = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Permission._ID));
                boolean _cameraPerm = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA)) == 1;
                boolean _locationPerm = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION)) == 1;
                boolean _micPerm = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_MIC)) == 1;
                boolean _isGiven = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_IS_GIVEN)) == 1;

                Permission permission = new Permission(_permID, _username, _isGiven);
                permission.setCam(_cameraPerm);
                permission.setLoc(_locationPerm);
                permission.setMic(_micPerm);

                permList.add(permission);

                if(i == NO_PERMISSION_VARIABLES - 1){
                    secretUser.setPermissions(permList);
                }

                // Move to next permission
                cursor.moveToNext();
            }

            // Add created User to list
            secretUserList.add(secretUser);

            // Move to next user
            cursor.moveToNext();
        }

        // clear
        cursor.close();


        return secretUserList;
    }

    /**********************************************************************************************/
    /* END user related functions */
    /**********************************************************************************************/


    /**********************************************************************************************/
    /* BEGIN permission related functions */

    /**********************************************************************************************/

    public void createPermission(Permission permission) {
        Log.d(TAG, "Add new permission for " + permission.getUsername() + "with isGiven=" + permission.isGiven());
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // content values to inset
        ContentValues values = new ContentValues();
        values.put(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA, 0);
        values.put(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION, 0);
        values.put(SenzorsDbContract.Permission.COLUMN_NAME_MIC, 0);
        values.put(SenzorsDbContract.Permission.COLUMN_NAME_IS_GIVEN, permission.isGiven() ? 1 : 0);
        values.put(SenzorsDbContract.Permission.COLUMN_NAME_USER, permission.getUsername());

        // Insert the new row, if fails throw an error
        db.insertOrThrow(SenzorsDbContract.Permission.TABLE_NAME, null, values);
    }

    public void updatePermission(String username, String permName, boolean permVal, boolean isGiven) {
        Log.d(TAG, "Update permission " + permName + "of " + username + "with isGiven=" + isGiven);
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(permName, permVal ? 1 : 0);

        // update
        db.update(SenzorsDbContract.Permission.TABLE_NAME,
                values,
                SenzorsDbContract.Permission.COLUMN_NAME_USER + " = ? AND is_given = ?",
                new String[]{username, isGiven ? "1" : "0"});
    }

    public Permission getPermission(String username, boolean isGiven) {
        // get matching user if exists
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();
        Cursor cursor = db.query(SenzorsDbContract.Permission.TABLE_NAME, // table
                null, // columns
                SenzorsDbContract.Permission.COLUMN_NAME_USER + "=? AND " + SenzorsDbContract.Permission.COLUMN_NAME_IS_GIVEN + "=?", // constraint
                new String[]{username, isGiven ? "1" : "0"}, // prams
                null, // order by
                null, // group by
                null); // join

        if (cursor.moveToFirst()) {
            String _id = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Permission._ID));
            String _username = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_USERNAME));
            boolean _location = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION)) == 1;
            boolean _cam = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA)) == 1;
            boolean _mic = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_MIC)) == 1;

            // clear
            cursor.close();

            Permission permission = new Permission(_id, _username, isGiven);
            permission.setLoc(_location);
            permission.setCam(_cam);
            permission.setMic(_mic);

            return permission;
        }

        return null;
    }

    /**********************************************************************************************/
    /* END permission related functions */
    /**********************************************************************************************/

    /**********************************************************************************************/
    /* BEGIN secret related functions */
    /**********************************************************************************************/

    /**
     * Create Secret message or images
     *
     * @param secret
     */
    public void createSecret(Secret secret) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();
        // content values to inset
        ContentValues values = new ContentValues();
        values.put(SenzorsDbContract.Secret.COLUMN_UNIQUE_ID, secret.getId());
        values.put(SenzorsDbContract.Secret.COLUMN_TIMESTAMP, secret.getTimeStamp());
        values.put(SenzorsDbContract.Secret.COLUMN_NAME_USER, secret.getUser().getUsername());
        values.put(SenzorsDbContract.Secret.COLUMN_NAME_IS_SENDER, secret.isSender() ? 1 : 0);
        values.put(SenzorsDbContract.Secret.COLUMN_BLOB_TYPE, secret.getType());
        values.put(SenzorsDbContract.Secret.COLUMN_NAME_BLOB, secret.getBlob());
        values.put(SenzorsDbContract.Secret.COLUMN_NAME_VIEWED, secret.isViewed() ? 1 : 0);
        values.put(SenzorsDbContract.Secret.COLUMN_NAME_VIEWED_TIMESTAMP, 0);
        values.put(SenzorsDbContract.Secret.COLUMN_NAME_MISSED, secret.isMissed() ? 1 : 0);
        values.put(SenzorsDbContract.Secret.COLUMN_NAME_DELIVERED, 0);
        values.put(SenzorsDbContract.Secret.COLUMN_NAME_DISPATCHED, 0);

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
     * Mark message as viewed
     *
     * @param uid unique identifier of message
     */
    public void markSecretViewed(String uid) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // content values to inset
        ContentValues values = new ContentValues();
        values.put(SenzorsDbContract.Secret.COLUMN_NAME_VIEWED, 1);
        long timestamp = System.currentTimeMillis() / 1000;
        values.put(SenzorsDbContract.Secret.COLUMN_NAME_VIEWED_TIMESTAMP, timestamp);

        // update
        db.update(SenzorsDbContract.Secret.TABLE_NAME,
                values,
                SenzorsDbContract.Secret.COLUMN_UNIQUE_ID + " =?",
                new String[]{uid});
    }

    /**
     * Mark message as delivered
     *
     * @param uid unique identifier of message
     */
    public void markSecretDelivered(String uid) {
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

    /**********************************************************************************************/
    /* BEGIN secret related functions */
    /**********************************************************************************************/

    /**
     * Add permissions for senz user
     *
     * @param username
     */
    public void createPermissionsForUser(String username) {
        Log.d(TAG, "Add New Permission: adding permission from - " + username);
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // content values to inset
        ContentValues values = new ContentValues();
        values.put(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA, 0);
        values.put(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION, 0);
        values.put(SenzorsDbContract.Permission.COLUMN_NAME_MIC, 0);
        values.put(SenzorsDbContract.Permission.COLUMN_NAME_USER, username);

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
    public ArrayList<Secret> getSecrets(SecretUser secretUser) {
        ArrayList<Secret> secretList = new ArrayList();

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
        String query = "SELECT _id, uid, blob, type, user, is_sender, viewed, view_timestamp, missed, delivered, dispatched, timestamp " +
                "FROM secret WHERE user = ? ORDER BY _id ASC";
        Cursor cursor = db.rawQuery(query, new String[]{secretUser.getUsername()});

        // secret attr
        String _secretId;
        String _secretBlob;
        String _secretBlobType;
        int _secretIsSender;
        int _isViewed;
        int _isMissed;
        int _secretIsDelivered;
        int _secretIsDispatched;
        Long _secretTimestamp;
        Long _secretViewTimestamp;

        // extract attributes
        while (cursor.moveToNext()) {
            // get secret attributes
            _secretId = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_UNIQUE_ID));
            _secretTimestamp = cursor.getLong(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_TIMESTAMP));
            _secretIsSender = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_IS_SENDER));
            _secretBlobType = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_BLOB_TYPE));
            _secretBlob = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_BLOB));
            _isViewed = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_VIEWED));
            _secretViewTimestamp = cursor.getLong(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_VIEWED_TIMESTAMP));
            _isMissed = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_MISSED));
            _secretIsDelivered = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_DELIVERED));
            _secretIsDispatched = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_DISPATCHED));

            // create secret
            Secret secret = new Secret(_secretBlob, _secretBlobType, secretUser, _secretIsSender == 1);
            secret.setId(_secretId);
            secret.setViewed(_isViewed == 1);
            secret.setMissed(_isMissed == 1);
            secret.setDelivered(_secretIsDelivered == 1);
            secret.setDispatched(_secretIsDispatched == 1);
            secret.setTimeStamp(_secretTimestamp);
            secret.setViewedTimeStamp(_secretViewTimestamp);

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
     * @param secretUser
     * @param timestamp
     * @return
     */
    public ArrayList<Secret> getSecrets(SecretUser secretUser, Long timestamp) {
        ArrayList secretList = new ArrayList();

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
        String query = "SELECT _id, uid, blob, type, user, is_sender, viewed, view_timestamp, missed, delivered, dispatched, timestamp " +
                "FROM secret WHERE user = ? AND timestamp > ? ORDER BY _id ASC";
        Cursor cursor = db.rawQuery(query, new String[]{secretUser.getUsername(), timestamp.toString()});

        // secret attr
        String _secretId;
        String _secretBlob;
        String _secretBlobType;
        String _secretUser;
        int _secretIsSender;
        int _isViewed;
        int _isMissed;
        int _secretIsDelivered;
        int _secretIsDispatched;
        Long _secretTimestamp;
        Long _secretViewTimestamp;

        // extract attributes
        while (cursor.moveToNext()) {
            // get secret attributes
            _secretId = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_UNIQUE_ID));
            _secretTimestamp = cursor.getLong(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_TIMESTAMP));
            _secretUser = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_USER));
            _secretIsSender = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_IS_SENDER));
            _secretBlobType = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_BLOB_TYPE));
            _secretBlob = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_BLOB));
            _isViewed = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_VIEWED));
            _secretViewTimestamp = cursor.getLong(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_VIEWED_TIMESTAMP));
            _isMissed = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_MISSED));
            _secretIsDelivered = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_DELIVERED));
            _secretIsDispatched = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_DISPATCHED));

            // create secret
            Secret secret = new Secret(_secretBlob, _secretBlobType, secretUser, _secretIsSender == 1);
            secret.setId(_secretId);
            secret.setViewed(_isViewed == 1);
            secret.setMissed(_isMissed == 1);
            secret.setDelivered(_secretIsDelivered == 1);
            secret.setDispatched(_secretIsDispatched == 1);
            secret.setTimeStamp(_secretTimestamp);
            secret.setViewedTimeStamp(_secretViewTimestamp);

            // fill secret list
            secretList.add(secret);
        }

        // clean
        cursor.close();

        Log.d(TAG, "GetSecretz: secrets count " + secretList.size());
        return secretList;
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
                new String[]{secret.getId()});
    }

    public void deleteAllSecretsExceptLast(String username) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();
        //String sqlDelete = "delete from secret where uid in (select uid from secret where _id not in (select _id from secret where user = '" + username + "' order by _id DESC limit 1) and user = '" + username + "')";

//        String sqlDelete =
//                "delete from secret where " +
//                        "uid in " +
//                        "(select uid from secret where " +
//                        "_id not in(select _id from secret where user = '" + username + "' order by _id DESC limit 1) and " +
//                        "user = '" + username + "' and " +
//                        "missed = 0)";

        String sqlDelete =
                "uid in " +
                        "(select uid from secret where " +
                        "_id not in(select _id from secret where user = '" + username + "' order by _id DESC limit 7) and " +
                        "user = '" + username + "')";

        // TODO refactor/optimize this
        //String sqlDelete = "uid in (select uid from secret where _id not in(select _id from secret where user = '" + username + "' order by _id DESC limit 7) and user = '" + username + "')";
        db.delete(SenzorsDbContract.Secret.TABLE_NAME,
                sqlDelete,
                null);
    }

    public void deleteAllSecretsThatBelongToUser(User user) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // delete senz of given user
        db.delete(SenzorsDbContract.Secret.TABLE_NAME,
                SenzorsDbContract.Secret.COLUMN_NAME_USER + "=?",
                new String[]{user.getUsername()});

        // delete last secret
        db.delete(SenzorsDbContract.LatestChat.TABLE_NAME,
                SenzorsDbContract.Secret.COLUMN_NAME_USER + "=?",
                new String[]{user.getUsername()});
    }


    /**
     * GEt list of the lates chat messages!!!!
     *
     * @return
     */
    public ArrayList<Secret> getLatestChatMessages() {
        ArrayList<Secret> secretList = new ArrayList();

        // TODO JOIN with user to get user image
        //SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
        //String query = "SELECT MAX(_id), _id, blob, type, user, is_sender, timestamp FROM secret " +
        //        "GROUP BY user ORDER BY timestamp DESC";

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
        String query = "SELECT * " +
                "FROM latest_chat " +
                "INNER JOIN user " +
                "ON user.username = latest_chat.user GROUP BY user.username ORDER BY timestamp DESC";

        Cursor cursor = db.rawQuery(query, null);

        // secret attr
        String _userID;
        String _secretBlob;
        String _secretBlobType;
        String _secretUser;
        Long _secretTimestamp;
        String _image;
        int _secretIsSender;
        int _isActive;

        // extract attributes
        while (cursor.moveToNext()) {
            // get secret attributes
            _userID = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User._ID));
            _secretBlob = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.LatestChat.COLUMN_BLOB));
            _secretBlobType = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.LatestChat.COLUMN_TYPE));
            _secretUser = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.LatestChat.COLUMN_USER));
            _secretTimestamp = cursor.getLong(cursor.getColumnIndex(SenzorsDbContract.LatestChat.COLUMN_TIMESTAMP));
            _secretIsSender = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_IS_SENDER));

            // get user attributes
            _image = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_IMAGE));
            _isActive = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_IS_ACTIVE));

            SecretUser secretUser = new SecretUser(_userID, _secretUser);
            secretUser.setImage(_image);
            secretUser.setActive(_isActive == 1);

            Secret secret = new Secret(_secretBlob, _secretBlobType, secretUser, _secretIsSender == 1);
            secret.setTimeStamp(_secretTimestamp);
            // fill secret list
            secretList.add(secret);
        }

        // clean
        cursor.close();

        Log.d(TAG, "GetSecretz: secrets count " + secretList.size());
        return secretList;
    }

    /**********************************************************************************************/
    /* END secret related functions */
    /**********************************************************************************************/

//    /**
//     * Get User information like image and username with theirs permissions
//     * Inner Join happening under the hood to combine the user table with permissions table
//     *
//     * @param user
//     * @return
//     */
//    public UserPermission getUserAndPermission(User user) {
//        UserPermission userPerm = null;
//
//        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
//
//        // join query to retrieve data
//        String query = "SELECT permission.camera, permission.location, permission.mic, user.image " +
//                "FROM user " +
//                "INNER JOIN permission " +
//                "ON user.username = permission.user WHERE permission.user = ?";
//        Cursor cursor = db.rawQuery(query, new String[]{user.getUsername()});
//
//        // user attributes
//        String _userimage;
//        boolean _location;
//        boolean _camera;
//        boolean _mic;
//
//        // extract attributes
//        if (cursor.moveToFirst()) {
//
//            // get permission attributes
//            _camera = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA)) == 1 ? true : false;
//            _location = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION)) == 1 ? true : false;
//            _mic = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_MIC)) == 1 ? true : false;
//
//            // create senz
//            _userimage = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_IMAGE));
//            user.setUserImage(_userimage);
//            userPerm = new UserPermission(user, _camera, _location, _mic);
//        }
//
//        // clean
//        cursor.close();
//
//        return userPerm;
//    }
//
//    /**
//     * Get only permissions for the given user.
//     *
//     * @param user
//     * @return
//     */
//    public UserPermission getUserPermission(User user) {
//        UserPermission userPerm = null;
//
//        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
//
//        // query
//        String query = "SELECT permission.location, permission.camera, permission.mic " +
//                "FROM permission " +
//                "WHERE permission.user = ?";
//        Cursor cursor = db.rawQuery(query, new String[]{user.getUsername()});
//
//        // user attributes
//        String _username;
//        boolean _location;
//        boolean _camera;
//        boolean _mic;
//        String _userId;
//
//        // extract attributes
//        if (cursor.moveToFirst()) {
//
//            // get permission attributes
//            _location = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION)) == 1 ? true : false;
//            _camera = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA)) == 1 ? true : false;
//            _mic = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_MIC)) == 1 ? true : false;
//
//            // create senz
//            userPerm = new UserPermission(user, _camera, _location, _mic);
//        }
//
//        // clean
//        cursor.close();
//
//        return userPerm;
//    }
//
//    /**
//     * Get User configuarable permission only for the given user
//     *
//     * @param user
//     * @return
//     */
//    public UserPermission getUserConfigPermission(User user) {
//        UserPermission userPerm = null;
//
//        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
//
//        // join query to retrieve data
//        String query = "SELECT permission_config.location, permission_config.camera, permission_config.mic " +
//                "FROM permission_config " +
//                "WHERE permission_config.user = ?";
//        Cursor cursor = db.rawQuery(query, new String[]{user.getUsername()});
//
//        // user attributes
//        boolean _location;
//        boolean _camera;
//        boolean _mic;
//
//        // extract attributes
//        if (cursor.moveToFirst()) {
//
//            // get permission attributes
//            _location = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION)) == 1 ? true : false;
//            _camera = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA)) == 1 ? true : false;
//            _mic = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_MIC)) == 1 ? true : false;
//
//            // create senz
//            userPerm = new UserPermission(user, _camera, _location, _mic);
//        }
//
//        // clean
//        cursor.close();
//
//        return userPerm;
//    }
//
//    /**
//     * Get list of all users/friends and also their permissions in a List
//     *
//     * @return
//     */
//    public List<UserPermission> getUsersAndTheirPermissions() {
//        List<UserPermission> permissionList = new ArrayList();
//
//        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
//
//        // join query to retrieve data
//        String query = "SELECT user._id, user.username, user.phone, user.image, user.is_active, permission.location, permission.camera, permission.mic " +
//                "FROM user " +
//                "INNER JOIN permission " +
//                "ON user.username = permission.user";
//        Cursor cursor = db.rawQuery(query, null);
//
//        // sensor/user attributes
//        String _username;
//        boolean _location;
//        boolean _camera;
//        boolean _mic;
//        boolean _isActive;
//        String _userId;
//        String _userImage;
//        String _userPhone;
//
//        // extract attributes
//        while (cursor.moveToNext()) {
//
//            // get permission attributes
//            _location = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION)) == 1 ? true : false;
//            _camera = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA)) == 1 ? true : false;
//            _mic = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_MIC)) == 1 ? true : false;
//
//            // get user attributes
//            _userId = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User._ID));
//            _username = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_USERNAME));
//            _userImage = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_IMAGE));
//            _userPhone = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_PHONE));
//
//            // create senz
//            User user = new User(_userId, _username);
//            user.setUserImage(_userImage);
//            user.setPhoneNumber(_userPhone);
//
//            _isActive = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_IS_ACTIVE)) == 1 ? true : false;
//            user.setIsActive(_isActive);
//
//            UserPermission userPerm = new UserPermission(user, _camera, _location, _mic);
//
//            // fill senz list
//            permissionList.add(userPerm);
//        }
//
//        // clean
//        cursor.close();
//
//        return permissionList;
//    }
//
//
//    public void insertImageToDB(String username, String encodedImage) {
//        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();
//
//        try {
//            db.beginTransaction();
//            Log.i(TAG, "USER IMAGE STORED TO DB : " + encodedImage);
//
//            // content values to inset
//            ContentValues values = new ContentValues();
//            values.put(SenzorsDbContract.User.COLUMN_NAME_IMAGE, encodedImage);
//
//            // update
//            db.update(SenzorsDbContract.User.TABLE_NAME,
//                    values,
//                    SenzorsDbContract.User.COLUMN_NAME_USERNAME + " = ?",
//                    new String[]{username});
//
//            db.setTransactionSuccessful();
//        } finally {
//            db.endTransaction();
//        }
//    }
//
//    public String getImageFromDB(String username) {
//        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
//
//        // query
//        String query = "SELECT user.image " +
//                "FROM user " +
//                "WHERE user.username = ?";
//        Cursor cursor = db.rawQuery(query, new String[]{username});
//
//        // user attributes
//        String _userImage = null;
//
//        // extract attributes
//        if (cursor.moveToFirst()) {
//            _userImage = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_IMAGE));
//        }
//
//        Log.i(TAG, "USER IMAGE RETRIEVED FROM DB : " + _userImage);
//
//        // clean
//        cursor.close();
//
//        return _userImage;
//
//    }

}
