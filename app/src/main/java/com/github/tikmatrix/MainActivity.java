package com.github.tikmatrix;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.tikmatrix.util.MemoryManager;
import com.github.tikmatrix.util.OkhttpManager;
import com.github.tikmatrix.util.Permissons4App;


import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends Activity {
    public static final String TAG="TIKMATRIX";
    private TextView tvInStorage;
    private TextView textViewIP;
    private TextView tvServiceMessage;


    private OkhttpManager okhttpManager = OkhttpManager.getSingleton();

    private static final class TextViewSetter implements Runnable {
        private final TextView v;
        private final String what;
        private final int color;

        TextViewSetter(TextView v, String what, int color) {
            this.v = v;
            this.what = what;
            this.color = color;
        }

        TextViewSetter(TextView v, String what) {
            this(v, what, Color.BLACK);
        }

        @Override
        public void run() {
            v.setText(what);
            v.setTextColor(color);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvServiceMessage = findViewById(R.id.serviceMessage);
        findViewById(R.id.accessibility).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });

        findViewById(R.id.development_settings).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
            }
        });

        Intent intent = getIntent();
        boolean isHide = intent.getBooleanExtra("hide", false);
        if (isHide) {
            Log.i(TAG, "launch args hide:true, move to background");
            moveTaskToBack(true);
        }
        textViewIP = findViewById(R.id.ip_address);
        tvInStorage = findViewById(R.id.in_storage);

        String[] permissions = new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_PHONE_NUMBERS,
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS};
        Permissons4App.initPermissions(this, permissions);


    }
    private void checkNotificationPermission() {
        if (!isNotificationPermissionGranted()) {
            // 通知权限未授予，显示提示
            Toast.makeText(this, "请授予通知权限以显示通知", Toast.LENGTH_SHORT).show();

            // 引导用户到应用设置页面
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        }else{
            Toast.makeText(this, "通知权限已授予", Toast.LENGTH_SHORT).show();
            Intent serviceIntent = new Intent(this, Service.class);
            startService(serviceIntent);
        }
    }
    private boolean isNotificationPermissionGranted() {
        NotificationManager notificationManager = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager = getSystemService(NotificationManager.class);
        }
        if (notificationManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return notificationManager.areNotificationsEnabled();
            }
        }
        return false;
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

    public void testUiautomator(View view) {
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
        okhttpManager.newCall(request, new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new TextViewSetter(tvServiceMessage, e.toString()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (response.body() == null || !response.isSuccessful()) {
                        runOnUiThread(new TextViewSetter(tvServiceMessage, "UIAutomator not responding!"));
                        return;
                    }
                    String responseData = response.body().string();
                    runOnUiThread(new TextViewSetter(tvServiceMessage, responseData));
//                    JSONObject obj = new JSONObject(responseData);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                    runOnUiThread(new TextViewSetter(tvServiceMessage, e.toString()));
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new TextViewSetter(tvServiceMessage, e.toString()));
                }
            }
        });
    }



    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();
//        checkNotificationPermission();

        tvInStorage.setText(Formatter.formatFileSize(this, MemoryManager.getAvailableInternalMemorySize()) + "/" + Formatter.formatFileSize(this, MemoryManager.getTotalExternalMemorySize()));
        checkNetworkAddress(null);
    }

    public void checkNetworkAddress(View v) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int ip = wifiManager.getConnectionInfo().getIpAddress();
        String ipStr = (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
        textViewIP.setText(ipStr);
        textViewIP.setTextColor(Color.BLUE);
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
