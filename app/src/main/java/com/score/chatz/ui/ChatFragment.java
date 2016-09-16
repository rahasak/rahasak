package com.score.chatz.ui;


import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.score.chatz.R;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.pojo.Secret;
import com.score.chatz.pojo.UserPermission;
import com.score.chatz.services.LocationAddressReceiver;
import com.score.chatz.utils.NetworkUtil;
import com.score.chatz.utils.PreferenceUtils;
import com.score.chatz.utils.SecretsUtil;
import com.score.chatz.utils.SenzUtils;
import com.score.senz.ISenzService;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TAG = ChatFragment.class.getName();
    private static final String RECEIVER = "RECEIVER";
    private static final String SENDER = "SENDER";

    // TODO: Rename and change types of parameters
    private String receiver;
    private String sender;

    private EditText text_message;
    private ImageButton sendBtn;
    private ImageButton micBtn;
    private ImageButton getLocBtn;
    private ImageButton getCamBtn;
    private ImageButton getMicBtn;

    SenzorsDbSource dbSource;
    User currentUser;
    private ListView listView;
    // custom font
    private Typeface typeface;
    boolean isServiceBound = false;
    private List<Secret> secretMessageList;
    private ChatFragmentListAdapter adapter;

    // service interface
    private ISenzService senzService = null;

    // service connection
    private ServiceConnection senzServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("TAG", "Connected with senz service");
            senzService = ISenzService.Stub.asInterface(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            senzService = null;
            Log.d("TAG", "Disconnected from senz service");
        }
    };


    public ChatFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param receiver receiver
     * @return A new instance of fragment ChatFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChatFragment newInstance(User sender, User receiver) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(RECEIVER, receiver.getUsername());
        args.putString(SENDER, sender.getUsername());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            receiver = getArguments().getString(RECEIVER);
            sender = getArguments().getString(SENDER);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        displayMessagesList();
        startDeletingChatMessages();
    }

    private void startDeletingChatMessages(){
        for (Secret secret : secretMessageList) {

            startTimerToDelete(secret);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // bind to senz service
        if (!isServiceBound) {
            Intent intent = new Intent();
            intent.setClassName("com.score.chatz", "com.score.chatz.services.RemoteSenzService");
            getActivity().bindService(intent, senzServiceConnection, Context.BIND_AUTO_CREATE);
            isServiceBound = true;
        }
    }


    @Override
    public void onStop() {
        super.onStop();

        // Unbind from the service
        if (isServiceBound) {
            getActivity().unbindService(senzServiceConnection);
            isServiceBound = false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        view.setTag("chatFragment");
        FragmentManager fm = getActivity().getSupportFragmentManager();

        dbSource = new SenzorsDbSource(getContext());
        text_message = (EditText) view.findViewById(R.id.text_message);

        sendBtn = (ImageButton) view.findViewById(R.id.sendBtn);
        micBtn = (ImageButton) view.findViewById(R.id.getMicBtn);
        getCamBtn = (ImageButton) view.findViewById(R.id.getCamBtn);
        getMicBtn = (ImageButton) view.findViewById(R.id.getMicBtn);
        getLocBtn = (ImageButton) view.findViewById(R.id.getLocBtn);

        typeface = Typeface.createFromAsset(getActivity().getAssets(), "fonts/vegur_2.otf");
        listView = (ListView) view.findViewById(R.id.messages_list_view);


        try {
            currentUser = PreferenceUtils.getUser(getContext());
        } catch (NoUserException e) {
            e.printStackTrace();
        }

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (NetworkUtil.isAvailableNetwork(getContext())) {
                    if (text_message.getText().length() != 0) {
                        // Get text
                        String secretMessage = text_message.getText().toString();

                        // clear text in ui
                        text_message.setText("");

                        // Create secret
                        Secret newSecret = new Secret(secretMessage, null, null, new User("", receiver), new User("", sender));
                        newSecret.setID(SenzUtils.getUniqueRandomNumber().toString());

                        // Send secret
                        sendMessage(newSecret);

                        //Save secret
                        saveSecretToDB(newSecret);

                        //Update list view
                        displayMessagesList();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.no_internet, Toast.LENGTH_SHORT).show();
                }
            }
        });

        getLocBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (NetworkUtil.isAvailableNetwork(getContext())) {
                    if (getLocBtn.isEnabled())
                        getSenz(new User("", sender));
                } else {
                    Toast.makeText(getActivity(), R.string.no_internet, Toast.LENGTH_SHORT).show();
                }
            }
        });

        getCamBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (NetworkUtil.isAvailableNetwork(getContext())) {
                    if (getCamBtn.isEnabled())
                        getPhoto(new User("", sender));
                } else {
                    Toast.makeText(getActivity(), R.string.no_internet, Toast.LENGTH_SHORT).show();
                }
            }
        });

        getMicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (NetworkUtil.isAvailableNetwork(getContext())) {
                    if (getMicBtn.isEnabled())
                        getMic(new User("", sender));
                } else {
                    Toast.makeText(getActivity(), R.string.no_internet, Toast.LENGTH_SHORT).show();
                }
            }
        });

        updateMainBtnUi();

        return view;
    }

    /**
     * Save text secret to db
     *
     * @param secret
     */
    private void saveSecretToDB(Secret secret) {
        SenzorsDbSource dbSource = new SenzorsDbSource(getActivity());
        Long _timeStamp = System.currentTimeMillis();
        secret.setTimeStamp(_timeStamp);
        dbSource.createSecret(secret);
    }

    private void displayMessagesList() {
        // get User from db
        if (secretMessageList == null || secretMessageList.size() == 0) {
            secretMessageList = java.util.Collections.synchronizedList(dbSource.getSecretz(new User("", sender), new User("", receiver)));
            adapter = new ChatFragmentListAdapter(getContext(), secretMessageList);
            listView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        } else {
            List<Secret> latestList = dbSource.getSecretz(new User("", sender), new User("", receiver), secretMessageList.get(secretMessageList.size() - 1).getTimeStamp());
            secretMessageList.addAll(latestList);
            adapter.notifyDataSetChanged();
        }
        //removeOldItemsFromChat();
        startDeletingChatMessages();
    }

    private void startTimerToDelete(final Secret secret){
        new CountDownTimer(10000, 10000) {
            @Override
            public void onTick(long millisUntilFinished) {
                //Ticking away to delete
            }

            @Override
            public void onFinish() {
                removeItemFromChat(secret);
            }
        }.start();
    }

    private void removeItemFromChat(Secret _secret){
        for (Iterator<Secret> iterator = secretMessageList.iterator(); iterator.hasNext(); ) {
            Secret secret = iterator.next();
            if(secret.equals(_secret)){
                new SenzorsDbSource(getContext()).deleteSecret(secret);
                iterator.remove();
                adapter.notifyDataSetChanged();
            }
        }

    }

    private void updateStatusOfMessage(String uid) {
        for (Secret secret : secretMessageList) {
            if (secret.getID().equalsIgnoreCase(uid)) {
                secret.setIsDelivered(true);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void updateMessageFailed(String uid) {
        for (Secret secret : secretMessageList) {
            if (secret.getID().equalsIgnoreCase(uid)) {
                secret.setDeliveryFailed(true);
            }
        }
        adapter.notifyDataSetChanged();
    }


    private void removeOldItemsFromChat() {
        Iterator<Secret> iter = secretMessageList.iterator();
        while (iter.hasNext()) {
            Secret secret = iter.next();

            if (!SecretsUtil.isSecretToBeShown(secret)) {
                iter.remove();
                dbSource.deleteSecret(secret);
            }
        }
    }

    /**
     * Share current sensor
     * Need to send share query to server via web socket
     */
    private void sendMessage(Secret secret) {
        try {
            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("chatzmsg", URLEncoder.encode(secret.getText(), "UTF-8"));
            String timeStamp = ((Long) (System.currentTimeMillis() / 1000)).toString();
            senzAttributes.put("time", timeStamp);
            senzAttributes.put("uid", secret.getID());

            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.DATA;
            User _sender = new User("", sender.trim());
            Senz senz = new Senz(id, signature, senzType, null, _sender, senzAttributes);


            senzService.send(senz);
        } catch (RemoteException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    /**
     * Handle broadcast message receives
     * Need to handle registration success failure here
     *
     * @param intent intent
     */
    public void handleMessage(Intent intent) {
        String action = intent.getAction();

        if (action.equalsIgnoreCase("com.score.chatz.DATA_SENZ")) {
            Senz senz = intent.getExtras().getParcelable("SENZ");

            if (senz.getAttributes().containsKey("msg")) {
                String msg = senz.getAttributes().get("msg");
                if (msg != null && msg.equalsIgnoreCase("MsgSent")) {
                    // save senz in db
                    Log.d(TAG, "save sent chatz");
                    Log.d(TAG, "reeiver: " + receiver + ", " + "senz.getSender() : " + senz.getSender().getUsername());

                    String uid = senz.getAttributes().get("uid");
                    dbSource.markSecretDelievered(uid);
                    updateStatusOfMessage(uid);
                } else if (msg != null && msg.equalsIgnoreCase("photoSent")) {
                    // save senz in db
                    String uid = senz.getAttributes().get("uid");
                    dbSource.markSecretDelievered(uid);
                    updateStatusOfMessage(uid);
                } else if (msg != null && msg.equalsIgnoreCase("soundSent")) {
                    String uid = senz.getAttributes().get("uid");
                    dbSource.markSecretDelievered(uid);
                    updateStatusOfMessage(uid);
                } else if (msg != null && msg.equalsIgnoreCase("MsgSentFail")) {
                    Toast.makeText(getActivity(), "User seems to be offline.", Toast.LENGTH_SHORT).show();
                }
            } else if (senz.getAttributes().containsKey("chatzphoto")) {
                displayMessagesList();
            } else if (senz.getAttributes().containsKey("chatzmsg")) {
                displayMessagesList();
            } else if (senz.getAttributes().containsKey("chatzsound")) {
                displayMessagesList();
            } else if (senz.getAttributes().containsKey("lat")) {
                // location response received
                double lat = Double.parseDouble(senz.getAttributes().get("lat"));
                double lan = Double.parseDouble(senz.getAttributes().get("lon"));
                LatLng latLng = new LatLng(lat, lan);

                // start location address receiver
                new LocationAddressReceiver(getActivity(), latLng, senz.getSender()).execute("PARAM");

                // start map activity
                Intent mapIntent = new Intent(getActivity(), SenzMapActivity.class);
                mapIntent.putExtra("extra", latLng);
                getActivity().startActivity(mapIntent);
                getActivity().overridePendingTransition(R.anim.right_in, R.anim.stay_in);
            }

            updateMainBtnUi();
        }
    }


    /**
     * Display message dialog when user request(click) to delete invoice
     *
     * @param message message to be display
     */
    public void displayInformationMessageDialog(String title, String message) {
        final Dialog dialog = new Dialog(getActivity());

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
        messageHeaderTextView.setTypeface(typeface);
        messageTextView.setTypeface(typeface);

        //set ok button
        Button okButton = (Button) dialog.findViewById(R.id.information_message_dialog_layout_ok_button);
        okButton.setTypeface(typeface);
        okButton.setTypeface(null, Typeface.BOLD);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        dialog.show();
    }


    /**
     * Send GET senz to service, actual senz sending task done by SenzService
     *
     * @param receiver senz receiver
     */
    private void getSenz(User receiver) {
        try {
            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
            senzAttributes.put("lat", "lat");
            senzAttributes.put("lon", "lon");

            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.GET;
            Senz senz = new Senz(id, signature, senzType, null, receiver, senzAttributes);

            senzService.send(senz);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /*
     * Get photo of user
     */
    private void getPhoto(User receiver) {
        try {
            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
            senzAttributes.put("chatzphoto", "chatzphoto");

            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.GET;
            Senz senz = new Senz(id, signature, senzType, null, receiver, senzAttributes);

            senzService.send(senz);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /*
     * Get mic of user
     */
    private void getMic(User receiver) {
        try {
            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
            senzAttributes.put("chatzmic", "chatzmic");

            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.GET;
            Senz senz = new Senz(id, signature, senzType, null, receiver, senzAttributes);

            senzService.send(senz);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void updateMainBtnUi() {
        Log.i(TAG, "Getting permission of user - " + sender);
        UserPermission userPerm = dbSource.getUserPermission(new User("", sender));
        if (userPerm.getCamPerm() == true) {
            getCamBtn.setImageResource(R.drawable.perm_camera_active);
            getCamBtn.setEnabled(true);
        } else {
            getCamBtn.setImageResource(R.drawable.perm_camera_deactive);
            getCamBtn.setEnabled(false);
        }

        if (userPerm.getLocPerm() == true) {
            getLocBtn.setImageResource(R.drawable.perm_locations_active);
            getLocBtn.setEnabled(true);
        } else {
            getLocBtn.setImageResource(R.drawable.perm_locations_deactive);
            getLocBtn.setEnabled(false);
        }

        if (userPerm.getMicPerm() == true) {
            getMicBtn.setImageResource(R.drawable.perm_mic_active);
            getMicBtn.setEnabled(true);
        } else {
            getMicBtn.setImageResource(R.drawable.perm_mic_deactive);
            getMicBtn.setEnabled(false);
        }
    }

    public void onPacketTimeout(Senz senz){
        dbSource.markSecretDeliveryFailed(senz.getAttributes().get("uid"));
        updateMessageFailed(senz.getAttributes().get("uid"));
        updateMainBtnUi();
    }
}
