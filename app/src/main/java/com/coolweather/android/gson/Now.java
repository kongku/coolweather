package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by quan on 2017-06-09 0009.
 */

public class Now {
    @SerializedName("tmp")
    public String temperature;

    @SerializedName("cond")
    public More more;
    public class More{
        public String code;
        @SerializedName("txt")
        public String info;
    }
}
