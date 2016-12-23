package com.score.rahasak.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Environment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.score.rahasak.R;
import com.score.rahasak.db.SenzorsDbSource;
import com.score.rahasak.enums.BlobType;
import com.score.rahasak.enums.DeliveryState;
import com.score.rahasak.pojo.Secret;
import com.score.rahasak.utils.Blur;
import com.score.rahasak.utils.LimitedList;
import com.score.rahasak.utils.TimeUtils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by eranga on 9/28/16
 */
class ChatListAdapter extends BaseAdapter {

    private Context context;
    private LimitedList<Secret> secretList;
    private SenzorsDbSource dbSource;

    private Typeface typeface;

    private static final int MY_CHAT_ITEM = 0;
    private static final int FRIEND_CHAT_ITEM = 1;
    private static final int MAX_TYPE_COUNT = 2;

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
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            holder = new ViewHolder();

            switch (type) {
                case MY_CHAT_ITEM:
                    view = layoutInflater.inflate(R.layout.my_chat_view_row_layout, parent, false);
                    holder.chatCamHolder = (RelativeLayout) view.findViewById(R.id.chat_cam_holder);
                    holder.chatMicHolder = (RelativeLayout) view.findViewById(R.id.chat_mic_holder);
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
                    holder.chatMicHolder = (RelativeLayout) view.findViewById(R.id.chat_mic_holder);
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
        if (secret.getBlobType() == BlobType.TEXT) {
            holder.chatCamHolder.setVisibility(View.GONE);
            holder.chatMicHolder.setVisibility(View.GONE);
            holder.chatMsgHolder.setVisibility(View.VISIBLE);

            holder.chatMsg.setText(secret.getBlob());
        } else if (secret.getBlobType() == BlobType.IMAGE) {
            holder.chatCamHolder.setVisibility(View.VISIBLE);
            holder.chatMicHolder.setVisibility(View.GONE);
            holder.chatMsgHolder.setVisibility(View.GONE);

            if (secret.isMissed()) {
                holder.chatCam.setImageResource(R.drawable.missed_selfie_call);
            } else {
                //holder.chatCam.setImageBitmap(new ImageUtils().decodeBitmap(secret.getBlob()));
                Transformation blurTransformation = new Transformation() {
                    @Override
                    public Bitmap transform(Bitmap source) {
                        Bitmap blurred = Blur.fastblur(context, source, 10);
                        source.recycle();
                        return blurred;
                    }

                    @Override
                    public String key() {
                        return "blur()";
                    }
                };

                try {
                    File dir = new File(Environment.getExternalStorageDirectory().getPath() + "/Rahasak");
                    if (!dir.exists()) {
                        dir.mkdir();
                    }

                    File file = new File(Environment.getExternalStorageDirectory().getPath() + "/Rahasak/" + secret.getId() + ".jpg");
                    file.createNewFile();

                    OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
                    byte data[] = Base64.decode(secret.getBlob(), Base64.DEFAULT);
                    os.write(data);
                    os.flush();
                    os.close();

                    Picasso.
                            with(context).
                            load(file).
                            resize(150, 150).
                            centerCrop().
                            error(R.drawable.rahaslogo).
                            into(holder.chatCam);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (secret.getBlobType() == BlobType.SOUND) {
            holder.chatCamHolder.setVisibility(View.GONE);
            holder.chatMicHolder.setVisibility(View.VISIBLE);
            holder.chatMsgHolder.setVisibility(View.GONE);
        }

        if (secret.isSender()) {
            holder.chatDelivered.setVisibility(View.GONE);
            holder.chatPending.setVisibility(View.GONE);
        } else {
            if (secret.getDeliveryState() == DeliveryState.DELIVERED) {
                holder.chatDelivered.setVisibility(View.VISIBLE);
                holder.chatPending.setVisibility(View.GONE);
                holder.chatTime.setVisibility(View.VISIBLE);
            } else if (secret.getDeliveryState() == DeliveryState.RECEIVED) {
                holder.chatDelivered.setVisibility(View.GONE);
                holder.chatPending.setVisibility(View.VISIBLE);
                holder.chatTime.setVisibility(View.VISIBLE);
            } else {
                holder.chatDelivered.setVisibility(View.GONE);
                holder.chatPending.setVisibility(View.GONE);
                holder.chatTime.setVisibility(View.GONE);
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
                    Intent intent = new Intent(context, SelfieCaptureActivity.class);
                    intent.putExtra("USER", secret.getUser().getUsername());
                    intent.putExtra("CAM_MIS", true);
                    context.startActivity(intent);

                    // remove item
                    secretList.remove(secret);
                    dbSource.deleteSecret(secret);
                    notifyDataSetChanged();
                } else {
                    Intent intent = new Intent(context, SelfieCallActivity.class);
                    intent.putExtra("IMAGE", secret.getBlob());
                    context.startActivity(intent);
                }
            }
        });

        holder.chatMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, SecretCallActivity.class);
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
