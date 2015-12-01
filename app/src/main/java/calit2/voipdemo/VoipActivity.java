/******************************************************************************
 * Author: Jacob Terrado
 * Date: 06/20/15
 * Company: Calit2
 *****************************************************************************/

package calit2.voipdemo;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.*;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.net.sip.SipSession;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.text.ParseException;

/**
 * Name: VoipActivity
 * Description: Enables an App to App calling between two phones
 */
public class VoipActivity extends Activity implements View.OnClickListener, DialogInterface.OnClickListener {

    private final String KEY = "";
    private final String USERNAME = "mooc_sip";
    private final String DOMAIN = "sip.linphone.org";
    private final String PASSWORD = "password";

    public SipManager manager = null;
    private SipProfile profile = null;
    public SipAudioCall call = null;
    public SipAudioCall incCall = null;


    private TextView status;
    private EditText adrToCall, authorization;
    private Button makeCallBtn, endCallBtn;

    /**
     * Name: onCreate
     * Description: onCreate instantiates the xml screens. onCreate also
     *              determines if the device is capable of using VOIP and if so
     *              instantiates the proper SIP managers and profiles to make
     *              the calls.
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voip);

        // Attaches the view's button onto the private
        makeCallBtn = (Button) findViewById(R.id.makeCall);
        endCallBtn = (Button) findViewById(R.id.endCall);
        status = (TextView) findViewById(R.id.textView);

        // get address text and authorization fields
        adrToCall = (EditText) findViewById(R.id.adrToCall);
        authorization = (EditText) findViewById(R.id.passcode);

        // Determines if the device is capable of VOIP
        if (SipManager.isVoipSupported(getApplicationContext()) &&
                SipManager.isApiSupported(getApplicationContext())) {
            Log.e("$$", "VoipActivity");
            // Creates a Sip Manager for the App
            makeSipManager();

            // Creates the User's Sip Profile
            makeSipProfile();

            Log.d("Test", adrToCall.getText().toString());

            call = new SipAudioCall(getApplicationContext(), profile); // set up your calling profile

            // Listener object to handle SIP functions
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {

                /**
                 * Name: onCallEstablished
                 * Description: onCallEstablished is called when the
                 *              user establishes a call. This method
                 *              will enable the User to talk to the
                 *              person on the opposite line.
                 */
                @Override
                public void onCallEstablished(SipAudioCall call) {
                    super.onCallEstablished(call);
                    call.startAudio();
                    call.setSpeakerMode(true);
                    Log.d("call", "Call established");

                    if (call.isMuted()) {
                        call.toggleMute();
                    }
                }

                /**
                 * Name: onCallEnded
                 * Description: onCallEnded is called when the call is ended
                 */
                @Override
                public void onCallEnded(SipAudioCall endedCall) {
                    super.onCallEnded(endedCall);
                    Log.d("call", "Call ended");
                    try {
                        endedCall.endCall();
                    } catch (SipException e) {
                        e.printStackTrace();
                    }
                }
            };
            call.setListener(listener);

            // Set up Intent filter to receive calls
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.VOIPDEMO.INCOMING_CALL");
            IncomingReceiver receiver = new IncomingReceiver();
            this.registerReceiver(receiver, filter);

            // Creates a listener for the buttons
            makeCallBtn.setOnClickListener(this);
            endCallBtn.setOnClickListener(this);
        } else {
            status.setText("Your device does not support VOIP");
        }
    }

    public void onClick(View v)
    {
        if (v.getId() == R.id.makeCall) {
            // check if you are authorized to call
            if (!(authorization.getText().toString().equals(KEY))) { // if not equal
                status.setText("INCORRECT AUTHORIZATION CODE");
                return;
            }

            // check for an address
            if (adrToCall.getText().toString().isEmpty()) { // if nothing; ask to input something
                status.setText("Please tell me who to call");
                return;
            }

            // Make a Call
            status.setText("Call Made");
            SipProfile.Builder builder;
            SipProfile toCall;
            try {
                builder = new SipProfile.Builder(adrToCall.getText().toString(), DOMAIN);
                toCall = builder.build();
                SipSession.Listener ssl = new SipSession.Listener() {
                    @Override
                    public void onCallEnded(SipSession session) {
                        super.onCallEnded(session);
                        try {
                            call.endCall();
                        } catch (SipException e) {
                            e.printStackTrace();
                        }
                        session.endCall();
                    }
                };

                call.makeCall(toCall, manager.createSipSession(profile, ssl), 30);

            } catch (SipException e) {
                status.setText("Call failed: " + e.getMessage());
                e.printStackTrace();
            } catch (ParseException e) {
                status.setText("Call failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
        else { // if v.getId() == R.id.endCall
            status.setText("Call Ended");
            try {
                call.endCall();
            } catch (SipException e) {
                e.printStackTrace();
            }
        }
    }

    public void incomingCall(SipAudioCall c)
    {
        if (c == null)
            return;

        if (call.isInCall()) // if there is a call already, ignore new one
            return;

        if (incCall != null) // if there is an incoming call
            return;

        //else if call isn't null
        Log.d("call", "Incoming call");
        SipProfile caller = c.getPeerProfile();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Incoming call from:")
                                .setMessage(caller.getUriString())
                                .setPositiveButton("Accept", this)
                                .setNegativeButton("Decline", this);
        builder.show();

    }

    /**
     *
     * @param which -1 = accept button, -2 = decline button
     */
    @Override
    public void onClick(DialogInterface dialog, int which)
    {
        if (which == -1) {
            try {
                incCall.answerCall(30);
                incCall.startAudio();
                incCall.setSpeakerMode(true);

                if(incCall.isMuted()) {
                    incCall.toggleMute();
                }
            } catch (SipException e) {
                e.printStackTrace();
            }
        }
        else if (which == -2) { // decline the call
            status.setText("Incoming Call Declined");
            try {
                incCall.endCall();
            } catch (SipException e) {
                e.printStackTrace();
            }
            incCall.close();
            incCall = null;
        }
    }

    /**
     * Name: makeSipManager
     * Description: Instantiates the SipManager object.
     */
    private void makeSipManager() {
        // Creates a SipManager to enable calls
        if (manager == null) {
            manager = SipManager.newInstance(getApplicationContext());
            Log.e("$$", "Manager was instantiated");
        }
    }

    /**
     * Name: makeSipProfile
     * Description: Instantiates the User's SipProfile and enables the app to
     *               receive calls.
     */
    private void makeSipProfile() {
        if (manager != null) {
            // Creates a SipProfile for the User
            try {
                SipProfile.Builder builder =
                        new SipProfile.Builder(USERNAME, DOMAIN);
                builder.setPassword(PASSWORD);
                profile = builder.build();
                Log.e("$$", "SipProfile was built.");

                // Creates an intent to receive calls
                Intent intent = new Intent();
                intent.setAction("android.VOIPDEMO.INCOMING_CALL");
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, Intent.FILL_IN_DATA);
                manager.open(profile, pendingIntent, null);

                // Determines if the SipProfile successfully registered
                manager.setRegistrationListener(profile.getUriString(),
                    new SipRegistrationListener() {

                        /**
                         * Name: onRegistering
                         * Description: Logs a status message indicating the
                         *              SipProfile is registering.
                         */
                        public void onRegistering(String localProfileUri) {
                            Log.e("$$", "Sip Profile is registering");
                        }

                        /**
                         * Name: onRegistrationDone
                         * Description: Logs a status message indicating the
                         *              SipProfile successfully registered.
                         */
                        public void onRegistrationDone(String localProfileUri, long expiryTime) {
                            Log.e("$$", "Sip Profile successfully registered");
                        }

                        /**
                         * Name: onRegistrationFailed
                         * Description: Logs a status message indicating the
                         *              SipProfile failed to register.
                         */
                        public void onRegistrationFailed(String localProfileUri, int errorCode, String errorMessage) {
                            Log.e("$$", "Sip Profile failed to register." +
                                    " Error message: " + errorMessage);
                        }
                    });
            } catch (ParseException e) {
                Log.e("$$", "SipProfile was not built.");
                e.printStackTrace();
            } catch (SipException ignored) {
            }
        }
    }


    /**
     * Name: IncomingCallReceiver
     * Description: Ensures the app listens to incoming SIP calls. This class
     *              handles the incoming calls.
     */
    public class IncomingReceiver extends BroadcastReceiver {
        /**
         * Name: onReceive
         * Description: Process the incoming calls and answers it.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            SipAudioCall incomingCall = null;

            try {

                SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                    @Override
                    public void onRinging(SipAudioCall call, SipProfile caller) {
                        try {
                            call.answerCall(30);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };

                VoipActivity wSIP = (VoipActivity) context;
                incomingCall = wSIP.manager.takeAudioCall(intent, listener);


                wSIP.incomingCall(incomingCall);

            } catch (Exception e) {
                e.printStackTrace();
                if (incomingCall != null) {
                    incomingCall.close();
                }
            }
        }
    }
}