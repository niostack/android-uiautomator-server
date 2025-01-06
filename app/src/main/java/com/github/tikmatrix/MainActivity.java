package com.github.tikmatrix;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.github.tikmatrix.util.MemoryManager;
import com.github.tikmatrix.util.OkhttpManager;
import com.github.tikmatrix.util.Permissons4App;


import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends Activity {
    public static final String TAG="TikMatrix";
    private TextView tvInStorage;
    private TextView textViewIP;
    private Switch switchNotification;
    private Switch switchFloatingWindow;
    private TextView tvWanIp;
    private TextView tvRunningStatus;


    private OkhttpManager okhttpManager = OkhttpManager.getSingleton();



    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((TextView) findViewById(R.id.product_name)).setText(Build.MANUFACTURER + " " + Build.MODEL);
        ((TextView) findViewById(R.id.android_system_version)).setText("Android " + Build.VERSION.RELEASE+" SDK " + Build.VERSION.SDK_INT);
        ((TextView) findViewById(R.id.language)).setText(Locale.getDefault().getCountry() + "-" + Locale.getDefault().getLanguage());
        ((TextView) findViewById(R.id.timezone)).setText(TimeZone.getDefault().getDisplayName());
        switchNotification = findViewById(R.id.notification_permission);
        switchFloatingWindow = findViewById(R.id.floating_window_permission);
        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestNotificationPermission();
            } else {
                NotificationManager notificationManager = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    notificationManager = getSystemService(NotificationManager.class);
                }
                if (notificationManager != null) {
                    notificationManager.cancelAll();
                }
            }
        });
        switchFloatingWindow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestFloatingWindowPermission();
            }
        });
        Intent intent = getIntent();
        String action=intent.getAction();
        Log.i(TAG, "action: " + action);
        boolean isHide = intent.getBooleanExtra("hide", false);
        if (isHide) {
            Log.i(TAG, "launch args hide:true, move to background");
            moveTaskToBack(true);
        }
        textViewIP = findViewById(R.id.ip_address);
        String ipAddress = getEthernetIpAddress();
        textViewIP.setText(ipAddress);
        textViewIP.setTextColor(Color.BLUE);
        tvInStorage = findViewById(R.id.in_storage);
        tvWanIp = findViewById(R.id.wan_ip_address);
        tvRunningStatus = findViewById(R.id.running_status);
        String[] permissions = new String[0];
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
        }else{
            permissions = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }
        Permissons4App.initPermissions(this, permissions);

        // register BroadcastReceiver
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction("com.github.tikmatrix.ACTION.SHOW_TOAST");
//        registerReceiver(new AdbBroadcastReceiver(), intentFilter, Context.RECEIVER_EXPORTED);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i(TAG, "onNewIntent: " + intent.getAction());
        if ("android.intent.action.SEND_MULTIPLE".equals(intent.getAction())) {
            //get extras
            String extras = intent.getStringExtra(Intent.EXTRA_STREAM);
            Log.i(TAG, "extras: " + extras);
            if (extras == null || extras.isEmpty()) {
                return;
            }
            ArrayList<Uri> uris = new ArrayList<Uri>();
            String packagename="com.zhiliaoapp.musically";
            for (String uriString : extras.split(",")) {
                Log.i(TAG, "uriString: " + uriString);
                if (uriString.startsWith("com.")) {
                    packagename = uriString;
                    continue;
                }
                File file = new File(uriString);
                Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
                Log.i(TAG, "uri: " + uri);
                uris.add(uri);
            }

            //send to com.zhiliaoapp.musically
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            sendIntent.setType("image/*");
            sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            sendIntent.setPackage(packagename);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(sendIntent);
            Log.d(TAG, "onReceive: sent to " + packagename);
            moveTaskToBack(true);
        }
    }
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.cancelAll();
            }
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        }
    }

    private void requestFloatingWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }

    }
    private boolean isNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return NotificationManagerCompat.from(this).areNotificationsEnabled();
        }
        return true;
    }
    private boolean isFloatingWindowPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Permissons4App.handleRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void testUiautomator() {
        //send boardcast to uiautomator
//        Intent intent = new Intent();
//        intent.setAction("com.github.tikmatrix.ACTION.SHOW_TOAST");
//        intent.putExtra("toast_text", "test uiautomator");
//        intent.putExtra("duration", 2000);
//        sendBroadcast(intent);
//        Log.i(TAG, "send boardcast to AdbBroadcastReceiver");

        String json = "{" +
                "            \"jsonrpc\": \"2.0\",\n" +
                "            \"id\": \"14d3bbb25360373624ea5b343c5abb1f\", \n" +
                "            \"method\": \"deviceInfo\",\n" +
                "            \"params\": []\n" +
                "        }";
        Request request = new Request.Builder()
                .url("http://127.0.0.1:9008/jsonrpc/0")
                .post(RequestBody.create(MediaType.parse("application/json"), json))
                .build();
        tvRunningStatus.setText("connecting...");
        okhttpManager.newCall(request, new Callback() {
            private void showFail() {
                runOnUiThread(() -> {
                    boolean isInstalled = Permissons4App.isAppInstalled(MainActivity.this, "com.github.tikmatrix.test");
                    if (!isInstalled) {
                        tvRunningStatus.setText("agent not installed, please click init app in the computer");
                        tvRunningStatus.setTextColor(Color.RED);
                    }else{
                        tvRunningStatus.setText("agent not start, please enable auto wake up in the computer");
                        tvRunningStatus.setTextColor(Color.RED);
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                    Log.e(TAG, e.toString());
                    showFail();
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (response.body() == null || !response.isSuccessful()) {
                        Log.e(TAG, response.toString());
                        showFail();
                        return;
                    }
                    String responseData = response.body().string();
                    Log.i(TAG, responseData);
                    runOnUiThread(() -> {
                        tvRunningStatus.setText("Success");
                        tvRunningStatus.setTextColor(Color.GREEN);
                    });

                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                    showFail();
                }
            }
        });
    }



    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();
        switchNotification.setChecked(isNotificationPermissionGranted());
        switchFloatingWindow.setChecked(isFloatingWindowPermissionGranted());
        tvInStorage.setText(Formatter.formatFileSize(this, MemoryManager.getAvailableInternalMemorySize()) + "/" + Formatter.formatFileSize(this, MemoryManager.getTotalExternalMemorySize()));
        checkNetworkAddress(null);
        testUiautomator();
    }
    public String getEthernetIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {

                // 排除回环接口和未活动的接口
                if (!networkInterface.isLoopback() && networkInterface.isUp()) {
                    List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
                    for (InetAddress address : addresses) {
                        if (!address.isLoopbackAddress() && address instanceof java.net.Inet4Address) {
                            if (Objects.equals(address.getHostAddress(), "0.0.0.0")) {
                                continue;
                            }
                            return address.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0.0.0.0";
    }
    public void checkNetworkAddress(View v) {
//        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        int ip = wifiManager.getConnectionInfo().getIpAddress();
//        String ipStr = (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
//        textViewIP.setText(ipStr);
//        textViewIP.setTextColor(Color.BLUE);
//
//        Log.i(TAG, "checkNetworkAddress: " + ipStr);
        //test
        Request request = new Request.Builder().url("https://api.tikmatrix.com/ip")
                .get()
                .build();
        okhttpManager.newCall(request, new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String content = response.body().string();
                    Log.i(TAG, content);
                    runOnUiThread(() -> {
                        tvWanIp.setText(content);
                        tvWanIp.setTextColor(Color.BLUE);
                    });
                }
            }
        });

    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // must unbind service, otherwise it will leak memory
        // connection no need to set it to null
        Log.i(TAG, "unbind service");
    }
}
