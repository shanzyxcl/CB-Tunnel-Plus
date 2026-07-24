package com.v2ray.ang.util;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import android.view.LayoutInflater;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.v2ray.ang.AppConfig;

import com.cbtunnel.plus.R;
import com.cbtunnel.plus.activities.OpenVPNClientBase;
import com.cbtunnel.plus.harliesApplication;
import com.cbtunnel.plus.utils.util;


public class harliesAppManager extends OpenVPNClientBase implements OnCheckedChangeListener,
OnClickListener {
    
    private static class ListEntry {
        private CheckBox box;
        private TextView text;
        private ImageView icon;
        private TableLayout mTableLay;
    }
    private BottomSheetBehavior progressSheetBehavior;
    private ArrayList<TorifiedApp> apps;
    private ListView listApps;
    private harliesAppManager mAppManager;
    private TextView overlay;
    private ListAdapter adapter;
    private static final int MSG_LOAD_START = 1;
    private static final int MSG_LOAD_FINISH = 2;
    private boolean appsLoaded = false;

    @SuppressLint({"HandlerLeak"})
    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_LOAD_START:
                    showProgrss();
                break;
            case MSG_LOAD_FINISH:

                listApps.setAdapter(adapter);

                listApps.setOnScrollListener(new OnScrollListener() {

                    boolean visible;

                    @Override
                    public void onScroll(AbsListView view,
                            int firstVisibleItem, int visibleItemCount,
                            int totalItemCount) {
                        if (visible) {
                            String name = apps.get(firstVisibleItem).getName();
                            if (name != null && name.length() > 1)
                                overlay.setText(apps.get(firstVisibleItem)
                                        .getName().substring(0, 1));
                            else
                                overlay.setText("*");
                            overlay.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onScrollStateChanged(AbsListView view,
                            int scrollState) {
                        visible = true;
                        if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                            overlay.setVisibility(View.INVISIBLE);
                        }
                    }
                });
                   hideProgrss();
                break;
            }
            super.handleMessage(msg);
        }
    };

    
    private void loadApps() {
        SharedPreferences prefs = harliesApplication.getDefaultSharedPreferences();
        apps = TorifiedApp.getApps(harliesAppManager.this, prefs);
        final LayoutInflater inflater = LayoutInflater.from(this);
        adapter = new ArrayAdapter<TorifiedApp>(this, R.layout.layout_apps_item, R.id.itemtext, apps) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                ListEntry entry;
                if (convertView == null) {
                    // Inflate a new view
                    convertView = inflater.inflate(R.layout.layout_apps_item, parent, false);
                    entry = new ListEntry();
                    entry.mTableLay = convertView.findViewById(R.id.mTableLayout);
                    entry.icon = convertView.findViewById(R.id.itemicon);
                    entry.box = convertView.findViewById(R.id.itemcheck);
                    entry.text = convertView.findViewById(R.id.itemtext);
                    entry.text.setTextColor(Objects.requireNonNull(getConfig()).gettextColor());
                    entry.text.setOnClickListener(mAppManager);
                    convertView.setTag(entry);
                    entry.box.setOnCheckedChangeListener(mAppManager);
                } else {
                    entry = (ListEntry) convertView.getTag();
                }
                final TorifiedApp app = apps.get(position);
                entry.icon.setTag(app.getUid());
                entry.icon.setImageDrawable(DisplayImage(app.getUid()));
                entry.text.setText(app.getName());
                final CheckBox box = entry.box;
                box.setTag(app);
                box.setChecked(app.isTorified());
                entry.text.setTag(box);
                entry.mTableLay.setVisibility(app.getName().length()==0.||app.getName().isEmpty()? View.GONE : View.VISIBLE);
                return convertView;
            }
        };
        appsLoaded = true;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public  Drawable DisplayImage(int uid) {
        PackageManager pm = getPackageManager();
        Drawable appIcon = getResources().getDrawable(R.drawable.ic_android);
        String[] packages = pm.getPackagesForUid(uid);
        if (packages != null) {
            if (packages.length == 1) {
                try {
                    ApplicationInfo appInfo = pm.getApplicationInfo(packages[0], 0);
                    appIcon = pm.getApplicationIcon(appInfo);
                } catch (Exception e) {
                    //ignored
                }
            }
        }
        return appIcon;
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        final TorifiedApp app = (TorifiedApp) buttonView.getTag();
        if (app != null) {
            app.setTorified(isChecked);
        }
        saveAppSettings();
    }

    @Override
    public void onClick(View v) {
        CheckBox cbox = (CheckBox) v.getTag();
        final TorifiedApp app = (TorifiedApp) cbox.getTag();
        if (app != null) {
            app.setTorified(!app.isTorified());
            cbox.setChecked(app.isTorified());
        }
        saveAppSettings();
    }


    private TextView BYPASS_MODETextView;
    private TextView APPSTextView;
    private Button BsetEnabled;
    private ImageView mPoint;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new util(harliesAppManager.this);
        setContentView(R.layout.layout_apps);
        SharedPreferences prefs = harliesApplication.getDefaultSharedPreferences();
        getWindow().setStatusBarColor(Objects.requireNonNull(getConfig()).getColorAccent());
        Toolbar mToolbar = findViewById(R.id.toolbar_main);
        mToolbar.setTitle(getResources().getString(R.string.app_name));    
        mToolbar.setSubtitle("Filter Apps");          
        mToolbar.setBackgroundColor(getConfig().getColorAccent());
        mToolbar.setNavigationIcon(getConfig().getAppThemeUtil()? R.drawable.arrow_d:R.drawable.arrow_l);
        setSupportActionBar(mToolbar);
        BsetEnabled = findViewById(R.id.fragmentallowedappButton1);
        APPSTextView = findViewById(R.id.fragmentallowedappTextView1);
        BYPASS_MODETextView = findViewById(R.id.fragmentallowedappTextView2);
        SwitchCompat APPS = findViewById(R.id.fragmentallowedappSwitch1);
        SwitchCompat BYPASS_MODE = findViewById(R.id.fragmentallowedappSwitch2);
        overlay = findViewById( R.id.layoutappsOverLay);
        View progbottomSheet = findViewById(R.id.progress_bottom_sheet);
        mPoint = findViewById(R.id.progPoint);
        progressSheetBehavior = BottomSheetBehavior.from(progbottomSheet);
        progressSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        findViewById(R.id.progress_bg).setBackgroundColor(getConfig().getProgLayoutBG());
        mPoint.setColorFilter(getConfig().getColorAccent(), PorterDuff.Mode.SRC_IN);
        overlay.setBackgroundColor(getConfig().getColorAccent());
        APPSTextView.setTextColor(getConfig().gettextColor());
        BYPASS_MODETextView.setTextColor(getConfig().gettextColor());
        mToolbar.setNavigationOnClickListener(v -> {
            hideProgrss();
            finish();
        });
        APPS.setOnCheckedChangeListener((buttonView, isChecked) -> {
            changeDisallowText1(isChecked);
            getConfig().setIsFilterApps(isChecked);
            prefs.edit().putBoolean(AppConfig.PREF_PER_APP_PROXY, isChecked).apply();
        });
        BYPASS_MODE.setOnCheckedChangeListener((buttonView, isChecked) -> {
            changeDisallowText2(isChecked);
            getConfig().setIsFilterBypassMode(isChecked);
            prefs.edit().putBoolean(AppConfig.PREF_BYPASS_APPS, isChecked).apply();
        });
        APPS.setChecked(getConfig().getIsFilterApps());
        BYPASS_MODE.setChecked(getConfig().getIsFilterBypassMode());
        mAppManager = this;
        APPSTextView.setOnClickListener(v -> {
            if (APPS.isChecked()){
                APPS.setChecked(false);
                getConfig().setIsFilterApps(false);
                changeDisallowText1(false);
                prefs.edit().putBoolean(AppConfig.PREF_PER_APP_PROXY, false).apply();
            }else{
                APPS.setChecked(true);
                getConfig().setIsFilterApps(true);
                changeDisallowText1(true);
                prefs.edit().putBoolean(AppConfig.PREF_PER_APP_PROXY, true).apply();
            }
        });
        BYPASS_MODETextView.setOnClickListener(v -> {
            if (BYPASS_MODE.isChecked()){
                BYPASS_MODE.setChecked(false);
                getConfig().setIsFilterBypassMode(false);
                changeDisallowText2(false);
                prefs.edit().putBoolean(AppConfig.PREF_BYPASS_APPS, false).apply();
            }else{
                BYPASS_MODE.setChecked(true);
                getConfig().setIsFilterBypassMode(true);
                changeDisallowText2(true);
                prefs.edit().putBoolean(AppConfig.PREF_BYPASS_APPS, true).apply();
            }
        });
    }

    private void hideProgrss(){
        if(mPoint!=null)mPoint.clearAnimation();
        progressSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void showProgrss(){
        ((TextView)findViewById(R.id.progTv)).setText("Loading app list please wait...");
        progressSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        RotateAnimation ra = new RotateAnimation(0,360, Animation.RELATIVE_TO_PARENT,0.37f,Animation.RELATIVE_TO_PARENT,0.37f);
        ra.setDuration(2000);
        ra.setRepeatCount(Animation.INFINITE);
        ra.setRepeatMode(Animation.RESTART);
        mPoint.startAnimation(ra);
    }

    @SuppressLint("SetTextI18n")
    private void changeDisallowText1(boolean selectedAreDisallowed) {
        if (selectedAreDisallowed){
            APPSTextView.setText("Apps Filter Enabled");
            BsetEnabled.setVisibility(View.GONE);
        }else{
            APPSTextView.setText("Apps Filter Disabled");
            BsetEnabled.setVisibility(View.VISIBLE);
        }
    }
    @SuppressLint("SetTextI18n")
    private void changeDisallowText2(boolean selectedAreDisallowed) {
        if (selectedAreDisallowed)
            BYPASS_MODETextView.setText("VPN is used for all apps but exclude selected");
        else
            BYPASS_MODETextView.setText("VPN is used for only for selected apps");
    }

    @Override
    public void onBackPressed(){
        hideProgrss();
        finish();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        changeDisallowText1(Objects.requireNonNull(getConfig()).getIsFilterApps());
        changeDisallowText2(getConfig().getIsFilterBypassMode());
        new Thread() {
            @Override
            public void run() {
                handler.sendEmptyMessage(MSG_LOAD_START);

                listApps = findViewById(R.id.applistview);

                if (!appsLoaded)
                    loadApps();
                handler.sendEmptyMessage(MSG_LOAD_FINISH);
            }
        }.start();
    }

    Set<String> mApps = new HashSet<>();
    private void saveAppSettings() {
        try {
            if (apps == null){
              return;
            }
            if (mApps!=null){
                mApps.clear();
            }
            SharedPreferences prefs = harliesApplication.getDefaultSharedPreferences();
            Editor edit = prefs.edit();
            StringBuilder tordApps = new StringBuilder();
            for (TorifiedApp app : apps) {
                if (app.isTorified()) {
                    tordApps.append(app.getUsername());
                    tordApps.append("|");
                    mApps.add(app.getUsername());
                }
            }
            Objects.requireNonNull(getConfig()).setPackageFilterApps(mApps);
            edit.putString("PROXYED_APPS", tordApps.toString()).apply();
            edit.putStringSet(AppConfig.PREF_PER_APP_PROXY_SET,mApps).apply();
        }catch (Exception e){
            addlogInfo(e.toString());
        }
    }

}
