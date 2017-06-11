package com.coolweather.android;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by quan on 2017-06-09 0009.
 */

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTY=2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    /*listview绑定的数据，只有省市县区名，没有其他数据*/
    private List<String> dataList= new ArrayList<>();
    /*从数据库加载的省市县区的数据，里面包含所有数据，然后抽取其中的名字放到datalist中供listview用*/
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;
    /*存储当前选择的省市县区*/
    private Province selectProvince;
    private City selectCity;
    private County selectCounty;
    /*记录当前选择的是省市县区中的哪一级*/
    private int currentLevel;

    /*
    * 为碎片创建视图（加载布局）时调用
    *用填充器生成地区选择的view，
    *并查找出各个控件，并记录到相应字段
    * 为listview绑定数据适配器
    * */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.choose_area,container);
        titleText= (TextView) view.findViewById(R.id.title_text);
        backButton= (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        /*实例适配器，使用系统自带的简单布局，只有一个textview
        * 绑定listview的适配器*/
        adapter=new ArrayAdapter<>(getActivity(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;
    }

    /*
    * 确保与碎片相关联的活动一定已经创建完毕的时候调用
    * 绑定监听器，并调用获取数据的方法，把省的数据展示出来
    * */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //listview的选项绑定点击事件，根据选项来更新listview的数据
        //并记录当前选着的项
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel==LEVEL_PROVINCE){
                    selectProvince=provinceList.get(position);
                    queryCites();
                }else if(currentLevel==LEVEL_CITY){
                    selectCity=cityList.get(position);
                    queryCounties();
                }else if (currentLevel==LEVEL_COUNTY){
                    String weatherId=countyList.get(position).getWeatherId();
                    if (getActivity() instanceof  MainActivity){
                        Intent intent=new Intent(getActivity(),WeatherActivity.class);
                        intent.putExtra("weather_id",weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if (getActivity() instanceof WeatherActivity){
                        WeatherActivity activity= (WeatherActivity) getActivity();
                        activity.weatherId=weatherId;
                        activity.drawerLayout.closeDrawer(GravityCompat.START);
                        activity.swipeRefreshLayout.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }

                }
            }
        });
        //对顶部导航中的返回按钮绑定监听器，根据当前选择的省市县区来决定返回的操作
        //这个返回，其实就是刷新listview内容
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel==LEVEL_COUNTY){
                    queryCites();
                }else if (currentLevel==LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    /*
    * 查找省份的数据
    * 默认从数据库中查找，如果数据库没有数据，就从网络上的api获取，并存到数据库中去
    * 设置当前的为省的级别
    * */
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList= DataSupport.findAll(Province.class);
        if (provinceList.size()>0){
            dataList.clear();
            for (Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else {
            String address="http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }

    /*
   * 查找某个省份的市数据
   * 默认从数据库中查找所选择省的市数据，如果数据库没有数据，就从网络上的api获取，并存到数据库中去
   * 设置当前的为市的级别
   * */
    private void queryCites() {
        titleText.setText(selectProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList=DataSupport.where("provinceid=?",String.valueOf(selectProvince.getId())).find(City.class);
        if (cityList.size()>0){
            dataList.clear();
            for (City city:cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_CITY;
        }else {
            String address="http://guolin.tech/api/china/"+selectProvince.getProvinceCode();
            queryFromServer(address,"city");
        }
    }

    /*
   * 查找某个市的县区数据
   * 默认从数据库中查找所选择市的县区数据，如果数据库没有数据，就从网络上的api获取，并存到数据库中去
   * 设置当前的为县区的级别
   * */
    private void queryCounties() {
        titleText.setText(selectCity.getCityName());
        countyList=DataSupport.where("cityid=?",String.valueOf(selectCity.getId())).find(County.class);
        if (countyList.size()>0){
            dataList.clear();
            for (County county:countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_COUNTY;
        }else {
            String address="http://guolin.tech/api/china/"+selectProvince.getProvinceCode()+"/"+selectCity.getCityCode();
            queryFromServer(address,"county");
        }
    }

    /*
    * 从服务器获取省市县区数据
    * 提供api地址，指定查找类型，province，city，county
    * 使用自己封装好（okhttp）的网络请求方法，创建匿名类作为回调来处理返回的数据和处理网络请求失败的情况
    * 返回的数据是json格式的，使用自己封装好（JSONObject，因为数据简单）的方法来解析并储存到数据库中去
    * 由于网络请求是在子线程中执行，返回的数据需要更新到ui上，因此把相关操作切回主线程中执行
    * 在主线程中重新调用相应查找省市县区数据的方法，刷新listview
    * */
    private void queryFromServer(String address, final String type) {
        //显示等待处理的对话框
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //回到主线程来操作UI，关闭等待处理对话刚，并弹出提示
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getActivity(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText=response.body().string();
                boolean result=false;
                //根据指定的类型来解析数据
                if ("province".equals(type)){
                    result= Utility.handleProvinceResponse(responseText);
                }else if ("city".equals(type)){
                    result=Utility.handleCityResponse(responseText,selectProvince.getId());
                }else if ("county".equals(type)){
                    result=Utility.handleCountyResponse(responseText,selectCity.getId());
                }
                //数据解析成功后，返回到主线程来更新listview的数据，并关闭等待处理对话框
                if (result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if ("province".equals(type)){
                                queryProvinces();
                            }else if ("city".equals(type)){
                                queryCites();
                            }else if ("county".equals(type)){
                                queryCounties();
                            }
                            closeProgressDialog();
                        }
                    });
                }
            }
        });
    }

    private void showProgressDialog() {
        if (progressDialog==null){
            progressDialog=new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog() {
        if (progressDialog!=null){
            //调用销毁，而不是隐藏，不然Activity退出时会报错
            progressDialog.dismiss();
        }
    }
}
