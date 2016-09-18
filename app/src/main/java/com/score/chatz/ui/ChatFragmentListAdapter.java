package com.score.chatz.ui;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.score.chatz.R;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.pojo.Secret;
import com.score.chatz.utils.AnimationUtils;
import com.score.chatz.utils.AudioUtils;
import com.score.chatz.pojo.BitmapTaskParams;
import com.score.chatz.asyncTasks.BitmapWorkerTask;
import com.score.chatz.utils.CameraUtils;
import com.score.chatz.utils.PreferenceUtils;
import com.score.chatz.utils.SecretsUtil;
import com.score.chatz.utils.SenzUtils;
import com.score.chatz.utils.TimeUtils;
import com.score.senzc.pojos.User;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by Lakmal on 8/9/16.
 */
public class ChatFragmentListAdapter extends ArrayAdapter<Secret> {
    private static final String TAG = ChatFragmentListAdapter.class.getName();
    Context context;
    List<Secret> userSecretList;
    static final int MY_MESSAGE_TYPE = 0;
    static final int NOT_MY_MESSAGE_TYPE = 1;
    static final int MY_PHOTO_TYPE = 2;
    static final int NOT_MY_PHOTO_TYPE = 3;
    static final int NOT_MY_SOUND_TYPE = 4;
    static final int MY_SOUND_TYPE = 5;
    static User currentUser;
    private LayoutInflater mInflater;
    private Integer DISPLAY_ITEM_COUNT;

    public ChatFragmentListAdapter(Context _context, List<Secret> secretList, Integer numberOfItemToDisplay) {
        super(_context, R.layout.single_user_card_row, R.id.user_name, secretList);
        context = _context;
        userSecretList = secretList;
        try {
            currentUser = PreferenceUtils.getUser(getContext());
        }catch (NoUserException e) {
            e.printStackTrace();
        }
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        DISPLAY_ITEM_COUNT = numberOfItemToDisplay;

    }

    @Override
    public int getViewTypeCount() {
        return 6;
    }

    @Override
    public int getItemViewType(int position) {
        //Log.i(TAG, "WHO IS SENDER: " + ((Secret)getItem(position)).getSender().getUsername() + ", currentUser: " + currentUser.getUsername());
        if(!((Secret)getItem(position)).getWho().getUsername().equalsIgnoreCase(currentUser.getUsername()) && ((Secret)getItem(position)).getType().equalsIgnoreCase("SOUND")){
            return NOT_MY_SOUND_TYPE;
        }else if(((Secret)getItem(position)).getWho().getUsername().equalsIgnoreCase(currentUser.getUsername()) && ((Secret)getItem(position)).getType().equalsIgnoreCase("SOUND")){
            return MY_SOUND_TYPE;
        }else if(((Secret)getItem(position)).getWho().getUsername().equalsIgnoreCase(currentUser.getUsername()) && !(((Secret)getItem(position)).getType().equalsIgnoreCase("IMAGE"))){
            return MY_MESSAGE_TYPE;
        }else if(!((Secret)getItem(position)).getWho().getUsername().equalsIgnoreCase(currentUser.getUsername()) && !(((Secret)getItem(position)).getType().equalsIgnoreCase("IMAGE"))){
            return NOT_MY_MESSAGE_TYPE;
        }else if(((Secret)getItem(position)).getWho().getUsername().equalsIgnoreCase(currentUser.getUsername()) && (((Secret)getItem(position)).getType().equalsIgnoreCase("IMAGE"))){
            return MY_PHOTO_TYPE;
        }else {
            //holder.image = (ImageView) view.findViewById(R.id.message);
            return NOT_MY_PHOTO_TYPE;
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
                case MY_MESSAGE_TYPE:
                    view = mInflater.inflate(R.layout.my_message_layout, viewGroup, false);
                    holder.message = (TextView) view.findViewById(R.id.message);
                    holder.sender = (TextView) view.findViewById(R.id.sender);
                    holder.status = (TextView) view.findViewById(R.id.deleviered_message);
                    holder.messageType = MY_MESSAGE_TYPE;
                    break;
                case NOT_MY_MESSAGE_TYPE:
                    view = mInflater.inflate(R.layout.not_my_message_layout, viewGroup, false);
                    holder.message = (TextView) view.findViewById(R.id.message);
                    holder.sender = (TextView) view.findViewById(R.id.sender);
                    holder.status = (TextView) view.findViewById(R.id.deleviered_message);
                    holder.messageType = NOT_MY_MESSAGE_TYPE;
                    break;
                case MY_PHOTO_TYPE:
                    view = mInflater.inflate(R.layout.my_photo_layout, viewGroup, false);
                    holder.image = (ImageView) view.findViewById(R.id.play);
                    holder.sender = (TextView) view.findViewById(R.id.sender);
                    holder.status = (TextView) view.findViewById(R.id.deleviered_message);
                    holder.messageType = MY_PHOTO_TYPE;
                    break;
                case NOT_MY_PHOTO_TYPE:
                    view = mInflater.inflate(R.layout.not_my_photo_layout, viewGroup, false);
                    holder.image = (ImageView) view.findViewById(R.id.not_my_image);
                    holder.sender = (TextView) view.findViewById(R.id.sender);
                    holder.status = (TextView) view.findViewById(R.id.deleviered_message);
                    holder.messageType = NOT_MY_PHOTO_TYPE;
                    break;
                case MY_SOUND_TYPE:
                    view = mInflater.inflate(R.layout.my_sound_layout, viewGroup, false);
                    holder.image = (ImageView) view.findViewById(R.id.play);
                    holder.sender = (TextView) view.findViewById(R.id.sender);
                    holder.status = (TextView) view.findViewById(R.id.deleviered_message);
                    holder.messageType = MY_SOUND_TYPE;
                    break;
                case NOT_MY_SOUND_TYPE:
                    view = mInflater.inflate(R.layout.not_my_sound_layout, viewGroup, false);
                    holder.image = (ImageView) view.findViewById(R.id.play);
                    holder.sender = (TextView) view.findViewById(R.id.sender);
                    holder.status = (TextView) view.findViewById(R.id.deleviered_message);
                    holder.messageType = NOT_MY_SOUND_TYPE;
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

    private void setUpRow(int i, final Secret secret, View view, ViewHolder viewHolder) {
        // enable share and change color of view
        viewHolder.sender.setText("@"+secret.getWho().getUsername());
        if (viewHolder.messageType == NOT_MY_MESSAGE_TYPE || viewHolder.messageType == MY_MESSAGE_TYPE){
            viewHolder.message.setText(secret.getBlob());
        }else if (viewHolder.messageType == NOT_MY_PHOTO_TYPE || viewHolder.messageType == MY_PHOTO_TYPE){
            /*viewHolder.image.setImageResource(R.drawable.confidential);*/
            viewHolder.image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, PhotoFullScreenActivity.class);
                    //intent.putExtra("IMAGE", secret.getImage());
                    String uid = SenzUtils.getUniqueRandomNumber().toString();
                    intent.putExtra("IMAGE_RES_ID", uid);
                    CameraUtils.savePhotoCache(uid, CameraUtils.getBitmapFromBytes(Base64.encode(secret.getBlob().getBytes(), 0)), getContext());
                    context.startActivity(intent);
                }
            });
            if(secret.getBlob() != null) {
                loadBitmap(secret.getBlob(), viewHolder.image);
            }
        }

        if(secret.getTimeStamp() != null){
            Timestamp timestamp = new Timestamp(secret.getTimeStamp());
            Date date = new Date(timestamp.getTime());
            viewHolder.sentTime.setText(TimeUtils.getTimeInWords(date));
        }


        if (viewHolder.messageType == NOT_MY_SOUND_TYPE || viewHolder.messageType == MY_SOUND_TYPE) {
            viewHolder.image.setImageResource(R.drawable.play_recording);
            viewHolder.image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AudioUtils.play(Base64.decode(secret.getBlob(), 0), getContext());
                }
            });
        }

        if(!currentUser.getUsername().equalsIgnoreCase(secret.getWho().getUsername())){
            viewHolder.status.setVisibility(View.INVISIBLE);
        }

        if(secret.isDelivered()){
            viewHolder.status.setText("Message sent!!!");
        }else{
            viewHolder.status.setText("Message sending...");
        }

        if(secret.isDeliveryFailed()){
            viewHolder.status.setText("Message failed to deliver!!!");
        }

        /*if(secret.getSeenTimeStamp() == 0 || secret.getSeenTimeStamp() == null){
            Animation bottomUp = android.view.animation.AnimationUtils.loadAnimation(getContext(),
                    R.anim.message_in);
            view.startAnimation(bottomUp);
            markMessagesAsSeen(secret);
        }else if(!SecretsUtil.isSecretToBeShown(secret) && !(i >= (userSecretList.size() - DISPLAY_ITEM_COUNT - 1))){
            Log.i(TAG, "DELETING INDEX OF VIEW CHAT ACTIVITY - " + i + ", secret - " + secret.getBlob());
            Animation fadeOut = android.view.animation.AnimationUtils.loadAnimation(getContext(),
                    R.anim.fade_out);
            view.startAnimation(fadeOut);
        }*/




        viewHolder.sender.setText(secret.getWho().getUsername());
    }

    private void markMessagesAsSeen(final Secret secret){

                if(secret.getSeenTimeStamp() == null || secret.getSeenTimeStamp() == 0){
                    secret.setSeenTimeStamp(System.currentTimeMillis() + 100);
                    new SenzorsDbSource(context).updateSeenTimestamp(secret);
                }
    }

    private void deleteSecret(final Secret secret){
            new SenzorsDbSource(context).deleteSecret(secret);
    }

    private void loadBitmap(String data, ImageView imageView) {
        BitmapWorkerTask task = new BitmapWorkerTask(imageView);
        //task.execute(new BitmapTaskParams(data, 100, 100));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (new BitmapTaskParams(data, 400, 400)));
        else
            task.execute(new BitmapTaskParams(data, 400, 400));
    }

    /**
     * Keep reference to children view to avoid unnecessary calls
     */
    static class ViewHolder {
        TextView status;
        TextView message;
        TextView sender;
        TextView sentTime;
        Integer messageType;
        ImageView image;

    }
}
