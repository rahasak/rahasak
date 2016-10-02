package com.score.chatz.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.score.chatz.R;
import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.pojo.Secret;
import com.score.chatz.pojo.BitmapTaskParams;
import com.score.chatz.asyncTasks.BitmapWorkerTask;
import com.score.chatz.utils.PreferenceUtils;
import com.score.chatz.utils.TimeUtils;
import com.score.senzc.pojos.User;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by lakmalcaldera on 8/19/16.
 */
public class LastItemChatListAdapter extends ArrayAdapter<Secret> {

    private static final String TAG = ChatFragmentListAdapter.class.getName();
    Context context;
    ArrayList<Secret> userSecretList;
    static final int TEXT_MESSAGE = 0;
    static final int IMAGE_MESSAGE = 1;
    static final int SOUND_MESSAGE = 2;
    static User currentUser;
    private LayoutInflater mInflater;
    private Typeface typeface;

    public LastItemChatListAdapter(Context _context, ArrayList<Secret> secretList) {
        super(_context, R.layout.single_user_card_row, R.id.user_name, secretList);
        context = _context;
        userSecretList = secretList;
        try {
            currentUser = PreferenceUtils.getUser(getContext());
        } catch (NoUserException e) {
            e.printStackTrace();
        }
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        typeface = Typeface.createFromAsset(context.getAssets(), "fonts/GeosansLight.ttf");
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        Secret secret = (Secret) getItem(position);
        //if(((Secret)getItem(position)).getImage() != null){
        if (((Secret) getItem(position)).getType().equalsIgnoreCase("IMAGE")) {
            return IMAGE_MESSAGE;
        } else if (((Secret) getItem(position)).getType().equalsIgnoreCase("SOUND")) {
            return SOUND_MESSAGE;
        } else {
            return TEXT_MESSAGE;
        }
    }


    /**
     * Create list row viewv
     *
     * @param i         index
     * @param view      current list item view
     * @param viewGroup parent
     * @return view
     */
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        // A ViewHolder keeps references to children views to avoid unnecessary calls
        // to findViewById() on each row.
        final ViewHolder holder;
        final Secret secret = (Secret) getItem(i);
        int type = getItemViewType(i);
        //Log.i("SECRETS" ,"Secret : Text - " + secret.getText() + ", Sender - " + secret.getSender().getUsername() + ", Receiver - " + secret.getReceiver().getUsername());
        if (view == null || (view != null && ((ViewHolder) view.getTag()).messageType != type)) {
            //inflate sensor list row layout
            //create view holder to store reference to child views
            holder = new ViewHolder();
            switch (type) {
                case IMAGE_MESSAGE:
                    view = mInflater.inflate(R.layout.rahas_image_row_layout, viewGroup, false);
                    holder.image = (ImageView) view.findViewById(R.id.image);
                    holder.sender = (TextView) view.findViewById(R.id.sender);
                    holder.sentTime = (TextView) view.findViewById(R.id.sent_time);
                    holder.userImage = (com.github.siyamed.shapeimageview.RoundedImageView) view.findViewById(R.id.user_image);
                    holder.messageType = IMAGE_MESSAGE;
                    break;
                case TEXT_MESSAGE:
                    view = mInflater.inflate(R.layout.rahas_row_layout, viewGroup, false);
                    holder.message = (TextView) view.findViewById(R.id.message);
                    holder.sender = (TextView) view.findViewById(R.id.sender);
                    holder.sentTime = (TextView) view.findViewById(R.id.sent_time);
                    holder.userImage = (com.github.siyamed.shapeimageview.RoundedImageView) view.findViewById(R.id.user_image);
                    holder.messageType = TEXT_MESSAGE;
                    break;
                case SOUND_MESSAGE:
                    view = mInflater.inflate(R.layout.rahas_sound_row_layout, viewGroup, false);
                    holder.sender = (TextView) view.findViewById(R.id.sender);
                    holder.sentTime = (TextView) view.findViewById(R.id.sent_time);
                    holder.messageType = SOUND_MESSAGE;
                    break;
            }
            view.setTag(holder);
        } else {
            //get view holder back_icon
            holder = (ViewHolder) view.getTag();
        }
        holder.sentTime = (TextView) view.findViewById(R.id.sent_time);
        setUpRow(i, secret, view, holder);
        return view;
    }

    private void setUpRow(int i, Secret secret, View view, ViewHolder viewHolder) {
        // enable share and change color of view
        if (viewHolder.messageType == TEXT_MESSAGE) {
            viewHolder.message.setText(secret.getBlob());
            viewHolder.message.setTypeface(typeface, Typeface.NORMAL);
        } else if (viewHolder.messageType == SOUND_MESSAGE) {
            //Nothing to do here!!
        } else {
            if (secret.getBlob() != null) {
                loadBitmap(secret.getBlob(), viewHolder.image);
            }
        }

        if (secret.getTimeStamp() != null) {
            Timestamp timestamp = new Timestamp(secret.getTimeStamp());
            Date date = new Date(timestamp.getTime());
            viewHolder.sentTime.setText(TimeUtils.getTimeInWords(date));
            viewHolder.sentTime.setTypeface(typeface, Typeface.NORMAL);
        }


        User selectedUser = secret.getUser();

        viewHolder.sender.setText("@" + selectedUser.getUsername());
        viewHolder.sender.setTypeface(typeface, Typeface.NORMAL);
        //Extracting user image
        if (selectedUser.getUserImage() != null) {
            loadBitmap(selectedUser.getUserImage(), viewHolder.userImage);
        }

    }

    private void loadBitmap(String data, ImageView imageView) {
        BitmapWorkerTask task = new BitmapWorkerTask(imageView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (new BitmapTaskParams(data, 100, 100)));
        else
            task.execute(new BitmapTaskParams(data, 100, 100));
    }

    /**
     * Keep reference to children view to avoid unnecessary calls
     */
    static class ViewHolder {
        TextView message;
        TextView sender;
        TextView sentTime;
        Integer messageType;
        ImageView image;
        com.github.siyamed.shapeimageview.RoundedImageView userImage;

    }
}
