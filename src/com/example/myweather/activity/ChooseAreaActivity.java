package com.example.myweather.activity;

import java.util.ArrayList;
import java.util.List;

import com.example.myweather.db.MyWeatherDB;
import com.example.myweather.model.City;
import com.example.myweather.model.County;
import com.example.myweather.model.Province;
import com.example.myweather.util.HttpCallbackListener;
import com.example.myweather.util.HttpUtil;
import com.example.myweather.util.Utility;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseAreaActivity extends Activity {
	public static final int LEVEL_PROVINCE=0;
	public static final int LEVEL_CITY=1;
	public static final int LEVEL_COUNTY=2;
	
	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private MyWeatherDB myWeatherDB;
	/*
	* 是否从WeatherActivity中跳转过来
	*/
	private List<String> datalist=new ArrayList<String>();
	/*
	  * 省列表
	  */
	private List<Province> provinceList;
	/*
	* 市列表
	*/
	private List<City> cityList;
	/*
	* 县列表
	*/
	private List<County> countyList;
	/*
	* 选中的省份
	*/
	private Province selectedProvince;
	/*
	* 选中的城市
	*/
	private City selectedCity;
	/*
	* 当前选中的级别
	*/
	private int currentLevel;
	/*
	* 是否从eatherActivity中跳转过来
	*/
	private boolean isFromWeatherActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getBoolean("city_selected",false) && !isFromWeatherActivity){
			Intent intent = new Intent(this,WeatherActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		listView = (ListView) findViewById(R.id.list_view);
		titleText = (TextView) findViewById(R.id.tile_text);
		adapter=new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,datalist);
		listView.setAdapter(adapter);
		myWeatherDB=MyWeatherDB.getInstance(this);
		listView.setOnItemClickListener(new OnItemClickListener() {
 
			public void onItemClick1(AdapterView<?> arg0,View view, int index,
					long arg3) {
				
				if(currentLevel==LEVEL_PROVINCE)
				{
					selectedProvince=provinceList.get(index);
					queryCities();
				}else if(currentLevel==LEVEL_CITY)
				{
					String countyCode = countyList.get(index).getCountyCode();
					Intent intent = new Intent(ChooseAreaActivity.this,WeatherActivity.class);
					intent.putExtra("county_code", countyCode);
					startActivity(intent);
					finish();
					selectedCity=cityList.get(index);
					queryCounties();
				}
			}

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				
			}
			
		});
		queryProvinces();//加载省级数据
	}
	/*
	* 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询
	*/
	private void queryProvinces()
	{
		provinceList=myWeatherDB.loadProvinces();
		if(provinceList.size()>0)
		{
			datalist.clear();
			for(Province province:provinceList)
			{
				datalist.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("中国");
			currentLevel=LEVEL_PROVINCE;
		}else{
			queryFromServer(null,"province");
		}
	}
	/*
	* 查询选中省内所有的市，优先从数据库查询，如果没有查询到再到服务器查询。
	*/
	private void queryCities()
	{
		cityList=myWeatherDB.loadCities(selectedProvince.getId());
		if(cityList.size()>0)
		{
			datalist.clear();
			for(City city:cityList)
			{
				datalist.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel=LEVEL_CITY;
		}else{
			queryFromServer(selectedProvince.getProvinceCode(),"city");
		}
	}
	/*
	* 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询
	*/
	private void queryCounties()
	{
		countyList=myWeatherDB.loadCounties(selectedCity.getId());
		if(countyList.size()>0)
		{
			datalist.clear();
			for(County county:countyList)
			{
				datalist.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel=LEVEL_COUNTY;
		}else{
			queryFromServer(selectedCity.getCityCode(),"county");
		}
		
	}
	/*
	* 根据传入的代号和类型从服务器上查询市县数据
	*/
	private void queryFromServer(final String code,final String type)
	{
		String address;
		if(!TextUtils.isEmpty(code))
		{
			address="http://www.weather.com.cn/data/list3/city"+code+".xml";
		}else{
			address="http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			
			@Override
			public void onFinish(String response) {
				boolean result=false;
				if("province".equals(type))
				{
					result=Utility.handleProvincesResponse(myWeatherDB, response);
				}else if("city".equals(type))
				{
					result=Utility.handleCitiesResponse(myWeatherDB, response, selectedProvince.getId());
				}else if("county".equals(type))
				{
					result=Utility.handleCountiesResponse(myWeatherDB, response, selectedCity.getId());
				}
				if(result)
				{
					//通过runOnUiThread方法回到主线程处理逻辑
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {	
							closeProgressDialog();
							if("province".equals(type))
							{
								queryProvinces();
							}else if("city".equals(type))
							{
								queryCities();
							}else if("county".equals(type))
							{
								queryCounties();
							}
							
						}
					});
				}
			}
			
			@Override
			public void onError(Exception e) {
				
				//通过runOnUiThread()回到主线程处理逻辑
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "加载失败！", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
	}
	/*
	* 显示进度对话框
	*/
	private void showProgressDialog()
	{
		if(progressDialog==null)
		{
			progressDialog=new ProgressDialog(this);
			progressDialog.setMessage("正在加载。。。");
			progressDialog.setCanceledOnTouchOutside(false);
			//用ProgressDialog的地方，最好加下这个属性，防止4.0系统出问题。mProgressDialog.setCanceledOnTouchOutside(false);
			//就是在loading的时候，如果你触摸屏幕其它区域，就会让这个progressDialog消失，然后可能出现崩溃问题
		}
		progressDialog.show();
	}
	/*
	* 关闭进度对话框
	*/
	private void closeProgressDialog()
	{
		if(progressDialog!=null)
		{
			progressDialog.dismiss();
		}
	}
	/*
	* 捕获Back按键，根据当前级别来判断，此时应该返回市列表，省列表，还是直接退出。
	*/
	@Override
	public void onBackPressed()
	{
		if(currentLevel==LEVEL_COUNTY)
		{
			queryCities();
		}else if(currentLevel==LEVEL_CITY)
		{
			queryProvinces();
		}else{
			if(isFromWeatherActivity){
				Intent intent = new Intent(this,WeatherActivity.class);
				startActivity(intent);
			}
			finish();
		}
	}
}

