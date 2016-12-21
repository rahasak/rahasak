package com.score.rahasak.utils;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.score.rahasak.R;
import com.score.rahasak.exceptions.InvalidInputFieldsException;
import com.score.senzc.pojos.User;

/**
 * Utility class to handle activity related common functions
 *
 * @author erangaeb@gmail.com (eranga herath)
 */
public class ActivityUtils {

    private static ProgressDialog progressDialog;

    /**
     * Hide keyboard
     * Need to hide soft keyboard in following scenarios
     * 1. When starting background task
     * 2. When exit from activity
     * 3. On button submit
     */
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getApplicationContext().getSystemService(activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**
     * Create and show custom progress dialog
     * Progress dialogs displaying on background tasks
     * <p/>
     * So in here
     * 1. Create custom layout for message dialog
     * 2, Set messages to dialog
     *
     * @param context activity context
     * @param message message to be display
     */
    public static void showProgressDialog(Context context, String message) {
        progressDialog = ProgressDialog.show(context, null, message, true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(true);

        progressDialog.show();
    }

    /**
     * Cancel progress dialog when background task finish
     */
    public static void cancelProgressDialog() {
        if (progressDialog != null) {
            progressDialog.cancel();
        }
    }

    /**
     * Validate input fields of registration form,
     * Need to have
     * 1. non empty valid phone no
     * 2. non empty username
     * 3. non empty passwords
     * 4. two passwords should be match
     *
     * @param user User object
     * @return valid or not
     */
    public static boolean isValidRegistrationFields(User user) throws InvalidInputFieldsException {
        if (user.getUsername().isEmpty() || user.getUsername().contains("@") || user.getUsername().contains("#")) {
            throw new InvalidInputFieldsException();
        }

        return true;
    }

    /**
     * validate input fields of login form
     *
     * @param user login user
     * @return valid of not
     */
    public static boolean isValidLoginFields(User user) {
        return !(user.getUsername().isEmpty());
    }

    /**
     * Create custom text view for tab view
     * Set custom typeface to the text view as well
     *
     * @param context application context
     * @param title   tab title
     * @return text view
     */
    public static TextView getCustomTextView(Context context, String title) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        Typeface typefaceThin = Typeface.createFromAsset(context.getAssets(), "fonts/vegur_2.otf");

        TextView textView = new TextView(context);
        textView.setText(title);
        textView.setTypeface(typefaceThin);
        textView.setTextColor(Color.parseColor("#4a4a4a"));
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(18);
        textView.setLayoutParams(layoutParams);

        return textView;
    }

    /**
     * Show a toast when required!!!
     *
     * @param message
     * @param context
     */
    public static void showToast(String message, Context context) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static void showCustomToast(String message, Context context) {
        /*
        Custom layout for Toast
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.custom_toast, null);
        TextView text = (TextView) layout.findViewById(R.id.text);
        text.setText(message);

        Toast toast = new Toast(context);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
        */

        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static void showCustomToastShort(String message, Context context) {
        /*LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.custom_toast, null);
        TextView text = (TextView) layout.findViewById(R.id.text);
        text.setText(message);

        Toast toast = new Toast(context);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();*/
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Generic display confirmation pop up
     *
     * @param message   - Message to ask
     * @param okClicked - instance of View.OnClickListener to handle okbutton clicked
     */
    public static void displayConfirmationMessageDialog(String title, String message, Context context, Typeface typeface, final View.OnClickListener okClicked) {
        final Dialog dialog = new Dialog(context);

        //set layout for dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.share_confirm_message_dialog);
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
            @Override
            public void onClick(View v) {
                okClicked.onClick(v);
                dialog.cancel();
            }
        });

        // cancel button
        Button cancelButton = (Button) dialog.findViewById(R.id.information_message_dialog_layout_cancel_button);
        cancelButton.setTypeface(typeface, Typeface.BOLD);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        dialog.show();
    }

}
