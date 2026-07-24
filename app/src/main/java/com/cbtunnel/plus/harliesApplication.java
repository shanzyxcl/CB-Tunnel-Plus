package com.cbtunnel.plus;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import com.cbtunnel.plus.activities.OpenVPNClient;
import com.cbtunnel.plus.config.ConfigUtil;
import com.cbtunnel.plus.utils.util;
import com.tencent.mmkv.MMKV;
import net.openvpn.openvpn.BuildConfig;

public class harliesApplication extends Application {
    private static SharedPreferences privateSharedPreferences;
    private static SharedPreferences defaultSharedPreferences;
    private static harliesApplication MainApp;
    private static final String PREF_LAST_VERSION = "pref_last_version";
    static boolean firstRun = false;
    private static final String PREFS_GERAL = "HarlieApplication";

    private AppOpenAdManager appOpenAdManager;

    @Override
    public void onCreate() {
        super.onCreate();
        MainApp = harliesApplication.this;
        TopExceptionHandler.init(harliesApplication.this);
        appOpenAdManager = new AppOpenAdManager(this);
        privateSharedPreferences = getSharedPreferences(PREFS_GERAL, MODE_PRIVATE);
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(harliesApplication.this);
        firstRun = defaultSharedPreferences.getInt(PREF_LAST_VERSION, 0) != BuildConfig.VERSION_CODE;
        if (firstRun) defaultSharedPreferences.edit().putInt(PREF_LAST_VERSION, BuildConfig.VERSION_CODE).apply();
        ConfigUtil.getInstance(harliesApplication.this);
        ConfigUtil.setNotificationActivityClass(OpenVPNClient.class);
        new util(harliesApplication.this).overrideFont("SERIF", "montserrat_medium.ttf");
        MMKV.initialize(harliesApplication.this);
    }

    public static harliesApplication getApp() {
        return MainApp;
    }

    public static String resString(int res_id) {
        return MainApp.getResources().getString(res_id);
    }

    public static SharedPreferences getDefaultSharedPreferences() {
        return defaultSharedPreferences;
    }
    public static SharedPreferences getPrivateSharedPreferences() {
        return privateSharedPreferences;
    }



    public void loadAd(@NonNull Activity activity) {
        appOpenAdManager.loadAd(activity);
    }

    public void showAdIfAvailable(@NonNull Activity activity, @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {
        appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener);
    }

    public interface OnShowAdCompleteListener {
        void onShowAdComplete();
    }
}
