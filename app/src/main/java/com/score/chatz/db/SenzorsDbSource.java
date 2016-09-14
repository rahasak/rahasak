package com.score.chatz.db;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.pojo.Secret;
import com.score.chatz.pojo.UserPermission;
import com.score.chatz.ui.ChatFragment;
import com.score.chatz.utils.PreferenceUtils;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
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
     *  Update Location information of user
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
     * @param user Other user or friend
     * @param camPerm Camera Permission given to current user by friend
     * @param locPerm Locations Permission given to current user by friend
     */
    public void updatePermissions(User user, String camPerm, String locPerm, String micPerm) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // intial permissions
        int _camPerm = 0;
        int _locPerm = 0;
        int _micPerm = 0;

        // Convert String value of boolean into an integer to be stored in sqlite (Since no support for boolean type)
        // update the inital variables with the new converted values
        if(camPerm != null) {
            _camPerm = camPerm.equalsIgnoreCase("true") ? 1 : 0;
        }
        if(locPerm != null) {
            _locPerm = locPerm.equalsIgnoreCase("true") ? 1 : 0;
        }
        if(micPerm != null) {
            _micPerm = micPerm.equalsIgnoreCase("true") ? 1 : 0;
        }

        // content values to inset
        ContentValues values = new ContentValues();
        if(camPerm != null) {
            values.put(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA, _camPerm);
        }
        if(locPerm != null) {
            values.put(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION, _locPerm);
        }
        if(micPerm != null) {
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
     * @param user Other user or friend
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
        if(camPerm != null) {
            _camPerm = camPerm.equalsIgnoreCase("true") ? 1 : 0;
        }
        if(locPerm != null) {
            _locPerm = locPerm.equalsIgnoreCase("true") ? 1 : 0;
        }
        if(micPerm != null) {
            _micPerm = micPerm.equalsIgnoreCase("true") ? 1 : 0;
        }

        // content values to inset
        ContentValues values = new ContentValues();
        if(camPerm != null) {
            values.put(SenzorsDbContract.PermissionConfiguration.COLUMN_NAME_CAMERA, _camPerm);
        }
        if(locPerm != null) {
            values.put(SenzorsDbContract.PermissionConfiguration.COLUMN_NAME_LOCATION, _locPerm);
        }
        if(micPerm != null) {
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
    public void createSenz (Senz senz) {
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
     * @param secret
     */
    public void createSecret (Secret secret) {
        Log.d(TAG, "AddSecret, adding secret from - " + secret.getSender().getUsername());
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        try {
            db.beginTransaction();
            Log.i("SECRET", "Secret created: text - " + secret.getText() + ", sender - " + secret.getSender().getUsername() + ", receiver - " + secret.getReceiver().getUsername());

            // content values to inset
            ContentValues values = new ContentValues();
            values.put(SenzorsDbContract.Secret.COLUMN_NAME_TEXT, secret.getText());
            values.put(SenzorsDbContract.Secret.COLOMN_NAME_IMAGE, secret.getImage());
            values.put(SenzorsDbContract.Secret.COLOMN_NAME_IMAGE_THUMB, secret.getThumbnail());
            values.put(SenzorsDbContract.Secret.COLUMN_NAME_RECEIVER, secret.getReceiver().getUsername());
            values.put(SenzorsDbContract.Secret.COLUMN_NAME_SENDER, secret.getSender().getUsername());
            values.put(SenzorsDbContract.Secret.COLUMN_UNIQUE_ID, secret.getID());
            values.put(SenzorsDbContract.Secret.COLUMN_NAME_DELIVERED, 0);
            values.put(SenzorsDbContract.Secret.COLUMN_NAME_DELIVERY_FAILED, 0);
            values.put(SenzorsDbContract.Secret.COLUMN_NAME_DELETE, 0);
            values.put(SenzorsDbContract.Secret.COLUMN_TIMESTAMP, secret.getTimeStamp());
            values.put(SenzorsDbContract.Secret.COLUMN_NAME_SOUND, secret.getSound());

            // Insert the new row, if fails throw an error
            db.insertOrThrow(SenzorsDbContract.Secret.TABLE_NAME, null, values);
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }

    }

    /**
     * Mark message as delivered
     * @param uid unique identifier of message
     */
    public void markSecretDelievered (String uid) {
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
        }
        finally {
            db.endTransaction();
        }
    }

    /**
     * Mark message as delivery Failed
     * @param uid unique identifier of message
     */
    public void markSecretDeliveryFailed (String uid) {
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
        }
        finally {
            db.endTransaction();
        }
    }

    /**
     * Add permissions for senz user
     * @param senz
     */
    public void createPermissionsForUser(Senz senz){
        Log.d(TAG, "Add New Permission: adding permission from - " + senz.getSender());
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // content values to inset
        ContentValues values = new ContentValues();
        if (senz.getAttributes().containsKey("permCam")) {
            values.put(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA, ((String)senz.getAttributes().get("permCam")).equalsIgnoreCase("true") ? 1 : 0);
        }
        if (senz.getAttributes().containsKey("permLoc")) {
            values.put(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION, ((String)senz.getAttributes().get("permLoc")).equalsIgnoreCase("true") ? 1 : 0);
        }
        if (senz.getAttributes().containsKey("permMic")) {
            values.put(SenzorsDbContract.Permission.COLUMN_NAME_MIC, ((String)senz.getAttributes().get("permMic")).equalsIgnoreCase("true") ? 1 : 0);
        }
        values.put(SenzorsDbContract.Permission.COLOMN_NAME_USER, senz.getSender().getUsername());

        // Insert the new row, if fails throw an error
        db.insertOrThrow(SenzorsDbContract.Permission.TABLE_NAME, null, values);
    }

    /**
     * Add configurable permissions for senz user
     * @param senz
     */
    public void createConfigurablePermissionsForUser(Senz senz){
        Log.d(TAG, "Add New Permission: adding permission from - " + senz.getSender());
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // content values to inset
        ContentValues values = new ContentValues();

        values.put(SenzorsDbContract.PermissionConfiguration.COLUMN_UNIQUE_ID, senz.getId());

        if (senz.getAttributes().containsKey("permCam")) {
            values.put(SenzorsDbContract.PermissionConfiguration.COLUMN_NAME_CAMERA, ((String)senz.getAttributes().get("permCam")).equalsIgnoreCase("true") ? 1 : 0);
        }
        if (senz.getAttributes().containsKey("permLoc")) {
            values.put(SenzorsDbContract.PermissionConfiguration.COLUMN_NAME_LOCATION, ((String)senz.getAttributes().get("permLoc")).equalsIgnoreCase("true") ? 1 : 0);
        }
        if (senz.getAttributes().containsKey("permMic")) {
            values.put(SenzorsDbContract.PermissionConfiguration.COLUMN_NAME_MIC, ((String)senz.getAttributes().get("permMic")).equalsIgnoreCase("true") ? 1 : 0);
        }
        values.put(SenzorsDbContract.PermissionConfiguration.COLOMN_NAME_USER, senz.getSender().getUsername());

        // Insert the new row, if fails throw an error
        db.insertOrThrow(SenzorsDbContract.PermissionConfiguration.TABLE_NAME, null, values);

    }

    /**
     * Get ALl secrets to be display in chat list
     * @return sensor list
     */
    public ArrayList<Secret> getSecretz(User sender, User receiver) {
        Log.i(TAG, "Get Secrets: getting all secret messages, sender - " + sender.getUsername() + ", receiver - " + receiver.getUsername());
        ArrayList<Secret> secretList = new ArrayList();

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
        String query = "SELECT _id, uid, text, image, thumbnail, sender, receiver, deleted, delivered, delivery_fail, timestamp, sound " +
                "FROM secret WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) ORDER BY _id ASC";
        Cursor cursor = db.rawQuery(query,  new String[] {sender.getUsername(), receiver.getUsername(), receiver.getUsername(), sender.getUsername()});

        // secret attr
        String _secretId;
        String _secretText;
        String _secretImage;
        String _secretSender;
        String _secretReceiver;
        int _secretDelete;
        int _secretIsDelivered;
        int _secretDeliveryFailed;
        Long _secretTimestamp;
        String _thumbnail;
        String _secretSound;

        // extract attributes
        while (cursor.moveToNext()) {
            // get secret attributes
            _secretId = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_UNIQUE_ID));
            _secretText = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_TEXT));
            _secretImage = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLOMN_NAME_IMAGE));
            _secretSender = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_SENDER));
            _secretReceiver = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_RECEIVER));
            _secretIsDelivered = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_DELIVERED));
            _secretDeliveryFailed = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_DELIVERY_FAILED));
            _secretDelete = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_DELETE));
            _secretTimestamp = cursor.getLong(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_TIMESTAMP));
            _thumbnail = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLOMN_NAME_IMAGE_THUMB));
            _secretSound = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_SOUND));

            // create secret
            Secret secret = new Secret(_secretText, _secretImage, _thumbnail, new User("", _secretSender), new User("", _secretReceiver));
            secret.setDelete(_secretDelete == 1 ? true : false);
            secret.setIsDelivered(_secretIsDelivered == 1 ? true : false);
            secret.setDeliveryFailed(_secretDeliveryFailed == 1 ? true : false);
            secret.setTimeStamp(_secretTimestamp);
            secret.setID(_secretId);
            secret.setSound(_secretSound);

            // fill secret list
            secretList.add(secret);
        }

        // clean
        cursor.close();

        Log.d(TAG, "GetSecretz: secrets count " + secretList.size());
        return secretList;
    }

    /**
     * Get limited secrets, use for lazy loading
     * @param sender
     * @param receiver
     * @param limit
     * @return secrets list
     */
    public ArrayList<Secret> getSecretz(User sender, User receiver, Integer limit) {
        Log.i(TAG, "Get Secrets: getting all secret messages, sender - " + sender.getUsername() + ", receiver - " + receiver.getUsername());
        ArrayList<Secret> secretList = new ArrayList();

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
        String query = "SELECT _id, uid, text, image, thumbnail, sender, receiver, deleted, delivered, delivery_fail, timestamp, sound " +
                "FROM secret WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) ORDER BY _id ASC LIMIT ?";
        Cursor cursor = db.rawQuery(query,  new String[] {sender.getUsername(), receiver.getUsername(), receiver.getUsername(), sender.getUsername(), limit.toString() });

        // secret attr
        String _secretId;
        String _secretText;
        String _secretImage;
        String _secretSender;
        String _secretReceiver;
        int _secretDelete;
        int _secretIsDelivered;
        int _secretDeliveryFailed;
        Long _secretTimestamp;
        String _thumbnail;
        String _secretSound;

        // extract attributes
        while (cursor.moveToNext()) {
            HashMap<String, String> chatzAttributes = new HashMap<>();

            // get secret attributes
            _secretId = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_UNIQUE_ID));
            _secretText = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_TEXT));
            _secretImage = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLOMN_NAME_IMAGE));
            _secretSender = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_SENDER));
            _secretReceiver = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_RECEIVER));
            _secretDelete = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_DELETE));
            _secretIsDelivered = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_DELIVERED));
            _secretDeliveryFailed = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_DELIVERY_FAILED));
            _secretTimestamp = cursor.getLong(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_TIMESTAMP));
            _thumbnail = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLOMN_NAME_IMAGE_THUMB));
            _secretSound = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_SOUND));


            // create secret
            Secret secret = new Secret(_secretText, _secretImage, _thumbnail, new User("", _secretSender), new User("", _secretReceiver));
            secret.setDelete(_secretDelete == 1 ? true : false);
            secret.setIsDelivered(_secretIsDelivered == 1 ? true : false);
            secret.setDeliveryFailed(_secretDeliveryFailed == 1 ? true : false);
            secret.setTimeStamp(_secretTimestamp);
            secret.setID(_secretId);
            secret.setSound(_secretSound);

            // fill secret list
            secretList.add(secret);
        }

        // clean
        cursor.close();

        Log.d(TAG, "GetSecretz: secrets count " + secretList.size());
        return secretList;
    }


    /**
     * get list of secrets from given last timestamp
     * @param sender
     * @param receiver
     * @param timestamp timestamp after which all secrets shall be loaded
     * @return
     */
    public ArrayList<Secret> getSecretz(User sender, User receiver, Long timestamp) {
        Log.i(TAG, "Get Secrets: getting all secret messages, sender - " + sender.getUsername() + ", receiver - " + receiver.getUsername());
        ArrayList<Secret> secretList = new ArrayList();

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
        String query = "SELECT _id, uid, text, image, thumbnail, sender, receiver, deleted, delivered, delivery_fail, timestamp, sound " +
                "FROM secret WHERE ((sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?)) AND timestamp > ? ORDER BY _id ASC";
        Cursor cursor = db.rawQuery(query,  new String[] {sender.getUsername(), receiver.getUsername(), receiver.getUsername(), sender.getUsername(), timestamp.toString()});

        // secret attr
        String _secretId;
        String _secretText;
        String _secretImage;
        String _secretSender;
        String _secretReceiver;
        int _secretDelete;
        int _secretIsDelivered;
        int _secretDeliveryFailed;
        Long _secretTimestamp;
        String _thumbnail;
        String _secretSound;

        // extract attributes
        while (cursor.moveToNext()) {
            HashMap<String, String> chatzAttributes = new HashMap<>();

            // get secret attributes
            _secretId = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_UNIQUE_ID));
            _secretText = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_TEXT));
            _secretImage = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLOMN_NAME_IMAGE));
            _secretSender = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_SENDER));
            _secretReceiver = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_RECEIVER));
            _secretDelete = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_DELETE));
            _secretIsDelivered = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_DELIVERED));
            _secretDeliveryFailed = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_DELIVERY_FAILED));
            _secretTimestamp = cursor.getLong(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_TIMESTAMP));
            _thumbnail = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLOMN_NAME_IMAGE_THUMB));
            _secretSound = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_SOUND));


            // create secret
            Secret secret = new Secret(_secretText, _secretImage, _thumbnail, new User("", _secretSender), new User("", _secretReceiver));
            secret.setDelete(_secretDelete == 1 ? true : false);
            secret.setIsDelivered(_secretIsDelivered == 1 ? true : false);
            secret.setDeliveryFailed(_secretDeliveryFailed == 1 ? true : false);
            secret.setTimeStamp(_secretTimestamp);
            secret.setID(_secretId);
            secret.setSound(_secretSound);

            // fill secret list
            secretList.add(secret);
        }

        // clean
        cursor.close();

        Log.d(TAG, "GetSecretz: secrets count " + secretList.size());
        return secretList;
    }

    /**
     * Get all secrets expect those that belong to current user, also get the lastest message for each user/friend.
     * Information used to display under the Rahas tab as recent activity
     * @param sender
     * @return
     */
    public ArrayList<Secret> getAllOtherSercets(User sender) {
        ArrayList<Secret> secretList = new ArrayList();

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
        String query = "SELECT MAX(_id), text, image, thumbnail, sender, receiver, timestamp, sound FROM secret " +
                "WHERE sender != ? GROUP BY sender ORDER BY _id DESC";
        Cursor cursor = db.rawQuery(query,  new String[] {sender.getUsername()});

        // secret attr
        String _secretText;
        String _secretImage;
        String _secretSender;
        String _secretReceiver;
        String _secretSenderImage;
        String _secretThumbnail;
        Long _timeStamp;
        String _secretSound;

        // extract attributes
        while (cursor.moveToNext()) {
            HashMap<String, String> chatzAttributes = new HashMap<>();

            // get chatz attributes
            _secretText = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_TEXT));
            _secretImage = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLOMN_NAME_IMAGE));
            _secretSender = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_SENDER));
            _secretReceiver = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_RECEIVER));
            _secretThumbnail = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLOMN_NAME_IMAGE_THUMB));
            _timeStamp = cursor.getLong(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_TIMESTAMP));
            _secretSenderImage = getImageFromDB(_secretSender);
            _secretSound = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_SOUND));

            // create secret
            User senderUser = new User("", _secretSender);
            senderUser.setUserImage(_secretSenderImage);
            Secret secret = new Secret(_secretText, _secretImage, _secretThumbnail, senderUser, new User("", _secretReceiver));
            secret.setTimeStamp(_timeStamp);
            secret.setSound(_secretSound);

            // fill secret list
            secretList.add(secret);
        }

        // clean
        cursor.close();

        Log.d(TAG, "GetSecretz: secrets count " + secretList.size());
        return secretList;
    }

    public ArrayList<Secret> getAllOtherSercets(User sender, Long timestamp) {
        ArrayList<Secret> secretList = new ArrayList();

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
        String query = "SELECT MAX(_id), text, image, thumbnail, sender, receiver, timestamp, sound FROM secret " +
                "WHERE sender != ? AND timestamp > ? GROUP BY sender ORDER BY _id DESC";
        Cursor cursor = db.rawQuery(query,  new String[] {sender.getUsername(), timestamp.toString()});

        // secret attr
        String _secretText;
        String _secretImage;
        String _secretSender;
        String _secretReceiver;
        String _secretSenderImage;
        String _secretThumbnail;
        Long _timeStamp;
        String _secretSound;

        // extract attributes
        while (cursor.moveToNext()) {
            HashMap<String, String> chatzAttributes = new HashMap<>();

            // get chatz attributes
            _secretText = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_TEXT));
            _secretImage = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLOMN_NAME_IMAGE));
            _secretSender = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_SENDER));
            _secretReceiver = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_RECEIVER));
            _secretThumbnail = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLOMN_NAME_IMAGE_THUMB));
            _timeStamp = cursor.getLong(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_TIMESTAMP));
            _secretSenderImage = getImageFromDB(_secretSender);
            _secretSound = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Secret.COLUMN_NAME_SOUND));

            // create secret
            User senderUser = new User("", _secretSender);
            senderUser.setUserImage(_secretSenderImage);
            Secret secret = new Secret(_secretText, _secretImage, _secretThumbnail, senderUser, new User("", _secretReceiver));
            secret.setTimeStamp(_timeStamp);
            secret.setSound(_secretSound);

            // fill secret list
            secretList.add(secret);
        }

        // clean
        cursor.close();

        Log.d(TAG, "GetSecretz: secrets count " + secretList.size());
        return secretList;
    }

    /**
     * Get all sensors, two types of sensors here
     * 1. my sensors
     * 2. friends sensors
     *
     * @return sensor list
     */
    public List<Senz> getSenzes() {
        Log.d(TAG, "GetSensors: getting all sensor");
        List<Senz> sensorList = new ArrayList();

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();

        // join query to retrieve data
        String query = "SELECT senz._id, senz.name, senz.value, user._id, user.username " +
                "FROM senz " +
                "LEFT OUTER JOIN user " +
                "ON senz.user = user._id";
        Cursor cursor = db.rawQuery(query, null);

        // sensor/user attributes
        String _senzId;
        String _senzName;
        String _senzValue;
        String _userId;
        String _username;

        // extract attributes
        while (cursor.moveToNext()) {
            HashMap<String, String> senzAttributes = new HashMap<>();

            // get senz attributes
            _senzId = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Location._ID));
            _senzName = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Location.COLUMN_NAME_NAME));
            _senzValue = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.Location.COLUMN_NAME_VALUE));

            // get user attributes
            _userId = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User._ID));
            _username = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_USERNAME));

            senzAttributes.put(_senzName, _senzValue);

            // create senz
            Senz senz = new Senz();
            senz.setId(_senzId);
            senz.setAttributes(senzAttributes);
            senz.setSender(new User(_userId, _username));

            // fill senz list
            sensorList.add(senz);
        }

        // clean
        cursor.close();

        Log.d(TAG, "GetSensors: sensor count " + sensorList.size());
        return sensorList;
    }

    /**
     * Get User information like image and username with theirs permissions
     * Inner Join happening under the hood to combine the user table with permissions table
     * @param user
     * @return
     */
    public UserPermission getUserAndPermission(User user){
        UserPermission userPerm = null;

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();

        // join query to retrieve data
        String query = "SELECT permission.camera, permission.location, permission.mic, user.image " +
                "FROM user " +
                "INNER JOIN permission " +
                "ON user.username = permission.user WHERE permission.user = ?";
        Cursor cursor = db.rawQuery(query,  new String[] {user.getUsername()});

        // user attributes
        String _userimage;
        boolean _location;
        boolean _camera;
        boolean _mic;

        // extract attributes
        if (cursor.moveToFirst()) {

            // get permission attributes
            _camera = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA))== 1 ? true : false ;
            _location = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION)) == 1 ? true : false ;
            _mic = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_MIC)) == 1 ? true : false ;

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
     * @param user
     * @return
     */
    public UserPermission getUserPermission(User user){
        UserPermission userPerm = null;

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();

        // query
        String query = "SELECT permission.location, permission.camera, permission.mic " +
                "FROM permission " +
                "WHERE permission.user = ?";
        Cursor cursor = db.rawQuery(query,  new String[] {user.getUsername()});

        // user attributes
        String _username;
        boolean _location;
        boolean _camera;
        boolean _mic;
        String _userId;

        // extract attributes
        if (cursor.moveToFirst()) {

            // get permission attributes
            _location = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION)) == 1 ? true : false ;
            _camera = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA))== 1 ? true : false ;
            _mic = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_MIC))== 1 ? true : false ;

            // create senz
            userPerm = new UserPermission(user, _camera, _location, _mic);
        }

        // clean
        cursor.close();

        return userPerm;
    }

    /**
     * Get User configuarable permission only for the given user
     * @param user
     * @return
     */
    public UserPermission getUserConfigPermission(User user){
        UserPermission userPerm = null;

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();

        // join query to retrieve data
        String query = "SELECT permission_config.location, permission_config.camera, permission_config.mic " +
                "FROM permission_config " +
                "WHERE permission_config.user = ?";
        Cursor cursor = db.rawQuery(query,  new String[] {user.getUsername()});

        // user attributes
        boolean _location;
        boolean _camera;
        boolean _mic;

        // extract attributes
        if (cursor.moveToFirst()) {

            // get permission attributes
            _location = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION)) == 1 ? true : false ;
            _camera = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA))== 1 ? true : false ;
            _mic = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_MIC))== 1 ? true : false ;

            // create senz
            userPerm = new UserPermission(user, _camera, _location, _mic);
        }

        // clean
        cursor.close();

        return userPerm;
    }

    /**
     * Get list of all users/friends and also their permissions in a List
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
            _location = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION)) == 1 ? true : false ;
            _camera = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA))== 1 ? true : false ;
            _mic = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_MIC))== 1 ? true : false ;

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
     * Get list of all users/friends and also their configurable permissions in a List
     * @return
     */
    public List<UserPermission> getUsersAndTheirConfigurablePermissions() {
        List<UserPermission> permissionList = new ArrayList();

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();

        // join query to retrieve data
        String query = "SELECT user._id, user.username, permission_config.location, permission_config.camera, permission_config.mic " +
                "FROM user " +
                "INNER JOIN permission_config " +
                "ON user.username = permission_config.user";
        Cursor cursor = db.rawQuery(query, null);

        // sensor/user attributes
        String _username;
        boolean _location;
        boolean _camera;
        boolean _mic;
        String _userId;

        // extract attributes
        while (cursor.moveToNext()) {

            // get permission attributes
            _location = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_LOCATION)) == 1 ? true : false ;
            _camera = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_CAMERA))== 1 ? true : false ;
            _mic = cursor.getInt(cursor.getColumnIndex(SenzorsDbContract.Permission.COLUMN_NAME_MIC))== 1 ? true : false ;

            // get user attributes
            _userId = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User._ID));
            _username = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_USERNAME));

            // create senz
            User user = new User(_userId, _username);
            UserPermission userPerm = new UserPermission(user, _camera, _location, _mic);

            // fill senz list
            permissionList.add(userPerm);
        }

        // clean
        cursor.close();

        return permissionList;
    }

    /**
     *
     * @return
     */
    public List<User> readAllUsers() {
        Log.d(TAG, "GetSensors: getting all sensor");
        List<User> userList = new ArrayList<User>();

        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();
        Cursor cursor = db.query(SenzorsDbContract.User.TABLE_NAME, null, null, null, null, null, null);

        // user attributes
        String _id;
        String _username;

        // extract attributes
        while (cursor.moveToNext()) {
            _id = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User._ID));
            _username = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.User.COLUMN_NAME_USERNAME));

            // we don't add mysensors user as a friend(its a server :))
            if (!_username.equalsIgnoreCase("mysensors")) userList.add(new User(_id, _username));
        }

        // clean
        cursor.close();

        Log.d(TAG, "user count " + userList.size());

        return userList;
    }

    /**
     * Delete sec from database,
     *
     * @param
     */
    public void deleteSecret(Secret secret) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        db.beginTransaction();

        // delete senz of given user
        db.delete(SenzorsDbContract.Secret.TABLE_NAME,
                SenzorsDbContract.Secret.COLUMN_UNIQUE_ID + "=?",
                new String[]{secret.getID()});

        db.endTransaction();
    }

    /**
     * Delete senz from database,
     *
     * @param senz senz
     */
    public void deleteSenz(Senz senz) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getWritableDatabase();

        // delete senz of given user
        db.delete(SenzorsDbContract.Location.TABLE_NAME,
                SenzorsDbContract.Location.COLUMN_NAME_USER + "=?" + " AND " +
                        SenzorsDbContract.Location.COLUMN_NAME_NAME + "=?",
                new String[]{senz.getSender().getId(), senz.getAttributes().keySet().iterator().next()});
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
        }
        finally {
            db.endTransaction();
        }
    }

    public String getImageFromDB(String username) {
        SQLiteDatabase db = SenzorsDbHelper.getInstance(context).getReadableDatabase();

        // query
        String query = "SELECT user.image " +
                "FROM user " +
                "WHERE user.username = ?";
        Cursor cursor = db.rawQuery(query,  new String[] {username});

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
