package com.github.uiautomator;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

public class ToastActivity extends Activity {
    final static String TAG = "ToastActivity";
    private static FloatView floatView;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate");
        Intent intent = getIntent();

        String message = intent.getStringExtra("message");
        if (message != null && !"".equals(message)) {
            Toast.makeText(this, "openatx: " + message, Toast.LENGTH_SHORT).show();
        }

        String showFloat = intent.getStringExtra("showFloatWindow");
        Log.i(TAG, "showFloat: " + showFloat);
        if ("true".equals(showFloat)) {
            getFloatView().show();
        } else if ("false".equals(showFloat)) {
            getFloatView().hide();
        }
        //设置系统时区
        String timezone = getIntent().getStringExtra("timezone");
        if (timezone != null) {
            updateTimezone(timezone);
        }
        //设置系统语言
        String language = getIntent().getStringExtra("language");
        if (language != null) {
            updateLocale(language);
        }

        moveTaskToBack(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private FloatView getFloatView() {
        if (floatView == null) {
            floatView = new FloatView(ToastActivity.this);
        }
        return floatView;
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
    private void updateTimezone(String timezone) {
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.setTimeZone(timezone);
        Log.d(TAG, "timezone updated to: " + timezone);

    }

    private void updateLocale(String language) {
        try {
            Locale locale = new Locale(language);

            Class amnClass = Class.forName("android.app.ActivityManagerNative");
            Object amn = null;
            Configuration config = null;

            // amn = ActivityManagerNative.getDefault();
            Method methodGetDefault = amnClass.getMethod("getDefault");
            methodGetDefault.setAccessible(true);
            amn = methodGetDefault.invoke(amnClass);

            // config = amn.getConfiguration();
            Method methodGetConfiguration = amnClass.getMethod("getConfiguration");
            methodGetConfiguration.setAccessible(true);
            config = (Configuration) methodGetConfiguration.invoke(amn);

            // config.userSetLocale = true;
            Class configClass = config.getClass();
            Field f = configClass.getField("userSetLocale");
            f.setBoolean(config, true);

            // set the locale to the new value
            config.locale = locale;

            // amn.updateConfiguration(config);
            Method methodUpdateConfiguration = amnClass.getMethod("updateConfiguration", Configuration.class);
            methodUpdateConfiguration.setAccessible(true);
            methodUpdateConfiguration.invoke(amn, config);
            Log.d(TAG, "language updated to: " + language);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "language updated failed: " + e.getMessage());
        }
    }
}
