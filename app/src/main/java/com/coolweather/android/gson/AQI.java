package com.coolweather.android.gson;


/**
 * Created by quan on 2017-06-09 0009.
 */

public class AQI {
    public AQICity city;

    public class AQICity {
        public String aqi;
        public String pm25;
    }
}
