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
import android.os.Bundle;
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
public class VoipActivity extends Activity {

    private static final String USERNAME = "mooc_sip";
    private static final String DOMAIN = "sip.linphone.org";
    private static final String PASSWORD = "password";
    public SipManager manager = null;
    private SipProfile profile = null;
    public SipAudioCall call = null;
    private TextView status;
    private EditText mEdit;

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
        Button button = (Button) findViewById(R.id.button);
        status = (TextView) findViewById(R.id.textView);
        // Determines if the device is capable of VOIP
        if (SipManager.isVoipSupported(getApplicationContext()) &&
                SipManager.isApiSupported(getApplicationContext())) {
            Log.e("$$", "VoipActivity");
            // Creates a Sip Manager for the App
            makeSipManager();

            // Creates the User's Sip Profile
            makeSipProfile();

            Log.d("Test", mEdit.getText().toString());




            // Listener object to handle SIP functions
            final SipAudioCall.Listener listener =
                    new SipAudioCall.Listener() {

                        /**
                         * Name: onCallEstablished
                         * Description: onCallEstablished is called when the
                         *              user establishes a call. This method
                         *              will enable the User to talk to the
                         *              person on the opposite line.
                         */
                        @Override
                        public void onCallEstablished(SipAudioCall call) {
                            call.startAudio();
                            call.setSpeakerMode(true);

                            if (call.isMuted()) {
                                call.toggleMute();
                            }
                        }

                        /**
                         * Name: onCallEnded
                         * Description: onCallEnded is called when the user ends the
                         *              call.
                         */
                        @Override
                        public void onCallEnded(SipAudioCall endedCall) {
                            try {
                                call = null;
                                endedCall.endCall();
                            } catch (SipException e) {
                                e.printStackTrace();
                            }
                        }
                    };

            // Set up Intent filter to receive calls
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.VOIPDEMO.INCOMING_CALL");
            IncomingReceiver receiver = new IncomingReceiver();
            this.registerReceiver(receiver, filter);

            // Creates a listener for the button
            button.setOnClickListener(new View.OnClickListener() {

                /**
                 * Name: onClick
                 * Description: onClick determines what action the button
                 *              performs when clicked by the user.
                 */
                @Override
                public void onClick(View view) {
                    // Make a Call
                    if (call == null) {
                        try {
                            call = manager.makeAudioCall(profile.getUriString(),
                                    "sip:" + mEdit.getText().toString()+"@sip.linphone.org", listener,
                                    30);

                            /*voip_demo2*/
                            status.setText("Call Made");
                            Log.e("$$", "Call went through");
                        } catch (SipException e) {
                            Log.e("$$", "Call failed. Error message: " + e);
                        }
                    } else { // Hang up
                        try {
                            call.endCall();
                        } catch (SipException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } else {
            status.setText("Your device does not support VOIP");
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
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
                        0, intent, Intent.FILL_IN_DATA);
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
                        public void onRegistrationDone(String localProfileUri,
                                                       long expiryTime) {
                            Log.e("$$", "Sip Profile successfully registered");
                        }

                        /**
                         * Name: onRegistrationFailed
                         * Description: Logs a status message indicating the
                         *              SipProfile failed to register.
                         */
                        public void onRegistrationFailed(String localProfileUri,
                                                         int errorCode,
                                                         String errorMessage) {
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
                    public void onRinging(SipAudioCall call,
                                          SipProfile caller) {
                        try {
                            call.answerCall(30);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };

                VoipActivity wSIP = (VoipActivity) context;
                incomingCall = wSIP.manager.takeAudioCall(intent, listener);
                incomingCall.answerCall(30);
                incomingCall.startAudio();
                incomingCall.setSpeakerMode(true);

                if(incomingCall.isMuted()) {
                    incomingCall.toggleMute();
                }

//                wSIP.call = incomingCall;
//                wSIP.updateStatus(incomingCall);

            } catch (Exception e) {
                if (incomingCall != null) {
                    incomingCall.close();
                }
            }
        }
    }
}

/*
id = voip_demo
pass = password
domain = sip.linphone.org
sip:voip_demo@sip.linphone.org

id = voip_demo2
pass = password
domain = sip.linphone.org
sip:voip_demo2@sip.linphone.org
 */