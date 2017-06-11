package com.coolweather.android.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;

import com.coolweather.android.R;
import com.coolweather.android.gson.BingPicture;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    public AutoUpdateService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherTime = preferences.getString("weathertime", null);
        if (weatherTime != null) {
            if (paresDateString(weatherTime) >= 1) {
                updateBingPicture();
            }
        }

        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour = 20 * 60 * 1000;//20分钟，测试后台服务是否成功更新数据
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
        Intent i = new Intent(this, AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        return super.onStartCommand(intent, flags, startId);
    }

    private void updateBingPicture() {
        String jsonUrl = getResources().getString(R.string.bing_picture_json_url);
        HttpUtil.sendOkHttpRequest(jsonUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                BingPicture bingPicture = Utility.handleBingPictureResponse(responseText);
                if (bingPicture != null) {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                    editor.putString("bingpicture", getResources().getString(R.string.bing_picture_host) + bingPicture.url);
                    editor.apply();
                }
            }
        });

    }

    private void updateWeather() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherId = preferences.getString("weatherid", null);
        if (weatherId != null) {
            Resources resources = getResources();
            String weatherUrl = resources.getString(R.string.weather_host_city) + weatherId + "&key=" + resources.getString(R.string.weather_key);
            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    Weather weather = Utility.handleWeatherResponse(responseText);
                    if (weather != null && "ok".equals(weather.status)) {
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString("weather", responseText);
                        editor.apply();
                    }
                }
            });
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    //返回大于等于1的，都是属于过时数据，需要更新
    private long paresDateString(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        try {
            Date date = dateFormat.parse(dateString);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return (System.currentTimeMillis() - cal.getTimeInMillis()) / (24 * 3600 * 1000);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 1;
    }
}
