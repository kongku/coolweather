package com.coolweather.android;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.BingPicture;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    private ImageView bingPictureView;
    private ScrollView weatherLayout;
    private TextView titleCityText;
    private TextView titleUpdateTimeText;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private Weather weather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT>=21){
            View decorView=getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        //初始化各控件
        bingPictureView = (ImageView) findViewById(R.id.bing_pic_view);
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCityText = (TextView) findViewById(R.id.title_city);
        titleUpdateTimeText = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);

        //优先从缓存中读取数据
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = preferences.getString("weather", null);
        if (weatherString != null) {
            weather = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
        } else {
            String weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(ScrollView.INVISIBLE);
            requestWeather(weatherId);
        }

        String bingPictureString = preferences.getString("bingpicture", null);
        if (bingPictureString != null) {
            weather.bingPictureUrl=bingPictureString;
        } else {
            requestBingPicture();
        }
        showBingPicture();

    }

    private void requestWeather(String weatherId) {
        String weatherUrl = "https://free-api.heweather.com/v5/weather?city=" + weatherId + "&key=17c87f5214084cb1ad324562a1da7294";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String responseText = response.body().string();
                weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }

                    }
                });
            }
        });
    }

    private void requestBingPicture() {
        String bingPictureUrl = "http://cn.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1";
        HttpUtil.sendOkHttpRequest(bingPictureUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取每日一图失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final BingPicture bingPicture = Utility.handleBingPictureResponse(response.body().string());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (bingPicture != null) {
                            String url = "http://cn.bing.com" + bingPicture.url;
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("bingpicture", url);
                            editor.apply();
                            weather.bingPictureUrl = url;
                        }
                    }
                });
            }
        });
    }

    private void showWeatherInfo(Weather weather) {

        titleCityText.setText(weather.basic.cityName);
        titleUpdateTimeText.setText(weather.basic.update.updateTime.split(" ")[1]);
        degreeText.setText(weather.now.temperature + "℃");
        weatherInfoText.setText(weather.now.more.info);
        forecastLayout.removeAllViews();
        TextView datetext;
        TextView infotext;
        TextView maxtext;
        TextView mintext;
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            datetext = (TextView) view.findViewById(R.id.date_text);
            infotext = (TextView) view.findViewById(R.id.info_text);
            maxtext = (TextView) view.findViewById(R.id.max_text);
            mintext = (TextView) view.findViewById(R.id.min_text);
            datetext.setText(forecast.date);
            infotext.setText(forecast.more.info);
            maxtext.setText(forecast.temperature.max);
            mintext.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        comfortText.setText("舒适度：" + weather.suggestion.comfort.info);
        sportText.setText("运动建议：" + weather.suggestion.sport.info);
        carWashText.setText("洗车建议：" + weather.suggestion.carWash.info);
        weatherLayout.setVisibility(View.VISIBLE);
    }

    private void showBingPicture() {

        if (weather.bingPictureUrl!=null){
            Glide.with(this).load(weather.bingPictureUrl).into(bingPictureView);
        }else {
            Glide.with(this).load(R.drawable.bingpicture).into(bingPictureView);
        }
    }
}
