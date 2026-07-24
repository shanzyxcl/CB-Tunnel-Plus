package com.cbtunnel.plus.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.cbtunnel.plus.R;
import com.cbtunnel.plus.harliesApplication;
import com.cbtunnel.plus.config.ConfigUtil;
import com.cbtunnel.plus.config.SettingsConstants;
import java.lang.reflect.Field;

import android.telephony.TelephonyManager;
import io.michaelrocks.paranoid.Obfuscate;

@Obfuscate
@SuppressLint("StaticFieldLeak")
public class util implements SettingsConstants {

    private static Context mContext;
    private static ConfigUtil mConfig;

    public static String x = new String(new byte[]{68,101,120,116,101,114,69,115,107,97,108,97,114,116,101,50,48,50,52});

    public util(Context c)
    {
        mContext = c;
        mConfig = ConfigUtil.getInstance(c);
    }
    private static Context mContext(){
        if(mContext==null){
            return harliesApplication.getApp();
        }
        return mContext;
    }

    public void overrideFont(String defaultFontNameToOverride, String customFontFileNameInAssets) {
        try {
            final Typeface customFontTypeface = Typeface.createFromAsset(mContext().getAssets(), customFontFileNameInAssets);
            final Field defaultFontTypefaceField = Typeface.class.getDeclaredField(defaultFontNameToOverride);
            defaultFontTypefaceField.setAccessible(true);
            defaultFontTypefaceField.set(null, customFontTypeface);
        } catch (Exception e) {
            Toast.makeText(mContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void showToast(String title, String subtitle){
        Toast.makeText(mContext(), title+" "+subtitle, Toast.LENGTH_LONG).show();
    }

    public static boolean isMyApp(){
        if(!mContext().getString(R.string.app_name).equals(new String(new byte[]{67,66,32,84,117,110,110,101,108,32,80,108,117,115,}))){
            return false;
        }else return mContext().getPackageName().equals(new String(new byte[]{99,111,109,46,99,98,116,117,110,110,101,108,46,112,108,117,115,}));
    }
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        return info != null && info.isAvailable() && info.isConnected();
    }

    public static String getNetworkType() {
        ConnectivityManager cm = (ConnectivityManager)mContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null || !info.isConnected())
            return ""; // not connected
        if (info.getType() == ConnectivityManager.TYPE_WIFI)
            return "WI-FI: ";
        if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
            int networkType = info.getSubtype();

            if (networkType == TelephonyManager.NETWORK_TYPE_GPRS || networkType == TelephonyManager.NETWORK_TYPE_EDGE || networkType == TelephonyManager.NETWORK_TYPE_CDMA || networkType == TelephonyManager.NETWORK_TYPE_1xRTT || networkType == TelephonyManager.NETWORK_TYPE_IDEN || networkType == TelephonyManager.NETWORK_TYPE_GSM) {      // api<25: replace by 16
                return "MOBILE: ";
            } else if (networkType == TelephonyManager.NETWORK_TYPE_UMTS || networkType == TelephonyManager.NETWORK_TYPE_EVDO_0 || networkType == TelephonyManager.NETWORK_TYPE_EVDO_A || networkType == TelephonyManager.NETWORK_TYPE_HSDPA || networkType == TelephonyManager.NETWORK_TYPE_HSUPA || networkType == TelephonyManager.NETWORK_TYPE_HSPA || networkType == TelephonyManager.NETWORK_TYPE_EVDO_B || networkType == TelephonyManager.NETWORK_TYPE_EHRPD || networkType == TelephonyManager.NETWORK_TYPE_HSPAP || networkType == TelephonyManager.NETWORK_TYPE_TD_SCDMA) { // api<25: replace by 17
                return "MOBILE: ";
            } else if (networkType == TelephonyManager.NETWORK_TYPE_LTE || networkType == TelephonyManager.NETWORK_TYPE_IWLAN || networkType == 19) { // LTE_CA
                return "MOBILE: ";
            } else if (networkType == TelephonyManager.NETWORK_TYPE_NR) {       // api<29: replace by 20
                return "MOBILE: ";
            }
            return "";
        }
        return "";
    }

    public static String pw_repl(String user, String pw) {
        return pw;
    }

    public static String b(Context context) {
        try {
            StringBuilder sb = new StringBuilder();
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature sign : info.signatures) {
                sb.append(sign.toCharsString());
            }
            return sb.toString();
        }catch(Exception e){
            return "";
        }
    }


}
   
