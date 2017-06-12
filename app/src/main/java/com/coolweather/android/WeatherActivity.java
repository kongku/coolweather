package com.coolweather.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.BingPicture;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.service.AutoUpdateService;
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

public class WeatherActivity extends AppCompatActivity {
    private ImageView bingPictureView;
    public DrawerLayout drawerLayout;
    public SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView weatherLayout;
    private Button navigationButton;
    private ImageView weatherIcon;
    private TextView titleCityText;
    private TextView titleUpdateTimeText;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private LinearLayout aqiLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    public String weatherId;
    public final static int LONG_TIME=0;
    public final static int SHORT_TIME=1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //全屏功能，5.0以上生效
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        //初始化各控件
        bingPictureView = (ImageView) findViewById(R.id.bing_pic_view);
        drawerLayout= (DrawerLayout) findViewById(R.id.drawer_layout);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        navigationButton= (Button) findViewById(R.id.nav_button);
        weatherIcon= (ImageView) findViewById(R.id.weather_icon);
        titleCityText = (TextView) findViewById(R.id.title_city);
        titleUpdateTimeText = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiLayout= (LinearLayout) findViewById(R.id.aqi_layout);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

        Weather weather = null;
        //优先从缓存中读取数据
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = preferences.getString("weather", null);
        if (weatherString != null) {
            weather = Utility.handleWeatherResponse(weatherString);
            if (weather != null) {
                weatherId =weather.basic.weatherId;
                //显示天气信息
                showWeatherInfo(weather);
                //有天气信息了，才去加载和显示每日一图
                String bingPictureString = preferences.getString("bingpicture", null);
                if (bingPictureString != null) {
                    Glide.with(this).load(bingPictureString).into(bingPictureView);
                } else {
                    requestBingPicture();
                }
            }else{
                //weather是null，就是说缓存解析不成功，就需要去网络请求天气信息
                weatherId = getIntent().getStringExtra("weather_id");
                weatherLayout.setVisibility(ScrollView.INVISIBLE);
                requestWeather(weatherId);
            }
        }else {
            //没有缓存，就需要去网络请求天气信息
            weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(ScrollView.INVISIBLE);
            requestWeather(weatherId);
        }

        //标题栏的按钮注册监听器
        navigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        //下拉刷新功能
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(weatherId);
                requestBingPicture();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences preference=PreferenceManager.getDefaultSharedPreferences(this);
        String date=preference.getString("weathertime",null);
        if (date!=null&&paresDateString(date,LONG_TIME)>=1){
            swipeRefreshLayout.setRefreshing(true);
            requestWeather(weatherId);
        }


    }

    public void requestWeather(String weatherId) {
        Resources resources = getResources();
        String weatherUrl = resources.getString(R.string.weather_host_city) + weatherId + "&key=" + resources.getString(R.string.weather_key);
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("weather", responseText);
                            editor.putString("weatherid", weather.basic.weatherId);
                            editor.putString("weathertime",weather.basic.update.updateTime);
                            editor.apply();
                            showWeatherInfo(weather);
                            requestBingPicture();
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
    }

    private void requestBingPicture() {
        String bingPictureJsonUrl = getResources().getString(R.string.bing_picture_json_url);
        HttpUtil.sendOkHttpRequest(bingPictureJsonUrl, new Callback() {
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
                            Glide.with(WeatherActivity.this).load(url).into(bingPictureView);
                        }
                    }
                });
            }
        });
    }

    private void showWeatherInfo(Weather weather) {

        titleCityText.setText(weather.basic.cityName);
        titleUpdateTimeText.setText(weather.basic.update.updateTime.split(" ")[1]);
        Glide.with(this).load("https://cdn.heweather.com/cond_icon/"+weather.now.more.code+".png").into(weatherIcon);
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
            aqiLayout.setVisibility(View.VISIBLE);
        }else {
            aqiLayout.setVisibility(View.GONE);
        }
        comfortText.setText("舒适度：" + weather.suggestion.comfort.info);
        sportText.setText("运动建议：" + weather.suggestion.sport.info);
        carWashText.setText("洗车建议：" + weather.suggestion.carWash.info);
        weatherLayout.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }

    //返回大于等于1的，都是属于过时数据，需要更新
    public static long paresDateString(String dateString,int type) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        if (type==LONG_TIME){
            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        }

        try {
            Date date = dateFormat.parse(dateString);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            if (type==LONG_TIME){
                return (System.currentTimeMillis() - cal.getTimeInMillis()) / (10 * 60 * 1000);
            }
            return (System.currentTimeMillis() - cal.getTimeInMillis()) / (24 * 3600 * 1000);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 1;
    }

}
