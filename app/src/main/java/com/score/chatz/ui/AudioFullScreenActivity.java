package com.score.chatz.ui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.score.chatz.R;
import com.score.chatz.application.IntentProvider;
import com.score.chatz.asyncTasks.RahasPlayer;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.interfaces.IRahasPlayListener;
import com.score.chatz.pojo.SecretUser;
import com.score.chatz.utils.ImageUtils;
import com.score.chatz.utils.PhotoUtils;
import com.score.senzc.pojos.Senz;

public class AudioFullScreenActivity extends AppCompatActivity implements IRahasPlayListener {

    private static final String TAG = AudioFullScreenActivity.class.getName();

    private View loadingView;

    private TextView playingText;
    private TextView usernameText;
    private TextView micCallingText;
    private ImageView waitingIcon;

    private Typeface typeface;

    private SecretUser secretUser;

    //Set the radius of the Blur. Supported range 0 < radius <= 25
    private static final float BLUR_RADIUS = 5f;

    // senz message
    private BroadcastReceiver senzReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got message from Senz service");

            // extract senz
            if (intent.hasExtra("SENZ")) {
                Senz senz = intent.getExtras().getParcelable("SENZ");
                switch (senz.getSenzType()) {
                    case DATA:
                        onSenzReceived(senz);
                        break;
                    case STREAM:
                        onSenzStreamReceived(senz);
                        break;
                    default:
                        break;
                }
            }
        }
    };

    private void onSenzStreamReceived(Senz senz) {
        if (senz.getAttributes().containsKey("mic")) {
            // display stream
            loadingView.setVisibility(View.INVISIBLE);
            playingText.setVisibility(View.VISIBLE);
            playSecret(Base64.decode(senz.getAttributes().get("mic"), 0));
        }
    }

    private void onSenzReceived(Senz senz) {
        if (senz.getAttributes().containsKey("status")) {
            if (senz.getAttributes().get("status").equalsIgnoreCase("901")) {
                // user busy
                displayInformationMessageDialog("info", "user busy");
            } else if (senz.getAttributes().get("status").equalsIgnoreCase("902")) {
                // camera error
                displayInformationMessageDialog("error", "mic error");
            } else if (senz.getAttributes().get("status").equalsIgnoreCase("offline")) {
                // camera error
                displayInformationMessageDialog("Offline", "User offline");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_full_screen);

        setUpFonts();
        setupUi();
        initIntent();
    }

    private void setUpFonts() {
        typeface = Typeface.createFromAsset(getAssets(), "fonts/GeosansLight.ttf");
    }

    private void setupUi() {
        waitingIcon = (ImageView) findViewById(R.id.selfie_image);
        loadingView = findViewById(R.id.mic_loading_view);

        playingText = (TextView) findViewById(R.id.mic_playingText);
        usernameText = (TextView) findViewById(R.id.mic_username);
        micCallingText = (TextView) findViewById(R.id.mic_calling_text);

        usernameText.setTypeface(typeface, Typeface.BOLD);
        micCallingText.setTypeface(typeface, Typeface.NORMAL);
        playingText.setTypeface(typeface, Typeface.NORMAL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(senzReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.SENZ));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(senzReceiver);
    }

    private void initIntent() {
        Intent intent = getIntent();
        if (intent.hasExtra("SOUND")) {
            loadingView.setVisibility(View.INVISIBLE);
            playingText.setVisibility(View.VISIBLE);
            playSecret(Base64.decode(intent.getStringExtra("SOUND"), 0));
        } else {
            loadingView.setVisibility(View.VISIBLE);
            playingText.setVisibility(View.INVISIBLE);
            setupUserImage(intent.getStringExtra("SENDER"));
            startAnimatingWaitingIcon();
        }
    }

    private void playSecret(byte[] rahasa) {
        RahasPlayer rahasPlayer = new RahasPlayer(rahasa, getApplicationContext(), this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            rahasPlayer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            rahasPlayer.execute("PLAY");
        }
    }

    private void startAnimatingWaitingIcon() {
        AnimationDrawable anim = (AnimationDrawable) waitingIcon.getBackground();
        anim.start();
    }

    private void setupUserImage(String sender) {
        secretUser = new SenzorsDbSource(this).getSecretUser(sender);
        if (secretUser.getImage() != null) {
            Bitmap bitmap = new ImageUtils().decodeBitmap(secretUser.getImage());
            ((ImageView) findViewById(R.id.user_profile_image)).setImageBitmap(new ImageUtils().blur(bitmap, BLUR_RADIUS, this));
        }

        usernameText.setText("@" + sender);
    }

    public void displayInformationMessageDialog(String title, String message) {
        final Dialog dialog = new Dialog(this);

        //set layout for dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.information_message_dialog);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);

        // set dialog texts
        TextView messageHeaderTextView = (TextView) dialog.findViewById(R.id.information_message_dialog_layout_message_header_text);
        TextView messageTextView = (TextView) dialog.findViewById(R.id.information_message_dialog_layout_message_text);
        messageHeaderTextView.setText(title);
        messageTextView.setText(Html.fromHtml(message));

        // set custom font
        messageHeaderTextView.setTypeface(typeface, Typeface.BOLD);
        messageTextView.setTypeface(typeface);

        //set ok button
        Button okButton = (Button) dialog.findViewById(R.id.information_message_dialog_layout_ok_button);
        okButton.setTypeface(typeface, Typeface.BOLD);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.cancel();
                AudioFullScreenActivity.this.finish();
            }
        });

        dialog.show();
    }

    @Override
    public void onFinishPlay() {
        this.finish();
    }
}