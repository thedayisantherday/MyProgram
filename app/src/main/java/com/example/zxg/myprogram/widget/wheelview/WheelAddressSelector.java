package com.example.zxg.myprogram.widget.wheelview;

import android.content.Context;
import android.content.res.AssetManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.example.zxg.myprogram.R;
import com.example.zxg.myprogram.widget.wheelview.adapters.ArrayWheelAdapter;
import com.example.zxg.myprogram.widget.wheelview.helper.JsonParserHandle;
import com.example.zxg.myprogram.widget.wheelview.helper.XmlParserHandler;
import com.example.zxg.myprogram.widget.wheelview.listener.OnWheelChangedListener;
import com.example.zxg.myprogram.widget.wheelview.model.CityModel;
import com.example.zxg.myprogram.widget.wheelview.model.DistrictModel;
import com.example.zxg.myprogram.widget.wheelview.model.ProvinceModel;
import com.example.zxg.myprogram.widget.wheelview.view.WheelView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * 省市区选择，三级联动控件
 * @author zxg
 *
 */
public class WheelAddressSelector implements OnClickListener, OnWheelChangedListener {

	private Context mContext;
	private View mView;

	//所有省
	protected String[] mProvinceDatas = new String[] { "" };
	//key - 省 value - 市
	protected Map<String, String[]> mCitisDatasMap = new HashMap<String, String[]>();
	//key - 市 values - 区
	protected Map<String, String[]> mDistrictDatasMap = new HashMap<String, String[]>();

	//key - 区 values - 邮编
	protected Map<String, String> mZipcodeDatasMap = new HashMap<String, String>();

	// 当前省的名称
	public String mCurrentProviceName ="";
	// 当前市的名称
	public String mCurrentCityName ="";
	//当前区的名称
	public String mCurrentDistrictName ="";
	//当前区的邮政编码
	public String mCurrentZipCode ="";

	private WheelView mViewProvince;
	private WheelView mViewCity;
	private WheelView mViewDistrict;
	public Button btn_cancel;
	public Button btn_confirm;

	public WheelAddressSelector(Context context, View view) {
		mContext = context;
		mView = view;
		setUpViews();
		setUpListener();
		setUpData();
	}

	private void setUpViews() {
		mViewProvince = (WheelView) mView.findViewById(R.id.wv_province);
		mViewCity = (WheelView) mView.findViewById(R.id.wv_city);
		mViewDistrict = (WheelView) mView.findViewById(R.id.wv_district);
		btn_cancel = (Button) mView.findViewById(R.id.btn_cancel);
		btn_confirm = (Button) mView.findViewById(R.id.btn_confirm);
	}

	private void setUpListener() {
		// 添加change事件
		mViewProvince.addChangingListener(this);
		// 添加change事件
		mViewCity.addChangingListener(this);
		// 添加change事件
		mViewDistrict.addChangingListener(this);
		// 添加onclick事件
		btn_cancel.setOnClickListener(this);
		btn_confirm.setOnClickListener(this);
	}

	private void setUpData() {
		//解析省市区的数据
		initProvinceDatas();
		mViewProvince.setViewAdapter(new ArrayWheelAdapter<String>(mContext, mProvinceDatas));
		// 设置可见条目数量
		mViewProvince.setVisibleItems(7);
		mViewCity.setVisibleItems(7);
		mViewDistrict.setVisibleItems(7);
		updateCities();
//		updateAreas();
	}

	@Override
	public void onChanged(WheelView wheel, int oldValue, int newValue) {
		//try...catch是为了防止滑动过快，造成mCitisDatasMap、mDistrictDatasMap等为空而产生异常。
		try{
			if (wheel == mViewProvince) {
				updateCities();
			} else if (wheel == mViewCity) {
				updateAreas();
			} else if (wheel == mViewDistrict) {
				mCurrentDistrictName = mDistrictDatasMap.get(mCurrentCityName)[newValue];
				mCurrentZipCode = mZipcodeDatasMap.get(mCurrentDistrictName);
			}
		}catch(Exception exception){
			exception.printStackTrace();
		}
	}

	/**
	 * 根据当前的市，更新区WheelView的信息
	 */
	private void updateAreas() {
		int pCurrent = mViewCity.getCurrentItem();
		if(mCitisDatasMap != null && !mCitisDatasMap.isEmpty())
			mCurrentCityName = mCitisDatasMap.get(mCurrentProviceName)[pCurrent];
		String[] areas = new String[] { "" };

		if (mDistrictDatasMap != null && !mDistrictDatasMap.isEmpty()) {
			areas = mDistrictDatasMap.get(mCurrentCityName);
			//默认为当前城市的第一个区县和邮编
			mCurrentDistrictName = mDistrictDatasMap.get(mCurrentCityName)[0];
			mCurrentZipCode = mZipcodeDatasMap.get(mCurrentDistrictName);
		}
		mViewDistrict.setViewAdapter(new ArrayWheelAdapter<String>(mContext, areas));
		mViewDistrict.setCurrentItem(0);
	}

	/**
	 * 根据当前的省，更新市WheelView的信息
	 */
	private void updateCities() {
		int pCurrent = mViewProvince.getCurrentItem();
		if (mProvinceDatas != null && mProvinceDatas.length > 0)
			mCurrentProviceName = mProvinceDatas[pCurrent];
		String[] cities = new String[] { "" };
		if (mCitisDatasMap != null && !mCitisDatasMap.isEmpty())
			cities = mCitisDatasMap.get(mCurrentProviceName);

		mViewCity.setViewAdapter(new ArrayWheelAdapter<String>(mContext, cities));
		mViewCity.setCurrentItem(0);
		updateAreas();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_confirm:
				showSelectedResult();
				break;
			default:
				break;
		}
	}

	private void showSelectedResult() {
		Toast.makeText(mContext, "当前选中:"+mCurrentProviceName+","+mCurrentCityName+","
				+mCurrentDistrictName+","+mCurrentZipCode, Toast.LENGTH_SHORT).show();
	}

	/**
	 * 解析省市区的XML数据
	 */
	protected void initProvinceDatas(){
		List<ProvinceModel> provinceList = null;
		AssetManager asset = mContext.getAssets();
		try {

			InputStream input = asset.open("province_data.xml");
			//pull解析省市区的xml数据
//			provinceList = pullParser(input);
			//sax解析省市区的xml数据
//	        provinceList = saxParser(input);
			//dom解析省市区的xml数据
	        provinceList = domParser(input);
			//解析省市区的JSON数据
//			provinceList = new JsonParserHandle(mContext).getProvinceList();
			input.close();

			//*/ 初始化默认选中的省、市、区
			if (provinceList!= null && !provinceList.isEmpty()) {
				mCurrentProviceName = provinceList.get(0).getName();
				List<CityModel> cityList = provinceList.get(0).getCityList();
				if (cityList!= null && !cityList.isEmpty()) {
					mCurrentCityName = cityList.get(0).getName();
					List<DistrictModel> districtList = cityList.get(0).getDistrictList();
					mCurrentDistrictName = districtList.get(0).getName();
					mCurrentZipCode = districtList.get(0).getZipcode();
				}
			}
			if(provinceList.size() > 0)
				mProvinceDatas = new String[provinceList.size()];
			for (int i=0; i< provinceList.size(); i++) {
				// 遍历所有省的数据
				mProvinceDatas[i] = provinceList.get(i).getName();
				List<CityModel> cityList = provinceList.get(i).getCityList();
				String[] cityNames = new String[cityList.size()];
				for (int j=0; j< cityList.size(); j++) {
					// 遍历省下面的所有市的数据
					cityNames[j] = cityList.get(j).getName();
					List<DistrictModel> districtList = cityList.get(j).getDistrictList();
					String[] distrinctNameArray = new String[districtList.size()];
					DistrictModel[] distrinctArray = new DistrictModel[districtList.size()];
					for (int k=0; k<districtList.size(); k++) {
						// 遍历市下面所有区/县的数据
						DistrictModel districtModel = new DistrictModel(districtList.get(k).getName(), districtList.get(k).getZipcode());
						// 区/县对于的邮编，保存到mZipcodeDatasMap
						mZipcodeDatasMap.put(districtList.get(k).getName(), districtList.get(k).getZipcode());
						distrinctArray[k] = districtModel;
						distrinctNameArray[k] = districtModel.getName();
					}
					// 市-区/县的数据，保存到mDistrictDatasMap
					mDistrictDatasMap.put(cityNames[j], distrinctNameArray);
				}
				// 省-市的数据，保存到mCitisDatasMap
				mCitisDatasMap.put(provinceList.get(i).getName(), cityNames);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * pull解析省市区的xml数据
	 */
	private List<ProvinceModel> pullParser(InputStream input){
		List<ProvinceModel> provinceList = new ArrayList<ProvinceModel>();
		ProvinceModel provinceModel = new ProvinceModel();
		CityModel cityModel = new CityModel();
		DistrictModel district = new DistrictModel();
		try{
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser xmlPullParser = factory.newPullParser();
			xmlPullParser.setInput(input, "UTF-8");
			int eventType = xmlPullParser.getEventType();
			while(eventType != XmlPullParser.END_DOCUMENT){
				String str_nodeName = xmlPullParser.getName();
				switch(eventType){
					case XmlPullParser.START_TAG:
						if("province".equals(str_nodeName)){
							provinceModel = new ProvinceModel();
							provinceModel.setName(xmlPullParser.getAttributeValue(null, "name"));
							provinceModel.setCityList(new ArrayList<CityModel>());
						}else if("city".equals(str_nodeName)){
							cityModel = new CityModel();
							cityModel.setName(xmlPullParser.getAttributeValue(null, "name"));
							cityModel.setDistrictList(new ArrayList<DistrictModel>());
						}else if("district".equals(str_nodeName)){
							district = new DistrictModel();
							district.setName(xmlPullParser.getAttributeValue(null, "name"));
							district.setZipcode(xmlPullParser.getAttributeValue(null, "zipcode"));
							cityModel.getDistrictList().add(district);
						}
						break;
					case XmlPullParser.END_TAG:
						if("city".equals(str_nodeName)){
							provinceModel.getCityList().add(cityModel);
						}else if("province".equals(str_nodeName)){
							provinceList.add(provinceModel);
						}
						break;
					default:
						break;
				}
				eventType = xmlPullParser.next();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return provinceList;
	}

	/**
	 * sax解析省市区的xml数据
	 */
	private List<ProvinceModel> saxParser(InputStream input){
		List<ProvinceModel> provinceList = new ArrayList<ProvinceModel>();
		try{
			// 创建一个解析xml的工厂对象
			SAXParserFactory spf = SAXParserFactory.newInstance();
			// 解析xml
			SAXParser parser = spf.newSAXParser();
			XmlParserHandler handler = new XmlParserHandler();
			parser.parse(input, handler);
			input.close();
			// 获取解析出来的数据
			provinceList = handler.getDataList();
		}catch(Exception e){
			e.printStackTrace();
		}
		return provinceList;
	}

	/**
	 * DOM解析省市区的xml数据
	 */
	private List<ProvinceModel> domParser(InputStream input){
		List<ProvinceModel> provinceList = new ArrayList<ProvinceModel>();
		try {
			//1.建立DocumentBuilderFactor，用于获得DocumentBuilder对象：
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			//2.建立DocumentBuidler：
			DocumentBuilder builder = factory.newDocumentBuilder();
			//3.建立Document对象，获取树的入口：
			Document doc = builder.parse(input);

			NodeList nodeProvinceList = doc.getElementsByTagName("province");
			for (int i = 0; nodeProvinceList != null && i < nodeProvinceList.getLength(); i++) {
				Element provinceNode = (Element)nodeProvinceList.item(i);
				ProvinceModel provinceModel = new ProvinceModel();
				provinceModel.setName(provinceNode.getAttribute("name"));
				provinceModel.setCityList(new ArrayList<CityModel>());

				NodeList nodeCityList = provinceNode.getElementsByTagName("city");
				for (int j = 0; nodeCityList != null && j < nodeCityList.getLength(); j++) {
					Element cityNode = (Element)nodeCityList.item(j);
					CityModel cityModel = new CityModel();
					cityModel.setName(cityNode.getAttribute("name"));
					cityModel.setDistrictList(new ArrayList<DistrictModel>());

					NodeList nodeDistrictList = cityNode.getElementsByTagName("district");
					for (int k = 0; nodeDistrictList != null && k < nodeDistrictList.getLength(); k++) {
						Element districtNode = (Element) nodeDistrictList.item(k);
						DistrictModel district = new DistrictModel();
						district.setName(districtNode.getAttribute("name"));
						district.setZipcode(districtNode.getAttribute("zipcode"));
						cityModel.getDistrictList().add(district);
					}
					provinceModel.getCityList().add(cityModel);
				}

				provinceList.add(provinceModel);
			}

		}catch (Exception e){
			e.printStackTrace();
		}

		return provinceList;
	}
}
