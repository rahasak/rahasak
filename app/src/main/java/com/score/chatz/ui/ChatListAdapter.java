package com.score.chatz.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.score.chatz.R;
import com.score.chatz.application.IntentProvider;
import com.score.chatz.asyncTasks.BitmapWorkerTask;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.pojo.BitmapTaskParams;
import com.score.chatz.pojo.Secret;
import com.score.chatz.utils.LimitedList;
import com.score.chatz.utils.PreferenceUtils;
import com.score.chatz.utils.TimeUtils;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;

import java.util.Date;
import java.util.HashMap;

/**
 * Created by eranga on 9/28/16
 */
class ChatListAdapter extends BaseAdapter {

    private Context context;
    private LimitedList<Secret> secretList;
    private SenzorsDbSource dbSource;

    private Typeface typeface;

    ChatListAdapter(Context context, LimitedList<Secret> secretList) {
        this.context = context;
        this.secretList = secretList;
        this.dbSource = new SenzorsDbSource(context);

        typeface = Typeface.createFromAsset(context.getAssets(), "fonts/GeosansLight.ttf");
    }

    @Override
    public int getCount() {
        return secretList.size();
    }

    @Override
    public Object getItem(int position) {
        return secretList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final ViewHolder holder;
        final Secret secret = (Secret) getItem(position);

        if (view == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.chat_view_row_layout, parent, false);

            holder = new ViewHolder();

            holder.imageLayout = (RelativeLayout) view.findViewById(R.id.chat_image);
            holder.soundLayout = (RelativeLayout) view.findViewById(R.id.chat_sound);
            holder.messageLayout = (LinearLayout) view.findViewById(R.id.chat_message);
            holder.myStatusLayout = (FrameLayout) view.findViewById(R.id.my_status);
            holder.friendStatusLayout = (FrameLayout) view.findViewById(R.id.friend_status);

            holder.myImageView = (ImageView) view.findViewById(R.id.my_image);
            holder.friendImageView = (ImageView) view.findViewById(R.id.friend_image);

            holder.mySoundView = (ImageView) view.findViewById(R.id.my_sound);
            holder.friendSoundView = (ImageView) view.findViewById(R.id.friend_sound);

            holder.myMessageView = (TextView) view.findViewById(R.id.my_message);
            holder.friendMessageView = (TextView) view.findViewById(R.id.friend_message);

            holder.mySentTimeView = (TextView) view.findViewById(R.id.my_sent_time);
            holder.friendSentTimeView = (TextView) view.findViewById(R.id.friend_sent_time);

            holder.mySentIconView = (ImageView) view.findViewById(R.id.my_delivered_message);
            holder.friendSentIconView = (ImageView) view.findViewById(R.id.friend_delivered_message);

            holder.myPendingIconView = (ImageView) view.findViewById(R.id.my_not_delivered_message);
            holder.friendPendingIconView = (ImageView) view.findViewById(R.id.friend_not_delivered_message);

            // set fonts
            holder.myMessageView.setTypeface(typeface, Typeface.BOLD);
            holder.friendMessageView.setTypeface(typeface, Typeface.BOLD);
            holder.mySentTimeView.setTypeface(typeface);
            holder.friendSentTimeView.setTypeface(typeface);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        setupRow(secret, holder);
        return view;
    }

    private void setupRow(final Secret secret, ViewHolder holder) {
        if (secret.getType().equalsIgnoreCase("TEXT")) {
            holder.imageLayout.setVisibility(View.GONE);
            holder.soundLayout.setVisibility(View.GONE);
            holder.messageLayout.setVisibility(View.VISIBLE);
            if (secret.isSender()) {
                // TODO
                holder.friendStatusLayout.setVisibility(View.VISIBLE);
                holder.myStatusLayout.setVisibility(View.GONE);

                holder.myMessageView.setVisibility(View.GONE);
                holder.friendMessageView.setVisibility(View.VISIBLE);
                holder.friendMessageView.setText(secret.getBlob());
            } else {
                // TODO
                holder.friendStatusLayout.setVisibility(View.GONE);
                holder.myStatusLayout.setVisibility(View.VISIBLE);

                holder.friendMessageView.setVisibility(View.GONE);
                holder.myMessageView.setVisibility(View.VISIBLE);
                holder.myMessageView.setText(secret.getBlob());
            }
        } else if (secret.getType().equalsIgnoreCase("IMAGE")) {
            holder.soundLayout.setVisibility(View.GONE);
            holder.messageLayout.setVisibility(View.GONE);
            holder.imageLayout.setVisibility(View.VISIBLE);

            // TODO
            holder.friendStatusLayout.setVisibility(View.GONE);
            holder.myStatusLayout.setVisibility(View.GONE);
            if (secret.isSender()) {
                holder.myImageView.setVisibility(View.GONE);
                holder.friendImageView.setVisibility(View.VISIBLE);
                new BitmapWorkerTask(holder.friendImageView).execute(new BitmapTaskParams(secret.getBlob(), 400, 400));
            } else {
                holder.myImageView.setVisibility(View.VISIBLE);
                holder.friendImageView.setVisibility(View.GONE);
                new BitmapWorkerTask(holder.myImageView).execute(new BitmapTaskParams(secret.getBlob(), 400, 400));
            }
        } else if (secret.getType().equalsIgnoreCase("SOUND")) {
            // TODO
            holder.friendStatusLayout.setVisibility(View.GONE);
            holder.myStatusLayout.setVisibility(View.GONE);

            holder.messageLayout.setVisibility(View.GONE);
            holder.imageLayout.setVisibility(View.GONE);
            holder.soundLayout.setVisibility(View.VISIBLE);
            if (secret.isSender()) {
                holder.mySoundView.setVisibility(View.GONE);
                holder.friendSoundView.setVisibility(View.VISIBLE);
            } else {
                holder.mySoundView.setVisibility(View.VISIBLE);
                holder.friendSoundView.setVisibility(View.GONE);
            }
        } else if (secret.getType().equalsIgnoreCase("MISSED_IMAGE")) {
            holder.soundLayout.setVisibility(View.GONE);
            holder.messageLayout.setVisibility(View.GONE);
            holder.imageLayout.setVisibility(View.VISIBLE);

            // TODO
            holder.friendStatusLayout.setVisibility(View.GONE);
            holder.myStatusLayout.setVisibility(View.GONE);
            if (secret.isSender()) {
                holder.myImageView.setVisibility(View.GONE);
                holder.friendImageView.setVisibility(View.VISIBLE);
                holder.friendImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.missed_selfie_call));
            }
        } else if (secret.getType().equalsIgnoreCase("MISSED_SOUND")) {
            holder.soundLayout.setVisibility(View.GONE);
            holder.messageLayout.setVisibility(View.GONE);
            holder.imageLayout.setVisibility(View.VISIBLE);

            // TODO
            holder.friendStatusLayout.setVisibility(View.GONE);
            holder.myStatusLayout.setVisibility(View.GONE);
            if (secret.isSender()) {
                holder.myImageView.setVisibility(View.GONE);
                holder.friendImageView.setVisibility(View.VISIBLE);
                holder.friendImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.missed_audio_call));
            }
        }

        // set status and time
        if (secret.isSender()) {
            holder.myStatusLayout.setVisibility(View.GONE);
            holder.friendStatusLayout.setVisibility(View.VISIBLE);
            holder.friendSentIconView.setVisibility(View.GONE);
            holder.friendPendingIconView.setVisibility(View.GONE);

            if (secret.getTimeStamp() != null) {
                java.sql.Timestamp timestamp = new java.sql.Timestamp(secret.getTimeStamp());
                Date date = new Date(timestamp.getTime());
                holder.friendSentTimeView.setText(TimeUtils.getTimeInWords(date));
            }
        } else {
            holder.myStatusLayout.setVisibility(View.VISIBLE);
            holder.friendStatusLayout.setVisibility(View.GONE);
            if (secret.isDelivered()) {
                holder.mySentIconView.setVisibility(View.VISIBLE);
                holder.myPendingIconView.setVisibility(View.GONE);
            } else {
                holder.mySentIconView.setVisibility(View.GONE);
                holder.myPendingIconView.setVisibility(View.VISIBLE);
            }

            if (secret.getTimeStamp() != null) {
                java.sql.Timestamp timestamp = new java.sql.Timestamp(secret.getTimeStamp());
                Date date = new Date(timestamp.getTime());
                holder.mySentTimeView.setText(TimeUtils.getTimeInWords(date));
            }
        }

        // set click listeners
        holder.friendImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PhotoFullScreenActivity.class);
                intent.putExtra("IMAGE", secret.getBlob());
                context.startActivity(intent);
            }
        });

        holder.myImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PhotoFullScreenActivity.class);
                intent.putExtra("IMAGE", secret.getBlob());
                context.startActivity(intent);
            }
        });

        holder.friendSoundView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, AudioFullScreenActivity.class);
                intent.putExtra("SOUND", secret.getBlob());
                context.startActivity(intent);
            }
        });

        holder.mySoundView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, AudioFullScreenActivity.class);
                intent.putExtra("SOUND", secret.getBlob());
                context.startActivity(intent);
            }
        });

        holder.friendImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                HashMap<String, String> senzAttributes;
                if (((ImageView) v).getDrawable().getConstantState().equals(context.getResources().getDrawable(R.drawable.missed_selfie_call).getConstantState())) {
                    // create senz attributes
                    senzAttributes = new HashMap<>();
                    senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
                    senzAttributes.put("cam", "");
                    senzAttributes.put("uid", secret.getId());

                    // new senz
                    String id = "_ID";
                    String signature = "_SIGNATURE";
                    SenzTypeEnum senzType = SenzTypeEnum.GET;
                    Senz senz = null;
                    try {
                        senz = new Senz(id, signature, senzType, secret.getUser(), PreferenceUtils.getUser(context), senzAttributes);
                    } catch (NoUserException ex) {
                        ex.printStackTrace();
                    }

                    Intent intent = IntentProvider.getCameraIntent(context);
                    intent.putExtra("Senz", senz);
                    intent.putExtra("MISSED_SELFIE_CALL", "MISSED_SELFIE_CALL");
                    context.startActivity(intent);
                } else if (((ImageView) v).getDrawable().getConstantState().equals(context.getResources().getDrawable(R.drawable.missed_audio_call).getConstantState())) {
                    // create senz attributes
                    senzAttributes = new HashMap<>();
                    senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
                    senzAttributes.put("mic", "");
                    senzAttributes.put("uid", secret.getId());

                    // new senz
                    String id = "_ID";
                    String signature = "_SIGNATURE";
                    SenzTypeEnum senzType = SenzTypeEnum.GET;
                    Senz senz = null;
                    try {
                        senz = new Senz(id, signature, senzType, secret.getUser(), PreferenceUtils.getUser(context), senzAttributes);
                    } catch (NoUserException ex) {
                        ex.printStackTrace();
                    }

                    Intent intent = new Intent();
                    intent.setClass(context, RecordingActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("Senz", senz);
                    intent.putExtra("MISSED_AUDIO_CALL", "MISSED_AUDIO_CALL");
                    context.startActivity(intent);
                }

                // Remove missed call from list and db so you can't use it again and again
                secretList.remove(secret);
                notifyDataSetChanged();
                dbSource.deleteSecret(secret);

            }
        });
    }

    /**
     * Keep reference to children view to avoid unnecessary calls
     */
    private static class ViewHolder {
        RelativeLayout imageLayout;
        RelativeLayout soundLayout;
        LinearLayout messageLayout;
        FrameLayout myStatusLayout;
        FrameLayout friendStatusLayout;

        ImageView myImageView;
        ImageView friendImageView;

        ImageView mySoundView;
        ImageView friendSoundView;

        TextView myMessageView;
        TextView friendMessageView;

        TextView mySentTimeView;
        TextView friendSentTimeView;

        ImageView mySentIconView;
        ImageView friendSentIconView;

        ImageView myPendingIconView;
        ImageView friendPendingIconView;
    }

}
