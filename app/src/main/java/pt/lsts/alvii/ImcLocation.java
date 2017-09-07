package pt.lsts.alvii;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.zerokol.views.JoystickView;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

import pt.lsts.accl.bus.AcclBus;
import pt.lsts.accl.util.pos.Position;
import pt.lsts.imc.EntityList;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanDB;
import pt.lsts.imc.SetThrusterActuation;
import pt.lsts.imc.Teleoperation;
import pt.lsts.imc.TeleoperationDone;

/**
 * Created by Pedro Gonçalves on 7/11/17.
 * LSTS - FEUP
 */
public class ImcLocation extends AppCompatActivity implements LocationListener, OnMapReadyCallback {
    public static Activity fa_ImcLocation;
    final Context context = this;
    public boolean result_permission = true;
    boolean deviceHaveSms;
    boolean firstBack = true;
    boolean isInitDone;
    private File storageDir = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "alvii");
    int longPressPos = 49;
    private int numberOfBufferPos = 60;
    private Position[] backPos = new Position[numberOfBufferPos];
    private int cntBackPos = 0;
    private Vibrator v;
    int timeVib = 80;
    private ImageView heartbeate;
    private ImageView direction;
    private boolean isToVib = false;
    private double minDistToPlot = 4;
    private double[] last_point = new double[2];
    MenuInflater inflater;
    private boolean isToShow = true;
    private JoystickView joystick;
    private boolean isInJoyMode = false;
    private boolean haveSysSelect = false;
    private String backMainSys;
    String TELEOPERATION = "teleoperation-mode";
    boolean firstRequestStopTeleop = true;

    //boolean isToShowAllSystem = false;
    boolean isToShowAllVehicles = false;
    boolean isToShowAllMantas = false;
    boolean isToShowAllCCU = false;

    //IMC
    private static final String TAG = "MEU IMC";
    private LocationManager locationManager;
    Double MyLat, MyLong;
    private GoogleMap mMap;
    private MarkerOptions host_marc_options;
    private Marker host_marc;
    private MarkerOptions sys_marc_options;
    private Marker sys_marc;
    private boolean first_lock;
    private Handler customHandler;
    private Handler customHandler_5s;
    private Handler customHandler_500ms;
    AcclBus acclBus;
    AcclBusListenner acclBusListenner;
    Intent imc_intent;
    ListView listView_sys;
    ArrayAdapter<String> adapter;
    private String[] sys_name;
    String[] sys_ip;
    private int sysSelected;
    public static int back_number_sys;
    boolean showAllsystem;
    private View rootView;
    int portImc = 6006;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        host_marc_options = new MarkerOptions().title("Me");
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private boolean isWifiAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifi.isConnected();
    }

    private void requestForSpecificPermission() {
        int PERMISSION_ALL = 101;
        String[] PERMISSIONS = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.SEND_SMS,
                //Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET};

        hasPermissions(this, PERMISSIONS);
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
    }

    public void hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                ActivityCompat.checkSelfPermission(context, permission);
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] == -1)
                result_permission = false;
        }
        if (!result_permission) {
            close_app();
        } else {
            if (!isNetworkAvailable()) {
                showErrorInfo("No network connection active!!!");
            } else {
                customHandler = new android.os.Handler();
                customHandler.postDelayed(updateTimerThread, 0);
                customHandler_5s = new android.os.Handler();
                customHandler_5s.postDelayed(updateTimerThread_5s, 0);
                customHandler_500ms = new android.os.Handler();
                customHandler_500ms.postDelayed(updateTimerThread_500ms, 0);
            }
        }
    }

    public void close_app() {
        Log.i(TAG, "close app");
        showErrorInfo("Please accept the permissions!!!\nIt is necessary to accept the permissions to run da app!!!");
    }

    //Run task periodically
    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            customHandler.postDelayed(this, 1000);
            if (isInitDone)
                try {
                    showSystem();
                    acclBusListenner.flushSpeek();
                    if (isInJoyMode) {
                        direction.setRotation((float) acclBusListenner.getHeadingSys(sys_name[longPressPos]));
                        if (!firstRequestStopTeleop) {
                            if (!acclBusListenner.isRunnigPlan(sys_name[longPressPos], TELEOPERATION)) {
                                firstBack = true;
                                isInJoyMode = false;
                                isToVib = false;
                                isToShow = true;
                                listView_sys.setVisibility(View.VISIBLE);
                                invalidateOptionsMenu();
                                joystick.setVisibility(View.INVISIBLE);
                                Toast.makeText(ImcLocation.this, "Stoping teleoperation request\n\tfor other CCU!", Toast.LENGTH_LONG).show();
                            }
                        } else
                            firstRequestStopTeleop = false;
                    } else
                        firstRequestStopTeleop = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
    };

    //Run task periodically 5 sec
    private Runnable updateTimerThread_5s = new Runnable() {
        public void run() {
            customHandler_5s.postDelayed(this, 5000);
            if (isInitDone)
                try {
                    if (acclBusListenner.getNumber_system() > 0) {
                        requestEntityList();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
    };

    //Run task periodically 500 msec
    private Runnable updateTimerThread_500ms = new Runnable() {
        public void run() {
            customHandler_500ms.postDelayed(this, 500);
            if (longPressPos != 49) {
                if (acclBusListenner.getHeartBeat(sys_name[longPressPos])) {
                    acclBusListenner.clearHeartBeat(sys_name[longPressPos]);
                    heartbeate.setImageResource(getResources().getIdentifier("@android:drawable/presence_online", null, null));
                    if (isToVib)
                        v.vibrate(timeVib);
                } else {
                    heartbeate.setImageResource(getResources().getIdentifier("@android:drawable/presence_invisible", null, null));
                }
            }
        }
    };

    public void requestEntityList() {
        EntityList msg = new EntityList();
        msg.setOpStr("QUERY");
        //short a = 1;
        //msg.setOpVal(a);
        acclBus.sendBroadcastMessage(msg);
    }

    public void requestPlanList() {
        PlanDB msg = new PlanDB();
        short REQUEST = 0;
        short GET_STATE = 5;
        msg.setTypeVal(REQUEST);
        msg.setOpVal(GET_STATE);
        msg.setRequestId(0);
        for (int i = 0; i < back_number_sys; i++) {
            if (!acclBusListenner.getSysType(sys_name[i]).equals("CCU")) {
                //Log.i(TAG, "SEND to: "+sys_name[i]);
                acclBus.sendMessage(msg, sys_name[i]);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);

        SubMenu themeMenu = menu.findItem(R.id.action_show_all_system).getSubMenu();
        themeMenu.clear();
        themeMenu.add(0, R.id.vehicles, Menu.NONE, "Vehicles").setCheckable(true).setChecked(false);
        themeMenu.add(0, R.id.mantas, Menu.NONE, "Mantas").setCheckable(true).setChecked(false);
        themeMenu.add(0, R.id.ccu, Menu.NONE, "CCU").setCheckable(true).setChecked(false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_printscreen:
                Toast.makeText(this, "Screen Captured!", Toast.LENGTH_SHORT).show();
                captureMapScreen();
                break;
            case R.id.action_clean_entity_buffer:
                acclBusListenner.cleanEntityBuffer();
                Toast.makeText(this, "Entity buffer clean!", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_review_data:
                Intent intent = new Intent(this, ReviewOldLogs.class);
                startActivity(intent);
                break;
            case R.id.action_manual_download:
                startFtp("0.0.0.0", "Manual Download");
                break;
            //TODO
            case R.id.vehicles:
                if(!isToShowAllVehicles) {
                    isToShowAllVehicles = true;
                    item.setChecked(true);
                }else{
                    isToShowAllVehicles = false;
                    item.setChecked(false);
                }
                break;
            case R.id.mantas:
                if(!isToShowAllMantas) {
                    isToShowAllMantas = true;
                    item.setChecked(true);
                }else{
                    isToShowAllMantas = false;
                    item.setChecked(false);
                }
                break;
            case R.id.ccu:
                if(!isToShowAllCCU) {
                    isToShowAllCCU = true;
                    item.setChecked(true);
                }else{
                    isToShowAllCCU = false;
                    item.setChecked(false);
                }
                break;
            case R.id.action_showSystem:
                isToShow = !isToShow;
                try {
                    listView_sys.setVisibility(View.GONE);
                    invalidateOptionsMenu();
                } catch (Exception io) {
                    isToShow = !isToShow;
                }
                break;
            case R.id.action_dont_showSystem:
                listView_sys.setVisibility(View.VISIBLE);
                isToShow = !isToShow;
                invalidateOptionsMenu();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        try {
            if (isToShow) {
                menu.findItem(R.id.action_dont_showSystem).setVisible(false);
                menu.findItem(R.id.action_showSystem).setVisible(true);
            } else {
                menu.findItem(R.id.action_dont_showSystem).setVisible(true);
                menu.findItem(R.id.action_showSystem).setVisible(false);
            }
        } catch (Exception ignored) {
        }

        return true;
    }

    public void captureMapScreen() {
        GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {

            @Override
            public void onSnapshotReady(Bitmap snapshot) {
                try {
                    rootView.setDrawingCacheEnabled(true);
                    Bitmap backBitmap = rootView.getDrawingCache();
                    Bitmap bmOverlay = Bitmap.createBitmap(
                            backBitmap.getWidth(), backBitmap.getHeight(),
                            backBitmap.getConfig());
                    Canvas canvas = new Canvas(bmOverlay);
                    canvas.drawBitmap(snapshot, new Matrix(), null);
                    canvas.drawBitmap(backBitmap, 0, 0, null);
                    Calendar c = Calendar.getInstance();
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_hh:mm:ss");
                    String formattedDate = df.format(c.getTime());
                    String filePath = Environment.getExternalStorageDirectory().getPath() + File.separator + "alvii/" + "screenCap_" + formattedDate + ".png";
                    FileOutputStream out = new FileOutputStream(filePath);
                    bmOverlay.compress(Bitmap.CompressFormat.PNG, 90, out);
                } catch (Exception e) {
                    Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        };
        mMap.snapshot(callback);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            AlviiMain.mainActivity.finish();
        } catch (Exception ignored) {
        }

        try {
            OfflineMode.fa.finish();
        } catch (Exception ignored) {
        }

        super.onCreate(savedInstanceState);
        fa_ImcLocation = this;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_imc_location);
        SupportMapFragment mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_imc));
        mapFragment.getMapAsync(this);
        v = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
        isInitDone = false;

        PackageManager pm = this.getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) || pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CDMA))
           deviceHaveSms = true;
        else
            deviceHaveSms = false;

        requestForSpecificPermission();
        if (isWifiAvailable()) {
            boolean success;
            if (!storageDir.exists()) {
                Log.i(TAG, "Creating foler");
                success = storageDir.mkdirs();
                if (success)
                    Log.i(TAG, "Folder created");
                else {
                    Log.i(TAG, "Error creating folder");
                    showErrorInfo("Error creating folder");
                }
            }
            init_layout();
            heartbeate = (ImageView) findViewById(R.id.imageHeartBeat);
            heartbeate.setVisibility(View.INVISIBLE);
            direction = (ImageView) findViewById(R.id.imagedirection);
            direction.setVisibility(View.INVISIBLE);
            joystick = (JoystickView) findViewById(R.id.joystick);
            joystick.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener() {

                @Override
                public void onValueChanged(int angle, int power, int direction) {
                    // TODO Auto-generated method stub
                    //Log.i(TAG," " + String.valueOf(angle) + "°");
                    //Log.i(TAG," " + String.valueOf(power) + "%");
                    switch (direction) {
                        case JoystickView.FRONT:
                        case JoystickView.FRONT_RIGHT:
                        case JoystickView.RIGHT:
                        case JoystickView.RIGHT_BOTTOM:
                        case JoystickView.BOTTOM:
                        case JoystickView.BOTTOM_LEFT:
                        case JoystickView.LEFT:
                        case JoystickView.LEFT_FRONT:
                            parseValuesToControl(power, angle, backMainSys);
                            break;

                        default:
                            Log.i(TAG, "is in center lab");
                            parseValuesToControl(0, 0, backMainSys);
                            break;
                    }
                }
            }, JoystickView.DEFAULT_LOOP_INTERVAL);
            if (!isInJoyMode) {
                joystick.setVisibility(View.INVISIBLE);
                direction.setVisibility(View.INVISIBLE);
            }
            isToShowAllVehicles = false;
            isToShowAllMantas = false;
            isToShowAllCCU = false;
            isInitDone = true;

            //send message func
            //sendSMS("+351xxxxxxxxx", "ALVIi hello :) ");

        } else {
            showErrorInfo("Please turn on Wifi");
        }
    }

    private void parseValuesToControl(double speed, int angle, String sysName) {
        //TODO
        if (isInJoyMode) {
            if (acclBusListenner.getSysType(sysName).equals("USV")) {
                if (angle >= 0 && angle <= 90) {
                    sendSpeedMotor(speed, (short) 0, sysName);
                    double m_value;
                    if (angle >= 0 && angle <= 45) {
                        int m_angle = 90 - angle;
                        m_value = ((m_angle * 100) / 90) * (speed / 100);
                        sendSpeedMotor(m_value, (short) 1, sysName);
                    } else if (angle > 45 && angle <= 90) {
                        m_value = ((angle * 100) / 90) * (speed / 100);
                        sendSpeedMotor(-m_value, (short) 1, sysName);
                    }
                } else if (angle >= -90 && angle < 0) {
                    sendSpeedMotor(speed, (short) 1, sysName);
                    double m_value;
                    if (angle >= -45 && angle < 0) {
                        int m_angle = 90 + angle;
                        m_value = ((m_angle * 100) / 90) * (speed / 100);
                        sendSpeedMotor(m_value, (short) 0, sysName);
                    } else if (angle >= -90 && angle < -45) {
                        int m_angle = -angle;
                        m_value = ((m_angle * 100) / 90) * (speed / 100);
                        sendSpeedMotor(-m_value, (short) 0, sysName);
                    }
                } else if (angle >= -179 && angle < -90) {
                    sendSpeedMotor(-speed, (short) 1, sysName);
                    double m_value;
                    if (angle >= -135 && angle < -90) {
                        int m_angle = 180 + angle;
                        m_value = ((m_angle * 100) / 90) * (speed / 100);
                        sendSpeedMotor(m_value, (short) 0, sysName);
                    } else if (angle >= -179 && angle < -135) {
                        int m_angle = -angle - 90;
                        m_value = -((m_angle * 100) / 90) * (speed / 100);
                        sendSpeedMotor(m_value, (short) 0, sysName);
                    }
                } else if (angle > 90 && angle <= 180) {
                    sendSpeedMotor(-speed, (short) 0, sysName);
                    double m_value;
                    if (angle > 90 && angle <= 135) {
                        int m_angle = 180 - angle;
                        m_value = ((m_angle * 100) / 90) * (speed / 100);
                        sendSpeedMotor(m_value, (short) 1, sysName);
                    } else if (angle > 135 && angle <= 180) {
                        int m_angle = 180 - angle + 90;
                        m_value = -((m_angle * 100) / 90) * (speed / 100);
                        sendSpeedMotor(m_value, (short) 1, sysName);
                    }
                }
            }/* else if (acclBusListenner.getSysType(sysName).equals("UUV")) {

            }*/
        }
    }

    private void sendSpeedMotor(double speed, short idMotor, String sysName) {
        SetThrusterActuation msg = new SetThrusterActuation();
        msg.setSrc(acclBus.getLocalIdImc());
        msg.setId(idMotor);
        msg.setValue(speed / 100.f);
        acclBus.sendMessage(msg, sysName);
    }

    void showErrorInfo(String text) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setTitle("Error !!!");
        alertDialogBuilder.setMessage(text)
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ImcLocation.this.finish();
                        System.exit(0);
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    void init_layout() {
        first_lock = true;
        showAllsystem = true;
        sysSelected = -1;
        back_number_sys = 0;

        turnGPSOn();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, this);

        startImc();
        customHandler = new android.os.Handler();
        customHandler.postDelayed(updateTimerThread, 0);

        rootView = getWindow().getDecorView().findViewById(R.id.map_imc);
        acclBusListenner.init_systm_info();
    }

    public void startImc() {
        Log.i(TAG, "Start IMC");
        TelephonyManager mngr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String imei = mngr.getDeviceId();
        acclBus.bind("ccu-" + android.os.Build.MODEL, portImc, convertSImeiToID(imei));
        acclBusListenner = new AcclBusListenner();
        imc_intent = new Intent(this, AcclBusListenner.class);
        acclBusListenner.onStartCommand(imc_intent, 0, 0);
        acclBusListenner.onBind(imc_intent);
        acclBus.register(acclBusListenner);
    }

    int convertSImeiToID(String text) {
        if(text == null)
            text = Calendar.getInstance().getTime().toString();

        int sum = 0;
        char imei[] = text.toCharArray();
        for (int i = 0; i < text.length(); i++)
            sum = sum + imei[i];
        return 0x4000 + sum;
    }

    // turning off the GPS if its in on state. to avoid the battery drain.
    @Override
    protected void onDestroy() {
        locationManager.removeUpdates(this);
        super.onDestroy();
        turnGPSOff();
        this.finish();
        AcclBus.onPause();
        Log.i(TAG, "STOP IMC");
    }

    @Override
    public void onLocationChanged(Location location) {
        MyLat = location.getLatitude();
        MyLong = location.getLongitude();
        if (first_lock) {
            host_marc_options.position(new LatLng(MyLat, MyLong));
            host_marc = mMap.addMarker(host_marc_options);
            //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(MyLat, MyLong), 17));
            host_marc.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            first_lock = false;
        } else {
            host_marc.setPosition(new LatLng(MyLat, MyLong));
            host_marc.showInfoWindow();
        }
    }

    private void showSystem() {
        if (acclBusListenner.getNumber_system() > 0 && acclBusListenner.getNumber_system() > back_number_sys) {
            back_number_sys = acclBusListenner.getNumber_system();
            int size = acclBusListenner.getNumber_system();
            sys_name = Arrays.copyOf(acclBusListenner.getSystem_names(), size);
            sys_ip = Arrays.copyOf(acclBusListenner.getSystem_ip(), size);

            listView_sys = (ListView) findViewById(R.id.sys_list);
            adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, sys_name) {

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView textView = view.findViewById(android.R.id.text1);
                    if (acclBusListenner.isAlive(sys_name[position]) && !acclBusListenner.m_system_Info.system_profile[position].getSysType().toString().equals("CCU")
                            && !acclBusListenner.m_system_Info.system_profile[position].getSysType().toString().equals("MOBILESENSOR"))
                        textView.setTextColor(Color.rgb(0, 150, 0));
                    else if (acclBusListenner.isAlive(sys_name[position]))
                        textView.setTextColor(Color.BLUE);
                    else
                        textView.setTextColor(Color.RED);

                    return view;
                }
            };
            listView_sys.setAdapter(adapter);
            listView_sys.setLongClickable(true);
            listView_sys.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    longPressPos = position;
                    firstBack = true;
                    acclBusListenner.setSelectedSystem(null);
                    backMainSys = null;
                    if (acclBusListenner.getPositionSystem(sys_name[position]) == null) {
                        Toast.makeText(getApplicationContext(), "< " + sys_name[position] + " > Waiting for position", Toast.LENGTH_SHORT).show();
                    } else {
                        haveSysSelect = true;
                        Position pos = acclBusListenner.getPositionSystem(sys_name[position]);
                        if (!acclBusListenner.m_system_Info.system_profile[position].getSysType().toString().equals("CCU")) {
                            if (sysSelected != position) {
                                for (int i = 0; i <= numberOfBufferPos - 1; i++) {
                                    backPos[i] = null;
                                }
                                cntBackPos = 0;
                                backPos[0] = pos;
                                last_point[0] = backPos[0].getLatitude();
                                last_point[1] = backPos[0].getLongitude();
                                cntBackPos++;
                            }
                            sysSelected = position;
                        } else {
                            if (sysSelected != position)
                                sysSelected = position;
                        }
                        mMap.clear();
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        mMap.setMyLocationEnabled(true);

                        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                            @Override
                            public void onCameraIdle() {
                                float zoomLevel = mMap.getCameraPosition().zoom;
                                //Log.i(TAG, "zoom: "+zoomLevel);
                                if (zoomLevel <= 20)
                                    minDistToPlot = 20 - zoomLevel;
                            }
                        });
                        if (acclBusListenner.m_system_Info.system_profile[position].getSysType().toString().equals("UUV"))
                            sys_marc_options = new MarkerOptions().title(sys_name[position]).icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_auv));
                        else if (acclBusListenner.m_system_Info.system_profile[position].getSysType().toString().equals("USV"))
                            sys_marc_options = new MarkerOptions().title(sys_name[position]).icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_usv));
                        else if (acclBusListenner.m_system_Info.system_profile[position].getSysType().toString().equals("UAV"))
                            sys_marc_options = new MarkerOptions().title(sys_name[position]).icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_uav));
                        else if (acclBusListenner.m_system_Info.system_profile[position].getSysType().toString().equals("CCU"))
                            sys_marc_options = new MarkerOptions().title(sys_name[position]).icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_ccu));
                        else
                            sys_marc_options = new MarkerOptions().title(sys_name[position]).icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_unknown));

                        sys_marc_options.position(new LatLng(0, 0));
                        sys_marc = mMap.addMarker(sys_marc_options);
                        sys_marc.setVisible(false);

                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(pos.getLatitude(), pos.getLongitude()), 16.0f));
                        sys_marc = mMap.addMarker(sys_marc_options);
                        sys_marc.setPosition(new LatLng(pos.getLatitude(), pos.getLongitude()));
                        sys_marc.showInfoWindow();
                        sys_marc.setVisible(true);
                    }
                }
            });

            listView_sys.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int pos, long l) {
                    longPressPos = pos;
                    return false;
                }
            });

            registerForContextMenu(listView_sys);
        }

        if (acclBusListenner.getNumber_system() > 0)
            acclBusListenner.updateCcu();

        updateLocationsystem();
        if (sysSelected >= 0)
            showLiteInfoSystem(sys_name[sysSelected], sysSelected);
    }

    private void showLiteInfoSystem(String sys_name, int idSys) {
        TextView info = (TextView) findViewById(R.id.textLiteInfoSystem);
        if (!acclBusListenner.m_system_Info.system_profile[idSys].getSysType().toString().equals("CCU")) {
            String infoText = "    CPU: " + acclBusListenner.getCpuUsage(sys_name) + " %\n" +
                    "    Disk: " + acclBusListenner.getHddUsage(sys_name) + " %\n" +
                    "    Speed: " + String.format("%.2f", acclBusListenner.getSpeed(sys_name)) + " m/s \n" +
                    "    Fuel: " + String.format("%.2f", acclBusListenner.getFuelUsage(sys_name)) + " %\n" +
                    "    Plan: " + acclBusListenner.getPlanControlState(sys_name) + "    ";
            info.setTextColor(Color.BLUE);
            info.setText(infoText);
            heartbeate.setVisibility(View.VISIBLE);
            //isToVib = true;
        } else {
            heartbeate.setVisibility(View.INVISIBLE);
            info.setText("");
            isToVib = false;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Options: " + sys_name[longPressPos] + " (" + acclBusListenner.getSystemIpByName(sys_name[longPressPos]) + " )");
        menu.add(0, v.getId(), 0, "View Log/Ftp");//groupId, itemId, order, title
        menu.add(0, v.getId(), 0, "Info of system");
        menu.add(0, v.getId(), 0, "View System Plans");
        menu.add(0, v.getId(), 0, "TeleOperation");

        if (acclBusListenner.getPositionSystem(sys_name[longPressPos]) == null) {
            if (acclBusListenner.getSysType(sys_name[longPressPos]).equals("CCU")) {
                menu.getItem(0).setEnabled(false);
                menu.getItem(2).setEnabled(false);
                menu.getItem(3).setEnabled(false);
            } else {
                menu.getItem(3).setEnabled(false);
                menu.getItem(2).setEnabled(true);
                menu.getItem(0).setEnabled(true);
            }

            if (acclBusListenner.getEntityCnt(sys_name[longPressPos]) > 0) {
                menu.getItem(1).setEnabled(true);
            } else {
                menu.getItem(1).setEnabled(false);
            }
        } else {
            if (acclBusListenner.getSysType(sys_name[longPressPos]).equals("CCU")) {
                menu.getItem(0).setEnabled(false);
                menu.getItem(2).setEnabled(false);
                menu.getItem(3).setEnabled(false);
            } else if (acclBusListenner.getSysType(sys_name[longPressPos]).equals("UAV") || acclBusListenner.getSysType(sys_name[longPressPos]).equals("UUV")) {
                menu.getItem(3).setEnabled(false);
                menu.getItem(2).setEnabled(true);
                menu.getItem(0).setEnabled(true);
            } else {
                menu.getItem(3).setEnabled(true);
                menu.getItem(2).setEnabled(true);
                menu.getItem(0).setEnabled(true);
            }

            if (acclBusListenner.getEntityCnt(sys_name[longPressPos]) > 0) {
                menu.getItem(1).setEnabled(true);
            } else {
                menu.getItem(1).setEnabled(false);
            }

            if (!acclBusListenner.m_system_Info.system_profile[longPressPos].getSysType().toString().equals("CCU")) {
                for (int i = 0; i <= numberOfBufferPos - 1; i++) {
                    backPos[i] = null;
                }
                cntBackPos = 0;
                backPos[0] = acclBusListenner.getPositionSystem(sys_name[longPressPos]);
                last_point[0] = backPos[0].getLatitude();
                last_point[1] = backPos[0].getLongitude();
                cntBackPos++;
                sysSelected = longPressPos;

                mMap.clear();
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mMap.setMyLocationEnabled(true);
                mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                    @Override
                    public void onCameraIdle() {
                        float zoomLevel = mMap.getCameraPosition().zoom;
                        //Log.i(TAG, "zoom: "+zoomLevel);
                        if (zoomLevel <= 20)
                            minDistToPlot = 20 - zoomLevel;
                    }
                });
                if (acclBusListenner.m_system_Info.system_profile[longPressPos].getSysType().toString().equals("UUV"))
                    sys_marc_options = new MarkerOptions().title(sys_name[longPressPos]).icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_auv));
                else if (acclBusListenner.m_system_Info.system_profile[longPressPos].getSysType().toString().equals("USV"))
                    sys_marc_options = new MarkerOptions().title(sys_name[longPressPos]).icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_usv));
                else if (acclBusListenner.m_system_Info.system_profile[longPressPos].getSysType().toString().equals("UAV"))
                    sys_marc_options = new MarkerOptions().title(sys_name[longPressPos]).icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_uav));
                else if (acclBusListenner.m_system_Info.system_profile[longPressPos].getSysType().toString().equals("CCU"))
                    sys_marc_options = new MarkerOptions().title(sys_name[longPressPos]).icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_ccu));
                else
                    sys_marc_options = new MarkerOptions().title(sys_name[longPressPos]).icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_unknown));

                sys_marc_options.position(new LatLng(0, 0));
                sys_marc = mMap.addMarker(sys_marc_options);
                sys_marc.setVisible(false);

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(backPos[0].getLatitude(), backPos[0].getLongitude()), 16.0f));
                sys_marc = mMap.addMarker(sys_marc_options);
                sys_marc.setPosition(new LatLng(backPos[0].getLatitude(), backPos[0].getLongitude()));
                sys_marc.showInfoWindow();
                sys_marc.setVisible(true);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        firstBack = true;
        if (item.getTitle() == "View Log/Ftp") {
            Log.i(TAG, sys_ip[longPressPos] + " : " + sys_name[longPressPos]);
            startFtp(sys_ip[longPressPos], sys_name[longPressPos]);
        } else if (item.getTitle() == "Info of system") {
            try {
                int cnt = acclBusListenner.getEntityCnt(sys_name[longPressPos]);
                int id = longPressPos;
                if (cnt > 0 && !acclBusListenner.m_system_Info.system_profile[id].getSysType().toString().equals("CCU")) {
                    String[] entityNames = Arrays.copyOf(acclBusListenner.getEntityNames(sys_name[longPressPos]), cnt);
                    String[] entityState = Arrays.copyOf(acclBusListenner.getEntityState(sys_name[longPressPos]), cnt);
                    final String[] entityMode = Arrays.copyOf(acclBusListenner.getEntityMode(sys_name[longPressPos]), cnt);
                    String[] entityText = new String[cnt];
                    for (int a = 0; a < cnt; a++) {
                        entityText[a] = entityNames[a] + " | " + entityState[a];
                    }
                    Dialog listDialog;
                    listDialog = new Dialog(context, R.style.Dialog);
                    listDialog.setTitle(sys_name[longPressPos]);
                    LayoutInflater li = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View v = li.inflate(R.layout.custom_dialog, null, false);
                    listDialog.setContentView(v);
                    listDialog.setCancelable(true);
                    ListView list1 = listDialog.findViewById(R.id.entity_list);
                    ArrayAdapter m_adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, entityText) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
                            TextView textView = view.findViewById(android.R.id.text1);
                            switch (entityMode[position]) {
                                case "BOOT":
                                    textView.setTextColor(Color.BLUE);
                                    break;
                                case "NORMAL":
                                    textView.setTextColor(Color.rgb(22, 170, 60));
                                    break;
                                default:
                                    textView.setTextColor(Color.RED);
                                    break;
                            }
                            return view;
                        }
                    };
                    list1.setAdapter(m_adapter);
                    listDialog.show();
                } else
                    Toast.makeText(getApplicationContext(), "No info available,\nPlease try again in 10 seconds!", Toast.LENGTH_SHORT).show();
            } catch (Exception io) {
                Toast.makeText(getApplicationContext(), "No info available", Toast.LENGTH_SHORT).show();
            }
        } else if (item.getTitle() == "View System Plans") {
            int size = acclBusListenner.getNumberPlanSystem(sys_name[longPressPos]);
            if (size <= 0) {
                requestPlanList();
                Toast.makeText(getApplicationContext(), "No info available,\nPlease try again in 10 seconds!", Toast.LENGTH_SHORT).show();
            } else {
                String[] planListName = Arrays.copyOf(acclBusListenner.getPlanList(sys_name[longPressPos]), size);
                /*for (int i = 0; i < size; i++) {
                    Log.i(TAG, planListName[i]);
                }*/
                showDialogList("Plans List: " + sys_name[longPressPos], planListName);
            }
        } else if (item.getTitle() == "TeleOperation") {
            //TODO
            if (haveSysSelect) {
                Toast.makeText(getApplicationContext(), "TeleOperation - " + sys_name[longPressPos], Toast.LENGTH_SHORT).show();
                acclBusListenner.setSelectedSystem(sys_name[longPressPos]);
                listView_sys.setVisibility(View.GONE);
                isInJoyMode = true;
                isToVib = true;
                isToShow = false;
                invalidateOptionsMenu();
                joystick.setVisibility(View.VISIBLE);
                direction.setVisibility(View.VISIBLE);
                backMainSys = sys_name[longPressPos];
                startTeleOp(backMainSys);
            } else {
                haveSysSelect = false;
                Toast.makeText(getApplicationContext(), "Please select system!", Toast.LENGTH_SHORT).show();
            }
        } else {
            return false;
        }
        return true;
    }

    private void showDialogList(String title, String[] list) {
        Dialog listDialog;
        listDialog = new Dialog(context, R.style.Dialog);
        listDialog.setTitle(title);
        LayoutInflater li = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = li.inflate(R.layout.custom_dialog, null, false);
        listDialog.setContentView(v);
        listDialog.setCancelable(true);
        ListView list1 = listDialog.findViewById(R.id.entity_list);
        ArrayAdapter m_adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);
        list1.setAdapter(m_adapter);
        listDialog.show();
    }

    void updateLocationsystem() {
        if (sysSelected < 0)
            return;

        mMap.clear();
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);

        if (isInJoyMode) {
            mMap.getUiSettings().setRotateGesturesEnabled(false);
        } else {
            mMap.getUiSettings().setRotateGesturesEnabled(true);
        }

        if (!isToShowAllVehicles && !isToShowAllCCU && !isToShowAllMantas) {
            Position pos = acclBusListenner.getPositionSystem(sys_name[sysSelected]);

            if (acclBusListenner.m_system_Info.system_profile[sysSelected].getSysType().toString().equals("UUV"))
                sys_marc_options = new MarkerOptions().title(sys_name[sysSelected]).icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_auv));
            else if (acclBusListenner.m_system_Info.system_profile[sysSelected].getSysType().toString().equals("USV"))
                sys_marc_options = new MarkerOptions().title(sys_name[sysSelected]).icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_usv));
            else if (acclBusListenner.m_system_Info.system_profile[sysSelected].getSysType().toString().equals("UAV"))
                sys_marc_options = new MarkerOptions().title(sys_name[sysSelected]).icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_uav));
            else if (acclBusListenner.m_system_Info.system_profile[sysSelected].getSysType().toString().equals("CCU"))
                sys_marc_options = new MarkerOptions().title(sys_name[sysSelected]).icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_ccu));
            else
                sys_marc_options = new MarkerOptions().title(sys_name[sysSelected]).icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_unknown));

            sys_marc_options.position(new LatLng(0, 0));
            sys_marc = mMap.addMarker(sys_marc_options);
            sys_marc.setVisible(false);
            sys_marc = mMap.addMarker(sys_marc_options);
            sys_marc.setPosition(new LatLng(pos.getLatitude(), pos.getLongitude()));
            sys_marc.showInfoWindow();
            sys_marc.setVisible(true);

            if (!acclBusListenner.m_system_Info.system_profile[sysSelected].getSysType().toString().equals("CCU") && !acclBusListenner.m_system_Info.system_profile[sysSelected].getSysType().toString().equals("MOBILESENSOR")) {
                if (distance(pos.getLatitude(), last_point[0], pos.getLongitude(), last_point[1], 0, 0) >= minDistToPlot) {
                    //Log.i(TAG, ""+distance(pos.getLatitude(), last_point[0], pos.getLongitude(), last_point[1], 0, 0));
                    backPos[cntBackPos] = pos;
                    last_point[0] = backPos[cntBackPos].getLatitude();
                    last_point[1] = backPos[cntBackPos].getLongitude();
                    cntBackPos++;
                }

                if (cntBackPos >= numberOfBufferPos)
                    cntBackPos = 0;

                for (int i = 0; i <= numberOfBufferPos - 1; i++) {
                    if (backPos[i] != null && acclBusListenner.getSpeed(sys_name[sysSelected]) > 0) {
                        sys_marc_options = new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.point));
                        sys_marc_options.position(new LatLng(0, 0));
                        sys_marc = mMap.addMarker(sys_marc_options);
                        sys_marc.setVisible(false);
                        sys_marc = mMap.addMarker(sys_marc_options);
                        sys_marc.setPosition(new LatLng(backPos[i].getLatitude(), backPos[i].getLongitude()));
                        sys_marc.setVisible(true);
                    }
                }
            }
        } else {
            //TODO
            if(isToShowAllVehicles)
                addAndShowLastPos("vehicles");
            if(isToShowAllMantas)
                addAndShowLastPos("mantas");
            if(isToShowAllCCU)
                addAndShowLastPos("ccu");

        }
    }

    void addAndShowLastPos(String systemToShow){
        boolean addInfo = false;
        for (int i = 0; i < back_number_sys; i++) {
            switch (systemToShow){
                case "vehicles":
                    if(acclBusListenner.m_system_Info.system_profile[i].getSysType().toString().equals("UUV")
                            || acclBusListenner.m_system_Info.system_profile[i].getSysType().toString().equals("USV")
                            || acclBusListenner.m_system_Info.system_profile[i].getSysType().toString().equals("UAV"))
                        addInfo = true;
                    else
                        addInfo = false;
                    break;

                case "mantas":
                    if(acclBusListenner.m_system_Info.system_profile[i].getSysType().toString().equals("MOBILESENSOR"))
                        addInfo = true;
                    else
                        addInfo = false;
                    break;

                case "ccu":
                    if(acclBusListenner.m_system_Info.system_profile[i].getSysType().toString().equals("CCU"))
                        addInfo = true;
                    else
                        addInfo = false;
                    break;
            }

            if(addInfo){
                Position pos = acclBusListenner.getPositionSystem(sys_name[i]);

                if (acclBusListenner.m_system_Info.system_profile[i].getSysType().toString().equals("UUV"))
                    sys_marc_options = new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_auv));
                else if (acclBusListenner.m_system_Info.system_profile[i].getSysType().toString().equals("USV"))
                    sys_marc_options = new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_usv));
                else if (acclBusListenner.m_system_Info.system_profile[i].getSysType().toString().equals("UAV"))
                    sys_marc_options = new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_uav));
                else if (acclBusListenner.m_system_Info.system_profile[i].getSysType().toString().equals("CCU"))
                    sys_marc_options = new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_ccu));
                else
                    sys_marc_options = new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ico_unknown));

                sys_marc_options.position(new LatLng(0, 0));
                sys_marc = mMap.addMarker(sys_marc_options);
                //sys_marc.setVisible(false);
                sys_marc = mMap.addMarker(sys_marc_options);
                sys_marc.setPosition(new LatLng(pos.getLatitude(), pos.getLongitude()));
                sys_marc.showInfoWindow();
                sys_marc.setVisible(true);

                if (!acclBusListenner.m_system_Info.system_profile[i].getSysType().toString().equals("CCU") && !acclBusListenner.m_system_Info.system_profile[sysSelected].getSysType().toString().equals("MOBILESENSOR")) {
                    if (distance(pos.getLatitude(), last_point[0], pos.getLongitude(), last_point[1], 0, 0) >= minDistToPlot) {
                        //Log.i(TAG, ""+distance(pos.getLatitude(), last_point[0], pos.getLongitude(), last_point[1], 0, 0));
                        backPos[cntBackPos] = pos;
                        last_point[0] = backPos[cntBackPos].getLatitude();
                        last_point[1] = backPos[cntBackPos].getLongitude();
                        cntBackPos++;
                    }

                    if (cntBackPos >= numberOfBufferPos)
                        cntBackPos = 0;

                    for (int ii = 0; ii <= numberOfBufferPos - 1; ii++) {
                        if (backPos[ii] != null && acclBusListenner.getSpeed(sys_name[i]) > 0) {
                            sys_marc_options = new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.point));
                            sys_marc_options.position(new LatLng(0, 0));
                            sys_marc = mMap.addMarker(sys_marc_options);
                            sys_marc.setVisible(false);
                            sys_marc = mMap.addMarker(sys_marc_options);
                            sys_marc.setPosition(new LatLng(backPos[ii].getLatitude(), backPos[ii].getLongitude()));
                            sys_marc.setVisible(true);
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     * <p>
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     *
     * @returns Distance in Meters
     */
    public static double distance(double lat1, double lat2, double lon1, double lon2, double el1, double el2) {
        final int R = 6371; // Radius of the earth
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;
        distance = Math.pow(distance, 2) + Math.pow(height, 2);
        return Math.sqrt(distance);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(getBaseContext(), "Gps is off, please turn on ", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(getBaseContext(), "Gps turned on ", Toast.LENGTH_LONG).show();
        turnGPSOn();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    public void turnGPSOn() {
        Log.i(TAG, "GPS ON");
        try {
            String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            if (!provider.contains("gps")) { //if gps is disabled
                final Intent poke = new Intent();
                poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
                poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
                poke.setData(Uri.parse("3"));
                sendBroadcast(poke);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void turnGPSOff() {
        String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (provider.contains("gps")) { //if gps is enabled
            Log.i(TAG, "GPS OFF");
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            sendBroadcast(poke);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!isInJoyMode) {
                if (firstBack) {
                    firstBack = false;
                    Toast.makeText(this, "Press again to exit", Toast.LENGTH_LONG).show();
                } else {
                    exitApp();
                }
                return false;
            } else {
                stopTeleOp(backMainSys);
                firstBack = true;
                isInJoyMode = false;
                isToVib = false;
                isToShow = true;
                listView_sys.setVisibility(View.VISIBLE);
                invalidateOptionsMenu();
                joystick.setVisibility(View.INVISIBLE);
                direction.setVisibility(View.INVISIBLE);
                return false;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void exitApp() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show();
        turnGPSOff();
        ImcLocation.this.finish();
        System.exit(0);
    }

    public void startFtp(final String ip, final String system) {
        LayoutInflater inflater = ImcLocation.this.getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_ftp, null);
        final EditText systemIp = layout.findViewById(R.id.dialogIp);
        final EditText systemPort = layout.findViewById(R.id.dialogPort);
        systemIp.setText(ip);
        final String portFtp = "30021";
        systemPort.setText(portFtp);

        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
        builder1.setMessage("Values Ftp!");
        builder1.setCancelable(true);

        builder1.setPositiveButton("Submit Config", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                Intent intent = new Intent(ImcLocation.this, FtpClient.class);
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
        alert11.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                alert11.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED);
                alert11.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GREEN);
            }
        });
        alert11.show();
    }

    private void startTeleOp(String sysName) {
        try {
            IMCDefinition.getInstance().create("Teleoperation").getMgid();
        } catch (Exception e) {
            Log.i(TAG, "1: ", e);
        }

        IMCMessage msg;
        try {
            msg = IMCDefinition.getInstance().create("RemoteActionsRequest", "op", 1);
            acclBus.sendMessage(msg, sysName);
        } catch (Exception e) {
            Log.i(TAG, "2: ", e);
        }

        int reqId = 99;
        Teleoperation teleoperationMsg = new Teleoperation();
        teleoperationMsg.setCustom("src=" + acclBus.getLocalIdImc());
        PlanControl msg_p = new PlanControl();
        msg_p.setType(PlanControl.TYPE.REQUEST);
        msg_p.setOp(PlanControl.OP.START);
        msg_p.setFlags(0);
        msg_p.setRequestId(reqId);
        msg_p.setPlanId(TELEOPERATION);
        msg_p.setArg(teleoperationMsg);
        acclBus.sendMessage(msg_p, sysName);
        acclBusListenner.sendToSpeak(sysName + ", is in teleoperation mode", true);
    }

    private void stopTeleOp(String sysName) {
        Log.i(TAG, "STOP: " + sysName);
        acclBus.sendMessage(new TeleoperationDone(), sysName);
        acclBusListenner.sendToSpeak(sysName + ", is in service mode", true);
    }

    public boolean sendSMS(String toNum, String smsText) {
        try
        {
            Toast.makeText(getApplicationContext(),"Number: "+ toNum + "  #  Text: " + smsText,Toast.LENGTH_SHORT).show();
            try{
                SmsManager sms = SmsManager.getDefault();
                sms.sendTextMessage(toNum, null, smsText, null, null);
            }catch (Exception e){
                Log.i(TAG, ""+e);
            }
            return true;
        }
        catch (Exception e)
        {
            Toast.makeText(getApplicationContext(),"SMS faild, please try again later!",Toast.LENGTH_SHORT).show();
            Log.i(TAG, ""+e);
        }
       return false;
    }
}
