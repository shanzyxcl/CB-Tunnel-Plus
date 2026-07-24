package com.cbtunnel.plus.adapter;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Objects;

import com.cbtunnel.plus.config.ConfigDataBase;
import com.cbtunnel.plus.harliesApplication;
import com.cbtunnel.plus.R;
import com.cbtunnel.plus.config.ConfigUtil;
import com.cbtunnel.plus.config.SettingsConstants;
import com.cbtunnel.plus.utils.Model;
import com.cbtunnel.plus.utils.util;
import com.cbtunnel.plus.view.swipe.DragListView;
import com.cbtunnel.plus.view.swipe.ItemAdapter;

public class ConfigSpinnerAdapter extends AppCompatActivity implements SettingsConstants {

	private DragListView ConfigListView;
	private SearchView searchview;
	private ItemAdapter listAdapter;
    private String ConfigType;
	private SharedPreferences mPref;
	private SharedPreferences.Editor mEditor;
	private ConfigUtil mConfig;
	private View show_random_ly;
	private final ArrayList<Model> arrayList = new ArrayList<>();
	private ConfigDataBase serverData,networkData;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_adapters);
		serverData = new ConfigDataBase(ConfigSpinnerAdapter.this, "mServerData");
		networkData = new ConfigDataBase(ConfigSpinnerAdapter.this, "mNetwrokData");
		mConfig = ConfigUtil.getInstance(ConfigSpinnerAdapter.this);
		mPref = harliesApplication.getPrivateSharedPreferences();
		mEditor = mPref.edit();
		getWindow().setStatusBarColor(mConfig.getColorAccent());
		Bundle bundle = getIntent().getExtras();
		if (bundle == null){
			finish();
			return;
		}
		ConfigType = bundle.getString("mConfigType");
		Toolbar mToolbar = findViewById(R.id.toolbar);
		if (Objects.equals(ConfigType, "0")){
			mToolbar.setTitle("Servers");
		}else if (Objects.equals(ConfigType, "1")){
			mToolbar.setTitle("Networks");
		}
		mToolbar.setBackgroundColor(mConfig.getColorAccent());
		mToolbar.setTitleTextColor(Color.WHITE);
		setSupportActionBar(mToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		mToolbar.setNavigationOnClickListener(v -> ConfigSpinnerAdapter.this.finish());
		ConfigListView = findViewById(R.id.config_listview);
        show_random_ly = findViewById(R.id.show_random_l);
		LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(ConfigSpinnerAdapter.this);
		if (Objects.equals(ConfigType, "0")){
			show_random_ly.setVisibility(View.VISIBLE);
		}else if (Objects.equals(ConfigType, "1")){
			show_random_ly.setVisibility(View.GONE);
		}
		ConfigListView.setBackgroundColor(Color.TRANSPARENT);
		ConfigListView.getRecyclerView().setVerticalScrollBarEnabled(false);
		ConfigListView.setLayoutManager(mLinearLayoutManager);
		listAdapter = new ItemAdapter(ConfigSpinnerAdapter.this,getConfigAdapter(ConfigType), R.layout.server_list, R.id.image, Integer.parseInt(ConfigType));
		ConfigListView.setAdapter(listAdapter, true);
		ConfigListView.setCanDragHorizontally(false);
		ConfigListView.setCanDragVertically(true);
		ConfigListView.setDragListListener(new DragListView.DragListListenerAdapter() {
            @Override
			public void onItemDragEnded(int fromPosition, int toPosition) {
				loadNewJS(listAdapter.getNewJS(),toPosition);
			}
		});
		listAdapter.setOnSelectedSerListener(new ItemAdapter.OnSelectedSerListener() {
			@Override
			public void onSelectSer(String charText) {
				getConfigAdapter(ConfigType);
				int p = 0;
				for (Model model : arrayList) {
					String name = model.getName();
					int postition = model.getPostition();
					if (name.equals(charText)){
						p = postition;
					}
				}
				if (Objects.equals(ConfigType, "0")){
					mEditor.putInt(SERVER_POSITION,p).apply();
					mEditor.putBoolean("isRandom", false).apply();
				}else if (Objects.equals(ConfigType, "1")){
					mEditor.putInt(SERVER_POSITION,0).apply();
					mEditor.putInt(NETWORK_POSITION,p).apply();
				}
				finish();
			}
			@Override
			public void onReloadConfig(int position) {
				loadNewJS(listAdapter.getNewJS(),position);
				mLinearLayoutManager.scrollToPosition(position);
			}
		});
		AppCompatImageView mSelectedItem = findViewById(R.id.mSelectedItem);
		if (mPref.getBoolean("isRandom",false)){
			mSelectedItem.setImageResource(R.drawable.ic_item_selected);
			mSelectedItem.setColorFilter(getResources().getColor(R.color.connect_color), PorterDuff.Mode.SRC_IN);
		}else{
			mSelectedItem.setImageResource(R.drawable.ic_item_unselected);
			mSelectedItem.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
		}
		show_random_ly.setOnClickListener(p1 -> {
			mEditor.putBoolean("isRandom", true).apply();
			mSelectedItem.setImageResource(R.drawable.ic_item_selected);
			mSelectedItem.setColorFilter(getResources().getColor(R.color.connect_color), PorterDuff.Mode.SRC_IN);
            finish();
        });
		mLinearLayoutManager.scrollToPosition(Objects.equals(ConfigType, "0")?mPref.getInt(SERVER_POSITION,0):mPref.getInt(NETWORK_POSITION,0));
		if (mPref.getBoolean("show_random_layout",false)&&Objects.equals(ConfigType, "0")){
			show_random_ly.setVisibility(listAdapter.getItemList().size()>=2?View.VISIBLE:View.GONE);
		}else{
			show_random_ly.setVisibility(View.GONE);
		}
	}

	private void loadNewJS(String data,int position){
        try {
			if (Objects.equals(ConfigType, "0")){
				String serType = mSType(getNetworkArrayDragaPosition().getJSONObject(mPref.getInt(NETWORK_POSITION, 0)));
				JSONArray jarr = new JSONArray(data);
				if (serType.equals("OVPN_SSH_KEY")) {
					for (int i=0;i < jarr.length();i++){
						JSONObject js = jarr.getJSONObject(i);
						if (js.getInt("serverType") == 0) {
							mEditor.putString(SERVER_TYPE_OVPN, data).apply();
						}
						if (js.getInt("serverType") == 1) {
							mEditor.putString(SERVER_TYPE_SSH, data).apply();
						}
						if (js.getInt("serverType") == 3) {
							mEditor.putString(SERVER_TYPE_V2RAY, data).apply();
						}
					}
				} else if (serType.equals("DNS_KEY")) {
					for (int i=0;i < jarr.length();i++){
						if (jarr.getJSONObject(i).getInt("serverType") == 2) {
							mEditor.putString(SERVER_TYPE_DNS, data).apply();
						}
						if (jarr.getJSONObject(i).getInt("serverType") == 3) {
							mEditor.putString(SERVER_TYPE_V2RAY, data).apply();
						}
					}
				} else if (serType.equals("UDP_KEY")) {
					for (int i=0;i < jarr.length();i++){
						if (jarr.getJSONObject(i).getInt("serverType") == 4) {
							mEditor.putString(SERVER_TYPE_UDP_HYSTERIA_V1, data).apply();
						}
						if (jarr.getJSONObject(i).getInt("serverType") == 3) {
							mEditor.putString(SERVER_TYPE_V2RAY, data).apply();
						}
					}
				}
				mEditor.putInt(SERVER_POSITION,0).apply();
			}else if (Objects.equals(ConfigType, "1")){
				mEditor.putInt(SERVER_POSITION,0).apply();
				mEditor.putInt(NETWORK_POSITION,position).apply();
				networkData.updateData("1", data);
			}
        } catch (Exception ignored) {
        }
	}

	private void setupListRecyclerView(DragListView mDragListView,ItemAdapter listAdapter) {
		LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(ConfigSpinnerAdapter.this);
		mDragListView.setLayoutManager(mLinearLayoutManager);
		mDragListView.setAdapter(listAdapter, true);
		mDragListView.setCanDragHorizontally(false);
		mDragListView.setCanDragVertically(true);
		mLinearLayoutManager.scrollToPosition(Objects.equals(ConfigType, "0")?mPref.getInt(SERVER_POSITION,0):mPref.getInt(NETWORK_POSITION,0));
	}

	private ArrayList<Pair<Long, String>> getConfigAdapter(String t) {
		ArrayList<Pair<Long, String>> mItemArray = new ArrayList<>();
		arrayList.clear();
		JSONArray jar = null;
		try {
			if (Objects.equals(t, "0")){
				jar = getServerArrayDragaPosition();
			}else if (Objects.equals(t, "1")){
				jar = getNetworkArrayDragaPosition();
			}
			for (int i=0;i < jar.length();i++) {
				String name = jar.getJSONObject(i).getString("Name");
				Model model = new Model(name, i);
				arrayList.add(model);
				mItemArray.add(new Pair<>((long) i, jar.getJSONObject(i).toString()));
			}
			return mItemArray;
		} catch (Exception e) {
			return null;
		}
	}

	private JSONArray getNetworkArrayDragaPosition(){
		try {
			JSONArray jar = new JSONArray();
			JSONArray jar1 = new JSONArray(networkData.getData());
			JSONArray jar2 = new JSONArray(serverData.getData());
			for (int i=0;i < jar1.length();i++) {
				jar.put(jar1.getJSONObject(i));
			}
			for (int i=0;i < jar2.length();i++) {
				if (jar2.getJSONObject(i).getInt("serverType") == 3) {
					jar.put(jar2.getJSONObject(i));
				}
			}
			return jar;
		} catch (JSONException e) {
			util.showToast("Error!", e.toString());
		}
		return null;
	}

	private String mSType(JSONObject js) throws JSONException {
		if (js.has("serverType")){
			return "V2RAY";
		} else if (js.getInt("proto_spin") == 0) {
			return "OVPN_SSH_KEY";
		} else if (js.getInt("proto_spin") == 1) {
			return "UDP_KEY";
		} else if (js.getInt("proto_spin") == 2) {
			return "DNS_KEY";
		} else if (js.getInt("proto_spin") == 3) {
			return "OVPN_SSH_KEY";
		} else if (js.getInt("proto_spin") == 4) {
			return "OVPN_SSH_KEY";
		} else if (js.getInt("proto_spin") == 5) {
			return "OVPN_SSH_KEY";
		}
		return "OVPN_SSH_KEY";
	}

    private JSONArray getServerArrayDragaPosition(){
        try {
            JSONArray jar = new JSONArray();
            String serType = mSType(getNetworkArrayDragaPosition().getJSONObject(mPref.getInt(NETWORK_POSITION, 0)));
            switch (serType) {
                case "V2RAY":
                    JSONArray jarr = new JSONArray(serverData.getData());
                    for (int i = 0; i < jarr.length(); i++) {
                        jar.put(jarr.getJSONObject(i));
                    }
                    break;
                case "OVPN_SSH_KEY":
                    JSONArray jarr1 = new JSONArray(mPref.getString(SERVER_TYPE_OVPN, "[]"));
                    JSONArray jarr2 = new JSONArray(mPref.getString(SERVER_TYPE_SSH, "[]"));
                    //JSONArray jarr5 = new JSONArray(mPref.getString(SERVER_TYPE_V2RAY, "[]"));
                    for (int i = 0; i < jarr1.length(); i++) {
                        JSONObject js = jarr1.getJSONObject(i);
                        if (js.getInt("serverType") == 0) {
                            jar.put(jarr1.getJSONObject(i));
                        }
                    }
                    for (int i = 0; i < jarr2.length(); i++) {
                        JSONObject js = jarr2.getJSONObject(i);
                        if (js.getInt("serverType") == 1) {
                            jar.put(jarr2.getJSONObject(i));
                        }
                    }
                    /*for (int i = 0; i < jarr5.length(); i++) {
                     JSONObject js = jarr5.getJSONObject(i);
                     if (js.getInt("serverType") == 3) {
                     jar.put(jarr5.getJSONObject(i));
                     }
                     }*/
                    break;
                case "DNS_KEY":
                    JSONArray jarr3 = new JSONArray(mPref.getString(SERVER_TYPE_DNS, "[]"));
                    //JSONArray jarr5 = new JSONArray(mPref.getString(SERVER_TYPE_V2RAY, "[]"));
                    for (int i = 0; i < jarr3.length(); i++) {
                        if (jarr3.getJSONObject(i).getInt("serverType") == 2) {
                            jar.put(jarr3.getJSONObject(i));
                        }
                    }
                    /*for (int i = 0; i < jarr5.length(); i++) {
                     if (jarr5.getJSONObject(i).getInt("serverType") == 3) {
                     jar.put(jarr5.getJSONObject(i));
                     }
                     }*/
                    break;
                case "UDP_KEY":
                    JSONArray jarr4 = new JSONArray(mPref.getString(SERVER_TYPE_UDP_HYSTERIA_V1, "[]"));
                    //JSONArray jarr5 = new JSONArray(mPref.getString(SERVER_TYPE_V2RAY, "[]"));
                    for (int i = 0; i < jarr4.length(); i++) {
                        if (jarr4.getJSONObject(i).getInt("serverType") == 4) {
                            jar.put(jarr4.getJSONObject(i));
                        }
                    }
                    /*for (int i = 0; i < jarr5.length(); i++) {
                     if (jarr5.getJSONObject(i).getInt("serverType") == 3) {
                     jar.put(jarr5.getJSONObject(i));
                     }
                     }*/
                    break;

            }
            if (jar.length()>=2){
                mEditor.putBoolean("show_random_layout",true).apply();
            }else{
                mEditor.putBoolean("show_random_layout",false).apply();
            }
            return jar;
        } catch (JSONException e) {
            mEditor.putBoolean("isRandom", false).apply();
            mEditor.putBoolean("show_random_layout",false).apply();
            util.showToast("Error!", e.getMessage());}
        return null;
    }

	/*@Override
	public boolean onCreateOptionsMenu(Menu menu){
		if (mConfig.getServerType().equals(SERVER_TYPE_V2RAY)) {
			getMenuInflater().inflate(R.menu.server_menu,menu);
		} else {
			if (mConfig.getServerType().equals(SERVER_TYPE_OVPN) || mConfig.getServerType().equals(SERVER_TYPE_SSH)){
				if (Objects.equals(ConfigType, "0")){
					getMenuInflater().inflate(R.menu.server_menu,menu);
				}else{
					getMenuInflater().inflate(R.menu.tweak_menu,menu);
				}
			}else {
				getMenuInflater().inflate(R.menu.server_menu,menu);
			}
		}
		searchview = (SearchView) menu.findItem(R.id.search).getActionView();
        assert searchview != null;
        searchview.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				return false;
			}
			@Override
			public boolean onQueryTextChange(String newText) {
				String text = newText;
				if (TextUtils.isEmpty(text)){
					listAdapter.filter("");
				}else {
					listAdapter.filter(text);
				}
				setupListRecyclerView(ConfigListView,listAdapter);
				show_random_ly.setVisibility(mPref.getBoolean("show_random_layout",false)&&Objects.equals(ConfigType, "0")&&listAdapter.getItemList().size()>=2?View.VISIBLE:View.GONE);
				EditText searchEditText = searchview.findViewById(R.id.search);
				searchEditText.setTextColor(Color.RED);
				return true;
			}
		});
		return true;
	}*/


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (mConfig.getServerType().equals(SERVER_TYPE_V2RAY)) {
			getMenuInflater().inflate(R.menu.server_menu, menu);
		} else {
			if (mConfig.getServerType().equals(SERVER_TYPE_OVPN) || mConfig.getServerType().equals(SERVER_TYPE_SSH)) {
				if (Objects.equals(ConfigType, "0")) {
					getMenuInflater().inflate(R.menu.server_menu, menu);
				} else {
					getMenuInflater().inflate(R.menu.tweak_menu, menu);
				}
			} else {
				getMenuInflater().inflate(R.menu.server_menu, menu);
			}
		}

		searchview = (SearchView) menu.findItem(R.id.search).getActionView();
		assert searchview != null;


		EditText searchEditText = searchview.findViewById(androidx.appcompat.R.id.search_src_text);
		if (searchEditText != null) {
			searchEditText.setTextColor(Color.WHITE); // Set initial text color
			searchEditText.setHintTextColor(Color.WHITE); // Set hint color
		}

		searchview.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				return false;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				if (TextUtils.isEmpty(newText)) {
					listAdapter.filter("");
				} else {
					listAdapter.filter(newText);
				}
				setupListRecyclerView(ConfigListView, listAdapter);
				show_random_ly.setVisibility(
						mPref.getBoolean("show_random_layout", false) && Objects.equals(ConfigType, "0") && listAdapter.getItemList().size() >= 2 ? View.VISIBLE : View.GONE
				);


				if (searchEditText != null) {
					if (newText.length() % 2 == 0) {
						searchEditText.setTextColor(Color.WHITE);
					} /*else {
						searchEditText.setTextColor(Color.BLUE);
					}*/
				}
				return true;
			}
		});

		return true;
	}



}
