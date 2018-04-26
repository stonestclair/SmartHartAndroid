package com.norhart.smarthartandroid;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import android.view.ViewGroup.LayoutParams;

import com.norhart.smarthartandroid.devices.DoorSensor;
import com.norhart.smarthartandroid.devices.LightDimmer;
import com.norhart.smarthartandroid.devices.LightSwitch;
import com.norhart.smarthartandroid.devices.MultiSensor;
import com.norhart.smarthartandroid.devices.Thermostat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.OkHttpClient;

import static com.norhart.smarthartandroid.BasicUtils.containsIgnoreCase;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private X509TrustManager myTrustManager = new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[]{};
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
    };
    private final String SCHEME = "https";
    private final String HOST = "sysmap.systech.com";
    private final String PORT = "4443";
    private final String URI = "sysmap://systech.com/systech/1.0/GW968822/device";
    private final String socketIoUrl = SCHEME + "://" + HOST + ":" + PORT;
    private final String API_KEY = "CiJ2hKAXpk3t";
    private Socket socket;
    private JSONObject login = new JSONObject();
    private ArrayList<ZWaveDevice> deviceList = new ArrayList<>();

    //private TextView mTextMessage;
    private ImageView aptUnitFloorPlan;
    private ListView deviceListView;
    private LinearLayout layoutOfPopup;
    private PopupWindow popupMessage;
    private Button insidePopupButton;
    private TextView popupText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //mTextMessage = (TextView) findViewById(R.id.message);
        aptUnitFloorPlan = findViewById(R.id.gg_unit1a);
        deviceListView = findViewById(R.id.deviceList);
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    @Override
    protected void onStart() {
        init();
        popupInit();
        setupSSL();
        setupSocket();
        setupDeviceList();

        super.onStart();
    }

    public void init() {
        popupText = new TextView(this);
        insidePopupButton = new Button(this);
        layoutOfPopup = new LinearLayout(this);
        insidePopupButton.setText("OK");
        popupText.setText("Not logged in.");
        popupText.setPadding(0, 0, 0, 20);
        layoutOfPopup.setOrientation(LinearLayout.VERTICAL);
        layoutOfPopup.addView(popupText);
        layoutOfPopup.addView(insidePopupButton);
    }

    public void popupInit() {
        insidePopupButton.setOnClickListener(this);
        popupMessage = new PopupWindow(layoutOfPopup, LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        popupMessage.setContentView(layoutOfPopup);
        popupMessage.setBackgroundDrawable(new ColorDrawable(Color.GRAY));
    }

    private void setupSSL() {
        try {
            SSLContext mySSLContext = SSLContext.getInstance("TLS");
            TrustManager[] trustAllCerts = new TrustManager[]{myTrustManager};

            mySSLContext.init(null, trustAllCerts, null);

            HostnameVerifier myHostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .hostnameVerifier(myHostnameVerifier)
                    .sslSocketFactory(mySSLContext.getSocketFactory(), myTrustManager)
                    .build();

            // default settings for all sockets
            IO.setDefaultOkHttpWebSocketFactory(okHttpClient);
            IO.setDefaultOkHttpCallFactory(okHttpClient);

            // set as an option
            IO.Options opts = new IO.Options();
            opts.callFactory = okHttpClient;
            opts.webSocketFactory = okHttpClient;
            opts.reconnection = true;

            login.put("op", "login");
            login.put("key", API_KEY);
            Log.d("sio", login.toString());

            socket = IO.socket(socketIoUrl, opts);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }

    private void setupSocket() {
        // Socket.IO Listeners
        socket

                    /* ******************
                     * Connection Error *
                     ****************** */
                .on("error", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        JSONObject response = (JSONObject) args[0];
                        Log.d("SIO", response.toString());
                    }
                })

                    /* ********************
                     * Connection Success *
                     ******************** */
                .on("connect", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        socket.emit("control", login);
                    }
                })

                    /* ************************
                     * Login Attempt Response *
                     ************************ */
                .on("control", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        JSONObject response = (JSONObject) args[0];
                        Log.d("SIO", response.toString());

                        popupText.setText(response.toString());
                        findViewById(R.id.textView).post(new Runnable() {
                            public void run() {
                                popupMessage.showAtLocation(findViewById(R.id.textView), Gravity.CENTER, 0, 0);
                            }
                        });
                        getAllDevices(socket);
                    }
                })

                    /* ******************
                     * Sysmap Responses *
                     ****************** */
                .on("sysmap", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        if (args[0] instanceof JSONObject) {
                            JSONObject response = (JSONObject) args[0];
                            Log.d("sysmap", response.toString());
                            deviceList.clear();

                            try {
                                JSONArray body = response.getJSONArray("body");
                                if (response.getString("method").equals("POST")) {
                                    for (int i = 0; i < body.length(); i++) {
                                        JSONObject properties = body.getJSONObject(i).getJSONObject("properties");
                                        StringBuilder types = new StringBuilder();
                                        JSONArray typeArray = properties.getJSONObject("type").getJSONArray("value");
                                        for (int typeNum = 0; typeNum < typeArray.length(); typeNum++) {
                                            types.append(typeArray.getString(typeNum));
                                        }
                                        // Multi-Sensor
                                        if (containsIgnoreCase(types.toString(), "MultiSensor")) {
                                            MultiSensor newMultiSensor = new MultiSensor();
                                            newMultiSensor.setName(properties.getJSONObject("name").getString("value"));
                                            newMultiSensor.setId(properties.getString("id"));
                                            deviceList.add(newMultiSensor);
                                        }
                                        // Thermostat
                                        else if (containsIgnoreCase(types.toString(), "Thermostat")) {
                                            Thermostat newThermostat = new Thermostat();
                                            newThermostat.setName(properties.getJSONObject("name").getString("value"));
                                            newThermostat.setId(properties.getString("id"));
                                            deviceList.add(newThermostat);
                                        }
                                        // Light Dimmer and Light Switch
                                        else if (containsIgnoreCase(types.toString(), "Switch")) {
                                            if (properties.has("brightness")) {
                                                LightDimmer newLightDimmer = new LightDimmer();
                                                newLightDimmer.setName(properties.getJSONObject("name").getString("value"));
                                                newLightDimmer.setDimLevel(properties.getJSONObject("brightness").getInt("value"));
                                                newLightDimmer.setId(properties.getString("id"));
                                                deviceList.add(newLightDimmer);
                                            } else if (properties.has("switch")) {
                                                LightSwitch newLightSwitch = new LightSwitch();
                                                newLightSwitch.setName(properties.getJSONObject("name").getString("value"));
                                                newLightSwitch.setState(properties.getJSONObject("switch").getBoolean("value"));
                                                newLightSwitch.setId(properties.getString("id"));
                                                deviceList.add(newLightSwitch);
                                            }
                                        }
                                        // Door Sensor
                                        else if (containsIgnoreCase(types.toString(), "LogicalDevice")) {
                                            DoorSensor newDoorSensor = new DoorSensor();
                                            newDoorSensor.setName(properties.getJSONObject("name").getString("value"));
                                            newDoorSensor.setState(properties.getJSONObject("generalBinarySensorValue").getBoolean("value"));
                                            newDoorSensor.setId(properties.getString("id"));
                                            deviceList.add(newDoorSensor);
                                        } else {
                                            continue;
                                        }
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else if (args[0] instanceof JSONArray) {

                        }
                    }
                })
        ;
        socket.connect();
    }

    private void setupDeviceList() {
        ArrayAdapter<ZWaveDevice> arrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                deviceList
        );
        deviceListView.setAdapter(arrayAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Object item = deviceListView.getItemAtPosition(i);
                JSONObject sendAction = new JSONObject();
                JSONArray bodyFrame = new JSONArray();
                JSONObject body = new JSONObject();
                JSONObject property = new JSONObject();

                if (item instanceof ZWaveDevice) {
                    try {
                        sendAction.put("method", "POST");
                        sendAction.put("uri", URI);
                        body.put("op", "change");
                        body.put("ref", ((ZWaveDevice) item).getId());

                        if (item instanceof DoorSensor) {

                        } else if (item instanceof LightDimmer) {
                            int dim = ((LightDimmer) item).getDimLevel();
                            if (dim > 0) {
                                dim = 0;
                            } else {
                                dim = 100;
                            }
                            property.put("brightness", new JSONObject().put("value", dim).put("status", "pending"));
                        } else if (item instanceof LightSwitch) {
                            LightSwitch.State state = ((LightSwitch) item).getState();
                            property.put("switch", new JSONObject().put("value", !(state.getValue())).put("status", "pending"));
                        } else if (item instanceof MultiSensor) {

                        } else if (item instanceof Thermostat) {

                        }

                        body.put("properties", property);
                        bodyFrame.put(body);
                        sendAction.put("body", bodyFrame);
                        sendAction.put("callback", "listClick");

                        Log.d("SIO", sendAction.toString());
                        socket.emit("sysmap", sendAction);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                getAllDevices(socket);
            }
        });
    }

    private void getAllDevices(Socket socket) {
        JSONObject deviceRequest = new JSONObject();
        try {
            deviceRequest.put("method", "GET");
            deviceRequest.put("uri", URI);
            deviceRequest.put("callback", "getDevices");
            socket.emit("sysmap", deviceRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    deviceListView.setVisibility(View.INVISIBLE);
                    aptUnitFloorPlan.setVisibility(View.VISIBLE);
                    return true;
                case R.id.navigation_dashboard:
                    deviceListView.setVisibility(View.VISIBLE);
                    aptUnitFloorPlan.setVisibility(View.INVISIBLE);
                    return true;
                case R.id.navigation_notifications:
                    deviceListView.setVisibility(View.INVISIBLE);
                    aptUnitFloorPlan.setVisibility(View.INVISIBLE);
                    return true;
            }
            return false;
        }
    };

    @Override
    public void onClick(View v) {
        popupMessage.dismiss();
    }

    @Override
    public void onStop() {
        popupMessage.dismiss();
        super.onStop();
    }
}
