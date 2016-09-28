package com.score.chatz.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.score.chatz.R;
import com.score.chatz.asyncTasks.BitmapWorkerTask;
import com.score.chatz.pojo.BitmapTaskParams;
import com.score.chatz.pojo.Secret;
import com.score.chatz.utils.TimeUtils;

import java.util.Date;
import java.util.List;

/**
 * Created by eranga on 9/28/16
 */
class ChatListAdapter extends BaseAdapter {

    private Context context;
    private List<Secret> secretList;

    ChatListAdapter(Context context, List<Secret> secretList) {
        this.context = context;
        this.secretList = secretList;
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
        // A ViewHolder keeps references to children views to avoid unnecessary calls
        // to findViewById() on each row
        final ViewHolder holder;
        final Secret secret = (Secret) getItem(position);

        //if (view == null) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = getInflateLayout(layoutInflater, parent, secret);

        holder = new ViewHolder();
        setupView(view, holder, secret);
        view.setTag(holder);

        //} else {
        //    // get view holder back
        //    holder = (ViewHolder) view.getTag();
        //}

        // bind view holder content
        setupHolder(holder, secret);

        return view;
    }

    /**
     * Keep reference to children view to avoid unnecessary calls
     */
    private static class ViewHolder {
        TextView sender;
        TextView status;
        TextView sentTime;
        ImageView image;
        TextView message;
    }

    private View getInflateLayout(LayoutInflater layoutInflater, ViewGroup parent, Secret secret) {
        if (secret.isSender()) {
            // send from user
            if (secret.getType().equalsIgnoreCase("TEXT")) {
                return layoutInflater.inflate(R.layout.my_message_layout, parent, false);
            } else if (secret.getType().equalsIgnoreCase("IMAGE")) {
                return layoutInflater.inflate(R.layout.my_photo_layout, parent, false);
            } else {
                return layoutInflater.inflate(R.layout.my_sound_layout, parent, false);
            }
        } else {
            // my secret
            if (secret.getType().equalsIgnoreCase("TEXT")) {
                return layoutInflater.inflate(R.layout.not_my_message_layout, parent, false);
            } else if (secret.getType().equalsIgnoreCase("IMAGE")) {
                return layoutInflater.inflate(R.layout.not_my_photo_layout, parent, false);
            } else {
                return layoutInflater.inflate(R.layout.not_my_sound_layout, parent, false);
            }
        }
    }

    private void setupView(View view, ViewHolder holder, Secret secret) {
        holder.sender = (TextView) view.findViewById(R.id.sender);
        holder.status = (TextView) view.findViewById(R.id.deleviered_message);
        holder.sentTime = (TextView) view.findViewById(R.id.sent_time);

        if (secret.getType().equalsIgnoreCase("TEXT")) {
            // text
            holder.message = (TextView) view.findViewById(R.id.message);
        } else if (secret.getType().equalsIgnoreCase("IMAGE")) {
            // photo
            holder.image = (ImageView) view.findViewById(R.id.image);
        } else {
            holder.image = (ImageView) view.findViewById(R.id.sound);
        }
    }

    private void setupHolder(ViewHolder holder, final Secret secret) {
        // set sender
        if (secret.isSender()) holder.sender.setText("@" + secret.getUser().getUsername());
        else holder.sender.setText("@" + "Me");

        // set status
        if (!secret.isSender() && secret.getType().equalsIgnoreCase("TEXT")) {
            if (secret.isDelivered()) holder.status.setText("Delivered ");
            else if (secret.isDeliveryFailed()) holder.status.setText("Delivery fail ");
            else holder.status.setText("Sending...");
        } else {
            holder.status.setVisibility(View.INVISIBLE);
        }

        // set sent time
        if (secret.getTimeStamp() != null) {
            java.sql.Timestamp timestamp = new java.sql.Timestamp(secret.getTimeStamp());
            Date date = new Date(timestamp.getTime());
            holder.sentTime.setText(TimeUtils.getTimeInWords(date));
        }

        // set message, image, sound
        if (secret.getType().equalsIgnoreCase("TEXT")) {
            holder.message.setText(secret.getBlob());
        } else if (secret.getType().equalsIgnoreCase("IMAGE")) {
            if (secret.getBlob() != null) {
                new BitmapWorkerTask(holder.image).execute(new BitmapTaskParams(secret.getBlob(), 400, 400));

                // set click listener
                holder.image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, PhotoFullScreenActivity.class);
                        intent.putExtra("IMAGE", secret.getBlob());
                        context.startActivity(intent);
                    }
                });
            }
        } else {
            if (secret.getBlob() != null) {
                holder.image.setImageResource(R.drawable.play_recording);

                // set click listener
                holder.image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, AudioFullScreenActivity.class);
                        intent.putExtra("SOUND", secret.getBlob());
                        context.startActivity(intent);
                    }
                });
            }
        }
    }

}
