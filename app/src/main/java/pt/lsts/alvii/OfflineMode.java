package pt.lsts.alvii;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * Created by Pedro Gonçalves on 7/11/17.
 * LSTS - FEUP
 */

public class OfflineMode extends AppCompatActivity {

    private static final String TAG = "MEU OFFLINEMODE";
    public static Activity fa;
    private final static int MOBILE = 0;
    private final static int WIFI = 1;
    private ProgressBar barWifi;
    private Handler customHandler;
    ImageButton imageButtonLog;
    ImageButton imageButtonFtp;
    private ImageButton imageButtonState;
    private TextView textStateNetwork;
    private TextView textViewWifiBar;
    String ssid;
    NetworkInfo networkInfo;
    ConnectivityManager connManager;
    boolean firstBack = true;
    boolean haveNetwork = false;
    int strengthWifi = 0;
    private TextToSpeech mTts;
    boolean textToSpeech = false;
    boolean isSpeakWifiStateOn = true;
    boolean isSpeakWifiStateOff = true;
    String textInitToSpeak = "ALVIi, is in Offline Mode";
    String[] bufferTextSpeak = new String[30];
    int cntSpeak = 0;
    int cntSpeakId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        cntSpeak = 0;
        cntSpeakId = 0;
        isSpeakWifiStateOn = true;
        isSpeakWifiStateOff = true;
        mTts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS) {
                    mTts.setLanguage(Locale.UK);
                    textToSpeech = true;
                    //Log.i(TAG, "OK");
                }
            }
        });

        AlviiMain.mainActivity.finish();
        try{
            ImcLocation.fa_ImcLocation.finish();
        }catch (Exception io){}

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_mode);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        fa = this;

        textStateNetwork = (TextView) findViewById(R.id.textStateNetwork);
        barWifi = (ProgressBar) findViewById(R.id.progressBarWifi);
        barWifi.setMax(100);
        textViewWifiBar = (TextView) findViewById(R.id.textViewWifiBar);
        textViewWifiBar.setText("0 %");

        addListenerOnButton();
        customHandler = new android.os.Handler();
        customHandler.postDelayed(updateTimerThread,0);

        sendToSpeak(textInitToSpeak, true);
    }

    private void sendToSpeak(String text, boolean isToAdd){
        if (isToAdd) {
            bufferTextSpeak[cntSpeak] = text;
            cntSpeak++;
            if (cntSpeak >= 30)
                cntSpeak = 0;
        } else {
            if (!mTts.isSpeaking() && textToSpeech && cntSpeak != cntSpeakId) {
                mTts.speak(bufferTextSpeak[cntSpeakId], TextToSpeech.QUEUE_FLUSH, null);
                cntSpeakId++;
                if (cntSpeakId >= 30)
                    cntSpeakId = 0;
            }
        }
    }

    public void exitApp() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show();
        if(mTts !=null){
            mTts.stop();
            mTts.shutdown();
        }
        this.finish();
        System.exit(0);
    }

    //Run task periodically
    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            customHandler.postDelayed(this, 1000);
            if(checkStateNetwork()) {
                haveNetwork = true;
                textStateNetwork.setText(getCurrentSsid(OfflineMode.this));
                barWifi.setProgress(strengthWifi);
                textViewWifiBar.setText(""+strengthWifi+" %");
                imageButtonState.setImageResource(R.drawable.online_wifi);
                if(isSpeakWifiStateOn) {
                    sendToSpeak("wifi is on", true);
                    isSpeakWifiStateOn = false;
                    isSpeakWifiStateOff = true;
                }
                else {
                    if (textToSpeech)
                        sendToSpeak("", false);
                }
            }
            else {
                textStateNetwork.setText("Wifi Offline");
                barWifi.setProgress(0);
                textViewWifiBar.setText("");
                imageButtonState.setImageResource(R.drawable.offline_wifi);
                haveNetwork = false;
                if(isSpeakWifiStateOff) {
                    sendToSpeak("wifi is off", true);
                    isSpeakWifiStateOff = false;
                    isSpeakWifiStateOn = true;
                }
                else{
                    if(textToSpeech)
                        sendToSpeak("", false);
                }
            }
        }
    };

    private boolean checkStateNetwork(){
        return (isWifiAvailable() && isNetworkAvailable());
    }

    private boolean isWifiAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return  wifi.isConnected();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void addListenerOnButton() {
        imageButtonLog = (ImageButton) findViewById(R.id.imageButtonLog);
        imageButtonLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                firstBack = true;
                //Toast.makeText(OfflineMode.this, "imageButtonLog is clicked!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(OfflineMode.this, ReviewOldLogs.class);
                startActivity(intent);
            }
        });

        imageButtonFtp = (ImageButton) findViewById(R.id.imageButtonDownload);
        imageButtonFtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                firstBack = true;
                if(haveNetwork) {
                    //Toast.makeText(OfflineMode.this, "imageButtonFtp is clicked!", Toast.LENGTH_SHORT).show();
                    startFtp("0.0.0.0", "no name");
                }
                else{
                    Toast.makeText(OfflineMode.this, "No network available!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        imageButtonState = (ImageButton) findViewById(R.id.imageButtonWifiState);
        imageButtonState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                firstBack = true;
                if(haveNetwork) {
                    //Toast.makeText(OfflineMode.this, "imageButtonState is clicked!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(OfflineMode.this, ImcLocation.class);
                    startActivity(intent);
                }
                else{
                    Toast.makeText(OfflineMode.this, "No network available!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(firstBack){
                firstBack = false;
                Toast.makeText(this, "Press back again to exit", Toast.LENGTH_LONG).show();
            }
            else
            {
                exitApp();
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    public String getCurrentSsid(Context context) {
        ssid = "null";
        connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        networkInfo = connManager.getActiveNetworkInfo();
        if (networkInfo.isConnected()) {
            if (networkInfo.getType() == WIFI) {
                final WifiManager wifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
                final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                    ssid = connectionInfo.getSSID() + " - IP: " + Formatter.formatIpAddress(connectionInfo.getIpAddress());
                    strengthWifi = WifiManager.calculateSignalLevel(connectionInfo.getRssi(), 101);
                }
            } else if (networkInfo.getType() == MOBILE) {
                ssid = "Using mobile internet: " + networkInfo.getExtraInfo();
            }
        }
        return ssid;
    }

    public void startFtp(final String ip, final String system) {
        LayoutInflater inflater=OfflineMode.this.getLayoutInflater();
        View layout=inflater.inflate(R.layout.dialog_ftp,null);
        final EditText systemIp = layout.findViewById(R.id.dialogIp);
        final EditText systemPort = layout.findViewById(R.id.dialogPort);
        systemIp.setText(ip);
        final String portFtp = "30021";
        systemPort.setText(portFtp);

        AlertDialog.Builder builder1 = new AlertDialog.Builder(OfflineMode.this);
        builder1.setMessage("Values Ftp!");
        builder1.setCancelable(true);

        builder1.setPositiveButton("Submit Config", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                Intent intent = new Intent(OfflineMode.this, FtpClient.class);
                Bundle b = new Bundle();
                b.putString("BUNDLE_IP", systemIp.getText().toString());
                b.putString("BUNDLE_SYSTEM_PORT", systemPort.getText().toString());
                b.putString("BUNDLE_SYSTEM_NAME", system);
                intent.putExtras(b);
                intent.putExtra("EXTRA_LOCATION_NAME", "ftp");
                startActivity(intent);
            }
        });
        builder1.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder1.setView(layout);
        final AlertDialog alert11 = builder1.create();
        alert11.setOnShowListener( new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                alert11.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED);
                alert11.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GREEN);
            }
        });
        alert11.show();
    }
}
