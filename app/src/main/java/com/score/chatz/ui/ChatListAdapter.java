package com.score.chatz.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.score.chatz.R;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.pojo.Secret;
import com.score.chatz.utils.ImageUtils;
import com.score.chatz.utils.LimitedList;
import com.score.chatz.utils.TimeUtils;

/**
 * Created by eranga on 9/28/16
 */
class ChatListAdapter extends BaseAdapter {

    private Context context;
    private LimitedList<Secret> secretList;
    private SenzorsDbSource dbSource;

    private Typeface typeface;
    private LayoutInflater layoutInflater;

    private static final int MY_CHAT_ITEM = 0;
    private static final int FRIEND_CHAT_ITEM = 1;
    private static final int MAX_TYPE_COUNT = 2;

    ChatListAdapter(Context context, LimitedList<Secret> secretList) {
        this.context = context;
        this.secretList = secretList;
        this.dbSource = new SenzorsDbSource(context);

        typeface = Typeface.createFromAsset(context.getAssets(), "fonts/GeosansLight.ttf");
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return secretList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return ((Secret) getItem(position)).isSender() ? FRIEND_CHAT_ITEM : MY_CHAT_ITEM;
    }

    @Override
    public int getViewTypeCount() {
        return MAX_TYPE_COUNT;
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
        final Secret secret = secretList.get(position);
        final int type = getItemViewType(position);

        if (view == null) {
            holder = new ViewHolder();

            switch (type) {
                case MY_CHAT_ITEM:
                    view = layoutInflater.inflate(R.layout.my_chat_view_row_layout, parent, false);
                    holder.chatCamHolder = (RelativeLayout) view.findViewById(R.id.chat_cam_holder);
                    holder.chatMicHolder = (RelativeLayout) view.findViewById(R.id.chat__mic_holder);
                    holder.chatMsgHolder = (LinearLayout) view.findViewById(R.id.chat_msg_holder);

                    holder.chatCam = (ImageView) view.findViewById(R.id.chat_cam);
                    holder.chatMic = (ImageView) view.findViewById(R.id.chat_mic);
                    holder.chatMsg = (TextView) view.findViewById(R.id.chat_msg);
                    holder.chatTime = (TextView) view.findViewById(R.id.chat_time);
                    holder.chatDelivered = (ImageView) view.findViewById(R.id.chat_delivered);
                    holder.chatPending = (ImageView) view.findViewById(R.id.chat_pending);

                    break;
                case FRIEND_CHAT_ITEM:
                    view = layoutInflater.inflate(R.layout.friend_chat_view_row_layout, parent, false);
                    holder.chatCamHolder = (RelativeLayout) view.findViewById(R.id.chat_cam_holder);
                    holder.chatMicHolder = (RelativeLayout) view.findViewById(R.id.chat__mic_holder);
                    holder.chatMsgHolder = (LinearLayout) view.findViewById(R.id.chat_msg_holder);

                    holder.chatCam = (ImageView) view.findViewById(R.id.chat_cam);
                    holder.chatMic = (ImageView) view.findViewById(R.id.chat_mic);
                    holder.chatMsg = (TextView) view.findViewById(R.id.chat_msg);
                    holder.chatTime = (TextView) view.findViewById(R.id.chat_time);
                    holder.chatDelivered = (ImageView) view.findViewById(R.id.chat_delivered);
                    holder.chatPending = (ImageView) view.findViewById(R.id.chat_pending);

                    break;
            }

            holder.chatMsg.setTypeface(typeface, Typeface.BOLD);
            holder.chatTime.setTypeface(typeface);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        setupRow(secret, holder);
        return view;
    }

    private void setupRow(final Secret secret, ViewHolder holder) {
        if (secret.getType().equalsIgnoreCase("TEXT")) {
            holder.chatCamHolder.setVisibility(View.GONE);
            holder.chatMicHolder.setVisibility(View.GONE);
            holder.chatMsgHolder.setVisibility(View.VISIBLE);

            holder.chatMsg.setText(secret.getBlob());
        } else if (secret.getType().equalsIgnoreCase("IMAGE")) {
            holder.chatCamHolder.setVisibility(View.VISIBLE);
            holder.chatMicHolder.setVisibility(View.GONE);
            holder.chatMsgHolder.setVisibility(View.GONE);

            if (secret.isMissed()) {
                holder.chatCam.setImageResource(R.drawable.missed_selfie_call);
            } else {
                //new BitmapWorkerTask(holder.chatCam).execute(new BitmapTaskParams(secret.getBlob(), 400, 400));
                holder.chatCam.setImageBitmap(new ImageUtils().decodeBitmap(secret.getBlob()));
            }
        } else if (secret.getType().equalsIgnoreCase("SOUND")) {
            holder.chatCamHolder.setVisibility(View.GONE);
            holder.chatMicHolder.setVisibility(View.VISIBLE);
            holder.chatMsgHolder.setVisibility(View.GONE);
        }

        if (secret.isSender()) {
            holder.chatDelivered.setVisibility(View.GONE);
            holder.chatPending.setVisibility(View.GONE);
        } else {
            if (secret.isDelivered()) {
                holder.chatDelivered.setVisibility(View.VISIBLE);
                holder.chatPending.setVisibility(View.GONE);
            } else {
                holder.chatDelivered.setVisibility(View.GONE);
                holder.chatPending.setVisibility(View.VISIBLE);
            }
        }

        if (secret.getTimeStamp() != null) {
            holder.chatTime.setText(TimeUtils.getTimeInWords(secret.getTimeStamp()));
        }

        holder.chatCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (secret.isMissed()) {
                    // missed
                    // start photo activity
                    Intent intent = new Intent(context, PhotoActivity.class);
                    intent.putExtra("USER", secret.getUser());
                    intent.putExtra("CAM_MIS", true);
                    context.startActivity(intent);

                    // remove item
                    secretList.remove(secret);
                    dbSource.deleteSecret(secret);
                    notifyDataSetChanged();
                } else {
                    Intent intent = new Intent(context, PhotoFullScreenActivity.class);
                    intent.putExtra("IMAGE", secret.getBlob());
                    context.startActivity(intent);
                }
            }
        });

        holder.chatMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, AudioFullScreenActivity.class);
                intent.putExtra("SOUND", secret.getBlob());
                context.startActivity(intent);
            }
        });
    }

    /**
     * Keep reference to children view to avoid unnecessary calls
     */
    private static class ViewHolder {
        RelativeLayout chatCamHolder;
        RelativeLayout chatMicHolder;
        LinearLayout chatMsgHolder;

        ImageView chatCam;
        ImageView chatMic;
        TextView chatMsg;

        TextView chatTime;
        ImageView chatDelivered;
        ImageView chatPending;
    }

}
