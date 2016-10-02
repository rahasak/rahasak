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
import com.score.chatz.asyncTasks.BitmapWorkerTask;
import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.pojo.BitmapTaskParams;
import com.score.chatz.pojo.Secret;
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

        typeface = Typeface.createFromAsset(context.getAssets(), "fonts/GeosansLight.ttf");
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        Secret secret = getItem(position);
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
        final ViewHolder holder;
        final Secret secret = getItem(i);

        if (view == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.rahas_row_layout, viewGroup, false);

            holder = new ViewHolder();
            holder.message = (TextView) view.findViewById(R.id.message);
            holder.sender = (TextView) view.findViewById(R.id.sender);
            holder.sentTime = (TextView) view.findViewById(R.id.sent_time);
            holder.userImage = (com.github.siyamed.shapeimageview.RoundedImageView) view.findViewById(R.id.user_image);

            holder.sender.setTypeface(typeface, Typeface.NORMAL);
            holder.message.setTypeface(typeface, Typeface.NORMAL);
            holder.sentTime.setTypeface(typeface, Typeface.NORMAL);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        setUpRow(secret, holder);
        return view;
    }

    private void setUpRow(Secret secret, ViewHolder viewHolder) {
        viewHolder.sender.setText("@" + secret.getUser().getUsername());

        if (secret.getType().equalsIgnoreCase("IMAGE")) {
            viewHolder.message.setText("Selfie secret");
        } else if (secret.getType().equalsIgnoreCase("IMAGE")) {
            viewHolder.message.setText("Audio secret");
        } else {
            viewHolder.message.setText(secret.getBlob());
        }

        if (secret.getTimeStamp() != null) {
            Timestamp timestamp = new Timestamp(secret.getTimeStamp());
            Date date = new Date(timestamp.getTime());
            viewHolder.sentTime.setText(TimeUtils.getTimeInWords(date));
        }

        if (secret.getUser().getUserImage() != null) {
            loadBitmap(secret.getUser().getUserImage(), viewHolder.userImage);
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
    private static class ViewHolder {
        TextView message;
        TextView sender;
        TextView sentTime;
        com.github.siyamed.shapeimageview.RoundedImageView userImage;
    }
}
