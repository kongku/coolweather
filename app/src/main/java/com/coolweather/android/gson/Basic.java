package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by quan on 2017-06-09 0009.
 */

public class Basic {

    //json中的名字不太适合直接作为java的字段来命名，就使用@SerializedName来建立映射
    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    //直接用json中的名字来作为java字段，就不需要@SerializedName来映射
    public Update update;

    public class Update {
        @SerializedName("loc")
        public String updateTime;
    }
}
