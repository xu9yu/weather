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
	* �Ƿ��WeatherActivity����ת����
	*/
	private List<String> datalist=new ArrayList<String>();
	/*
	  * ʡ�б�
	  */
	private List<Province> provinceList;
	/*
	* ���б�
	*/
	private List<City> cityList;
	/*
	* ���б�
	*/
	private List<County> countyList;
	/*
	* ѡ�е�ʡ��
	*/
	private Province selectedProvince;
	/*
	* ѡ�еĳ���
	*/
	private City selectedCity;
	/*
	* ��ǰѡ�еļ���
	*/
	private int currentLevel;
	/*
	* �Ƿ��eatherActivity����ת����
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
		queryProvinces();//����ʡ������
	}
	/*
	* ��ѯȫ�����е�ʡ�����ȴ����ݿ��ѯ�����û�в�ѯ����ȥ�������ϲ�ѯ
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
			titleText.setText("�й�");
			currentLevel=LEVEL_PROVINCE;
		}else{
			queryFromServer(null,"province");
		}
	}
	/*
	* ��ѯѡ��ʡ�����е��У����ȴ����ݿ��ѯ�����û�в�ѯ���ٵ���������ѯ��
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
	* ��ѯѡ���������е��أ����ȴ����ݿ��ѯ�����û�в�ѯ����ȥ�������ϲ�ѯ
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
	* ���ݴ���Ĵ��ź����ʹӷ������ϲ�ѯ��������
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
					//ͨ��runOnUiThread�����ص����̴߳����߼�
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
				
				//ͨ��runOnUiThread()�ص����̴߳����߼�
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "����ʧ�ܣ�", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
	}
	/*
	* ��ʾ���ȶԻ���
	*/
	private void showProgressDialog()
	{
		if(progressDialog==null)
		{
			progressDialog=new ProgressDialog(this);
			progressDialog.setMessage("���ڼ��ء�����");
			progressDialog.setCanceledOnTouchOutside(false);
			//��ProgressDialog�ĵط�����ü���������ԣ���ֹ4.0ϵͳ�����⡣mProgressDialog.setCanceledOnTouchOutside(false);
			//������loading��ʱ������㴥����Ļ�������򣬾ͻ������progressDialog��ʧ��Ȼ����ܳ��ֱ�������
		}
		progressDialog.show();
	}
	/*
	* �رս��ȶԻ���
	*/
	private void closeProgressDialog()
	{
		if(progressDialog!=null)
		{
			progressDialog.dismiss();
		}
	}
	/*
	* ����Back���������ݵ�ǰ�������жϣ���ʱӦ�÷������б�ʡ�б�����ֱ���˳���
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

