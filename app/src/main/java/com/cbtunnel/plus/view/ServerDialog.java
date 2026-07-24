package com.cbtunnel.plus.view;

import android.app.DatePickerDialog;
import android.content.Context;
import androidx.appcompat.app.AlertDialog;

import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import com.cbtunnel.plus.R;
import com.cbtunnel.plus.adapter.SerAdapter;
import com.cbtunnel.plus.config.ConfigUtil;
import com.cbtunnel.plus.utils.FileUtils;
import com.cbtunnel.plus.utils.SpinnerListener;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import android.widget.AdapterView;
import com.google.android.material.textfield.TextInputLayout;
import com.cbtunnel.plus.config.SettingsConstants;
import com.cbtunnel.plus.utils.util;

import android.widget.TextView;
import android.widget.Toast;

public class ServerDialog implements SettingsConstants{
    private final int mTimeFormat = -100;
    public static final int TIME_FORMAT_NONE = 0;
    public static final int TIME_FORMAT_SHORT = 1;
    public static final int TIME_FORMAT_ISO = 2;
    private long mValidade = 0;
    private final AlertDialog a;
    private final Context c;
    private TextView sName_xp;
    private TextInputLayout server_web_renew_lay;
    private EditText sName,etServerIP,etServerCloudFront,etServerHTTP,etTcpPort,etSSLPort,etUser,etPass,etCertificate, etProxyHost,etProxyPort,edServerWebRenew;
    private Spinner sFlag,category, serverType;
    private CheckBox ckUseLogin, ckMultiCert, protegerCheck;
    private View v;
    private final ConfigUtil mConfig;
    private boolean isAddOrEdited = false;

    public ServerDialog(Context c) {
        this.c = c;
        mConfig = ConfigUtil.getInstance(c);
        a = new AlertDialog.Builder(c,mConfig.getFullAlertDialog()).create();
        a.setCancelable(false);
    }

    public void add() {
        v = LayoutInflater.from(c).inflate(R.layout.dialog_add_server, null);
        v.findViewById(R.id.color_bg).setBackgroundColor(mConfig.getColorAccent());
        ((TextView)v.findViewById(R.id.cancel_tv)).setTextColor(mConfig.getColorAccent());
        v.findViewById(R.id.save).setBackgroundTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
        sName = v.findViewById(R.id.etServerName);
        sName = v.findViewById(R.id.etServerName);
        sFlag = v.findViewById(R.id.flagspin);
        category = v.findViewById(R.id.categorySpinner);
        etServerIP = v.findViewById(R.id.etServerIP);
        etServerCloudFront = v.findViewById(R.id.etServerCloudFront);
        etServerHTTP = v.findViewById(R.id.etServerHTTP);
        etTcpPort = v.findViewById(R.id.etTcpPort);
        etSSLPort = v.findViewById(R.id.etSSLPort);
        etCertificate = v.findViewById(R.id.etCertificate);
        etProxyHost = v.findViewById(R.id.etServerProxyHost);
        etProxyPort = v.findViewById(R.id.etServerProxyPort);
        etUser = v.findViewById(R.id.etUser);
        etPass = v.findViewById(R.id.etPass);
        ckUseLogin = v.findViewById(R.id.ckUseLogin);
        ckMultiCert = v.findViewById(R.id.ckMultiCert);
        protegerCheck = v.findViewById(R.id.server_xp);
        edServerWebRenew = v.findViewById(R.id.server_web_renew);
        server_web_renew_lay = v.findViewById(R.id.server_web_renew_lay);
        sName_xp = v.findViewById(R.id.sName_xp);
        server_web_renew_lay.setBoxStrokeColor(mConfig.getColorAccent());
        ckUseLogin.setTextColor(mConfig.getColorAccent());
        ckMultiCert.setTextColor(mConfig.getColorAccent());
        protegerCheck.setTextColor(mConfig.getColorAccent());
        ckUseLogin.setTextSize(TypedValue.COMPLEX_UNIT_DIP,8);
        ckMultiCert.setTextSize(TypedValue.COMPLEX_UNIT_DIP,8);
        protegerCheck.setTextSize(TypedValue.COMPLEX_UNIT_DIP,8);
        sName_xp.setTextSize(TypedValue.COMPLEX_UNIT_DIP,8);
        sName_xp.setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
        ((TextView)v.findViewById(R.id.notiftext1)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
        ((TextView)v.findViewById(R.id.savetv)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
        ckUseLogin.setButtonTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
        ckMultiCert.setButtonTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
        protegerCheck.setButtonTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
        int[] title = {R.id.title1,R.id.title2,R.id.title3,R.id.title4,R.id.title5};
        for (int t : title) {
            ((TextView) v.findViewById(t)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
            v.findViewById(t).setBackgroundColor(mConfig.getColorAccent());
        }
        int[] txtly = {R.id.TextInputLayout1,R.id.TextInputLayout2,R.id.TextInputLayout3,R.id.TextInputLayout4,R.id.etServerIP_ly,R.id.etServerCloudFront_ly,R.id.etTcpPort_ly,R.id.etSSLPort_ly,R.id.etServerProxyHost_ly,R.id.etServerProxyPort_ly,R.id.etCertificate_ly};
        for (int tl : txtly) {
            ((TextInputLayout) v.findViewById(tl)).setBoxStrokeColor(mConfig.getColorAccent());
        }
        try {
            String[] list = c.getAssets().list("flags");
            ArrayList<String> flg = new ArrayList<>();
            Collections.addAll(flg, list);
            sFlag.setAdapter(new SerAdapter(c,flg));
        } catch (Exception e) {
            util.showToast("Server Dialog",e.getMessage());
        }
        serverType = v.findViewById(R.id.server_sptype);
        serverType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                v.findViewById(R.id.title3).setVisibility(View.VISIBLE);
                v.findViewById(R.id.etTcpPort_ly).setVisibility(View.VISIBLE);
                v.findViewById(R.id.etSSLPort).setVisibility(View.VISIBLE);
                ckMultiCert .setVisibility(View.GONE);
                v.findViewById(R.id.etServerIP_ly).setVisibility(View.VISIBLE);
                v.findViewById(R.id.etCertificate_ly).setVisibility(View.GONE);
                if (position==0 || position==1){
                    ((TextInputLayout)v.findViewById(R.id.etTcpPort_ly)).setHint(position==0?"TCP Port:UDP Port":"SSH Port:Dropbear");
                    ((TextInputLayout)v.findViewById(R.id.etServerIP_ly)).setHint("Server IP/Host (cf)");
                    ((TextInputLayout)v.findViewById(R.id.etServerCloudFront_ly)).setHint("Cloud Front DNS (ws)");
                    v.findViewById(R.id.http_ly).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.title3).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.etTcpPort_ly).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.etSSLPort_ly).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.etServerProxyLay).setVisibility(View.VISIBLE);
                    if(position==0){
                        ckMultiCert .setVisibility(View.VISIBLE);
                        v.findViewById(R.id.etCertificate_ly).setVisibility(ckMultiCert.isChecked()?View.VISIBLE:View.GONE);
                    }
                }else if (position==2){
                    ((TextInputLayout)v.findViewById(R.id.etServerIP_ly)).setHint("Server IP/Host (ServerName)");
                    ((TextInputLayout)v.findViewById(R.id.etServerCloudFront_ly)).setHint("Public Key");
                    v.findViewById(R.id.http_ly).setVisibility(View.GONE);
                }else if (position==3){
                    v.findViewById(R.id.etServerIP_ly).setVisibility(View.GONE);
                    ((TextInputLayout)v.findViewById(R.id.etServerCloudFront_ly)).setHint("v2ray Config");
                    v.findViewById(R.id.http_ly).setVisibility(View.GONE);
                }
                else if (position==4){
                    ((TextInputLayout)v.findViewById(R.id.etServerIP_ly)).setHint("Server IP/Host (cf)");
                    ((TextInputLayout)v.findViewById(R.id.etServerCloudFront_ly)).setHint("Cloud Front DNS (ws)");
                    v.findViewById(R.id.http_ly).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.title3).setVisibility(View.GONE);
                    v.findViewById(R.id.etTcpPort_ly).setVisibility(View.GONE);
                    v.findViewById(R.id.etSSLPort_ly).setVisibility(View.GONE);
                    v.findViewById(R.id.etServerProxyLay).setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        ckMultiCert.setOnClickListener(p1 -> {
            if (ckMultiCert.isChecked()){
                v.findViewById(R.id.etCertificate_ly).setVisibility(View.VISIBLE);
            }else {
                v.findViewById(R.id.etCertificate_ly).setVisibility(View.GONE);
            }
        });
        ckUseLogin.setOnClickListener(p1 -> {
            if (ckUseLogin.isChecked()){
                v.findViewById(R.id.Account_ly).setVisibility(View.VISIBLE);
            }else {
                v.findViewById(R.id.Account_ly).setVisibility(View.GONE);
            }
        });
        protegerCheck.setOnClickListener(p1 -> {
            if (protegerCheck.isChecked()) {
                server_web_renew_lay.setVisibility(View.VISIBLE);
                setValidadeDate(c);
            }
            else {
                server_web_renew_lay.setVisibility(View.GONE);
                mValidade = 0;
            }
        });
        isAddOrEdited = true;
        a.setView(v);
    }


    public void edit(JSONObject json) {
        v=LayoutInflater.from(c).inflate(R.layout.dialog_add_server, null);
        v.findViewById(R.id.color_bg).setBackgroundColor(mConfig.getColorAccent());
        ((TextView)v.findViewById(R.id.cancel_tv)).setTextColor(mConfig.getColorAccent());
        v.findViewById(R.id.save).setBackgroundTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
        sName = v.findViewById(R.id.etServerName);
        sFlag = v.findViewById(R.id.flagspin);
        category = v.findViewById(R.id.categorySpinner);
        etServerIP = v.findViewById(R.id.etServerIP);
        etServerCloudFront = v.findViewById(R.id.etServerCloudFront);
        etServerHTTP = v.findViewById(R.id.etServerHTTP);
        etTcpPort = v.findViewById(R.id.etTcpPort);
        etSSLPort = v.findViewById(R.id.etSSLPort);
        etCertificate = v.findViewById(R.id.etCertificate);
        etProxyHost = v.findViewById(R.id.etServerProxyHost);
        etProxyPort = v.findViewById(R.id.etServerProxyPort);
        etUser = v.findViewById(R.id.etUser);
        etPass = v.findViewById(R.id.etPass);
        ckUseLogin = v.findViewById(R.id.ckUseLogin);
        ckMultiCert = v.findViewById(R.id.ckMultiCert);
        protegerCheck = v.findViewById(R.id.server_xp);
        server_web_renew_lay = v.findViewById(R.id.server_web_renew_lay);
        edServerWebRenew = v.findViewById(R.id.server_web_renew);
        sName_xp = v.findViewById(R.id.sName_xp);
        server_web_renew_lay.setBoxStrokeColor(mConfig.getColorAccent());
        ckUseLogin.setTextColor(mConfig.getColorAccent());
        ckMultiCert.setTextColor(mConfig.getColorAccent());
        protegerCheck.setTextColor(mConfig.getColorAccent());
        ckUseLogin.setTextSize(TypedValue.COMPLEX_UNIT_DIP,8);
        ckMultiCert.setTextSize(TypedValue.COMPLEX_UNIT_DIP,8);
        protegerCheck.setTextSize(TypedValue.COMPLEX_UNIT_DIP,8);
        sName_xp.setTextSize(TypedValue.COMPLEX_UNIT_DIP,8);
        ((TextView)v.findViewById(R.id.savetv)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
        ((TextView)v.findViewById(R.id.notiftext1)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
        sName_xp.setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
        ckUseLogin.setButtonTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
        ckMultiCert.setButtonTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
        protegerCheck.setButtonTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
        int[] title = {R.id.title1,R.id.title2,R.id.title3,R.id.title4,R.id.title5};
        for (int t : title) {
            ((TextView) v.findViewById(t)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
            v.findViewById(t).setBackgroundColor(mConfig.getColorAccent());
        }
        int[] txtly = {R.id.TextInputLayout1,R.id.TextInputLayout2,R.id.TextInputLayout3,R.id.TextInputLayout4,R.id.etServerIP_ly,R.id.etServerCloudFront_ly,R.id.etTcpPort_ly,R.id.etSSLPort_ly,R.id.etServerProxyHost_ly,R.id.etServerProxyPort_ly,R.id.etCertificate_ly};
        for (int tl : txtly) {
            ((TextInputLayout) v.findViewById(tl)).setBoxStrokeColor(mConfig.getColorAccent());
        }
        try {
            String[] list = c.getAssets().list("flags");
            ArrayList<String> flg = new ArrayList<>();
            for (String s : list) {
                flg.add(s);
            }
            sFlag.setAdapter(new SerAdapter(c,flg));
        } catch (Exception e) {
            util.showToast("Server Dialog",e.getMessage());
        }
        serverType = v.findViewById(R.id.server_sptype);
        serverType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                v.findViewById(R.id.title3).setVisibility(View.VISIBLE);
                v.findViewById(R.id.etTcpPort_ly).setVisibility(View.VISIBLE);
                v.findViewById(R.id.etSSLPort).setVisibility(View.VISIBLE);
                ckMultiCert .setVisibility(View.GONE);
                v.findViewById(R.id.etServerIP_ly).setVisibility(View.VISIBLE);
                v.findViewById(R.id.etCertificate_ly).setVisibility(View.GONE);
                if (position==0 || position==1){
                    ((TextInputLayout)v.findViewById(R.id.etTcpPort_ly)).setHint(position==0?"TCP Port:UDP Port":"SSH Port:Dropbear");
                    ((TextInputLayout)v.findViewById(R.id.etServerIP_ly)).setHint("Server IP/Host (cf)");
                    ((TextInputLayout)v.findViewById(R.id.etServerCloudFront_ly)).setHint("Cloud Front DNS (ws)");
                    v.findViewById(R.id.http_ly).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.title3).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.etTcpPort_ly).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.etSSLPort_ly).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.etServerProxyLay).setVisibility(View.VISIBLE);
                    if(position==0){
                        ckMultiCert .setVisibility(View.VISIBLE);
                        v.findViewById(R.id.etCertificate_ly).setVisibility(ckMultiCert.isChecked()?View.VISIBLE:View.GONE);
                    }
                }else if (position==2){
                    ((TextInputLayout)v.findViewById(R.id.etServerIP_ly)).setHint("Server IP/Host (ServerName)");
                    ((TextInputLayout)v.findViewById(R.id.etServerCloudFront_ly)).setHint("Public Key");
                    v.findViewById(R.id.http_ly).setVisibility(View.GONE);
                }else if (position==3){
                    v.findViewById(R.id.etServerIP_ly).setVisibility(View.GONE);
                    ((TextInputLayout)v.findViewById(R.id.etServerCloudFront_ly)).setHint("v2ray Config");
                    v.findViewById(R.id.http_ly).setVisibility(View.GONE);
                }
                else if (position==4){
                    ((TextInputLayout)v.findViewById(R.id.etServerIP_ly)).setHint("Server IP/Host (cf)");
                    ((TextInputLayout)v.findViewById(R.id.etServerCloudFront_ly)).setHint("Cloud Front DNS (ws)");
                    v.findViewById(R.id.http_ly).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.title3).setVisibility(View.GONE);
                    v.findViewById(R.id.etTcpPort_ly).setVisibility(View.GONE);
                    v.findViewById(R.id.etSSLPort_ly).setVisibility(View.GONE);
                    v.findViewById(R.id.etServerProxyLay).setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        ckMultiCert.setOnClickListener(p1 -> {
            if (ckMultiCert.isChecked()){
                v.findViewById(R.id.etCertificate_ly).setVisibility(View.VISIBLE);
            }else {
                v.findViewById(R.id.etCertificate_ly).setVisibility(View.GONE);
            }
        });
        ckUseLogin.setOnClickListener(p1 -> {
            if (ckUseLogin.isChecked()){
                v.findViewById(R.id.Account_ly).setVisibility(View.VISIBLE);
            }else {
                v.findViewById(R.id.Account_ly).setVisibility(View.GONE);
            }
        });
        protegerCheck.setOnClickListener(p1 -> {
            if (protegerCheck.isChecked()) {
                server_web_renew_lay.setVisibility(View.VISIBLE);
                setValidadeDate(c);
            }
            else {
                sName_xp.setVisibility(View.GONE);
                server_web_renew_lay.setVisibility(View.GONE);
                mValidade = 0;
            }
        });
        try {
            sName.setText(json.getString("Name"));
            etServerIP.setText(FileUtils.showJson(json.getString("ServerIP")));
            etServerCloudFront.setText(FileUtils.showJson(json.getString("ServerCloudFront")));
            etServerHTTP.setText(FileUtils.showJson(json.getString("ServerHTTP")));
            etTcpPort.setText(json.getString("TcpPort"));
            etSSLPort.setText(json.getString("SSLPort"));
            etProxyHost.setText(json.has("ProxyHost")?FileUtils.showJson(json.getString("ProxyHost")):"");
            etProxyPort.setText(json.has("ProxyPort")?json.getString("ProxyPort"):"");
            etCertificate.setText(json.has("ovpnCertificate")?FileUtils.showJson(json.getString("ovpnCertificate")):"");
            ckMultiCert.setChecked(json.has("MultiCert")?json.getBoolean("MultiCert"):false);
            ckUseLogin.setChecked(json.getBoolean("AutoLogIn"));
            isAddOrEdited = json.has("isAddOrEdited");
            etUser.setText(json.getBoolean("AutoLogIn")?FileUtils.showJson(json.getString("Username")):"");
            etPass.setText(json.getBoolean("AutoLogIn")?FileUtils.showJson(json.getString("Password")):"");
            String[] list = c.getAssets().list("flags");
            for (int i = 0; i < list.length; i++) {
                if (list[i].replace("flag_","").replace(".png","").equals(json.getString("FLAG"))) {
                    sFlag.setSelection(i);
                }
            }
            edServerWebRenew.setText(json.has("server_web_renew")? json.getString("server_web_renew"):"");
            serverType.setSelection(json.getInt("serverType"));
            category.setSelection(json.getInt("Category"));
            v.findViewById(R.id.Account_ly).setVisibility(json.getBoolean("AutoLogIn")?View.VISIBLE:View.GONE);
            if(json.getInt("serverType")==0&&json.has("MultiCert")){
                v.findViewById(R.id.etCertificate_ly).setVisibility(json.getBoolean("MultiCert")?View.VISIBLE:View.GONE);
            }
            if (json.has("Server_exp_box")){
                protegerCheck.setChecked(json.getBoolean("Server_exp_box"));
                if (Long.parseLong(json.getString("Server_exp").split("-spliter-")[0])!=0){
                    mValidade = Long.parseLong(json.getString("Server_exp").split("-spliter-")[0]);
                    String getTime = json.getString("Server_exp").split("-spliter-")[1];
                    Calendar cal = Calendar.getInstance();
                    long time_hoje = cal.getTimeInMillis();
                    DateFormat df = DateFormat.getDateInstance();
                    long dias = ((mValidade-time_hoje)/1000/60/60/24);
                    sName_xp.setVisibility(View.VISIBLE);
                    server_web_renew_lay.setVisibility(View.VISIBLE);
                    if (String.format("%s (%s)", dias, df.format(mValidade)).startsWith("1 (")){
                        sName_xp.setText(String.format("%s Day left Until (%s) ", dias, df.format(mValidade))+getTime);
                    }else{
                        sName_xp.setText(String.format("%s Days left Until (%s) ", dias, df.format(mValidade))+getTime);
                    }
                }
            }else{
                mValidade = 0;
                protegerCheck.setChecked(false);
                sName_xp.setVisibility(View.GONE);
                server_web_renew_lay.setVisibility(View.GONE);
            }
        } catch (Exception ignored) {}
        a.setView(v);
    }
    public void onServerAdd(final SpinnerListener oca) {
        v.findViewById(R.id.cancel).setOnClickListener(p1 -> a.dismiss());
        v.findViewById(R.id.save).setOnClickListener(p1 -> {
            JSONObject jo=new JSONObject();
            try {
                int position2 = sFlag.getSelectedItemPosition();
                String[] list = c.getAssets().list("flags");
                boolean x = serverType.getSelectedItemPosition()==0;
                boolean y = x?ckMultiCert.isChecked():false;
                jo.put("Name", sName.getText().toString());
                jo.put("serverType", serverType.getSelectedItemPosition());
                jo.put("FLAG", list[position2].replace("flag_","").replace(".png",""));
                jo.put("Category", category.getSelectedItemPosition());
                jo.put("ServerIP", FileUtils.hideJson(etServerIP.getText().toString()));
                jo.put("ServerCloudFront", FileUtils.hideJson(etServerCloudFront.getText().toString()));
                jo.put("ServerHTTP", FileUtils.hideJson(etServerHTTP.getText().toString()));
                jo.put("TcpPort", etTcpPort.getText().toString());
                jo.put("SSLPort", etSSLPort.getText().toString());
                jo.put("ProxyHost", FileUtils.hideJson(etProxyHost.getText().toString()));
                jo.put("ProxyPort", etProxyPort.getText().toString());
                jo.put("AutoLogIn", ckUseLogin.isChecked());
                jo.put("Username", ckUseLogin.isChecked()?FileUtils.hideJson(etUser.getText().toString()):"");
                jo.put("Password", ckUseLogin.isChecked()?FileUtils.hideJson(etPass.getText().toString()):"");
                jo.put("MultiCert", y);
                jo.put("ovpnCertificate", y?FileUtils.hideJson(etCertificate.getText().toString()):"");
                jo.put("Server_exp_box", protegerCheck.isChecked());
                jo.put("Server_exp", mValidade+"-spliter-"+getCurrentTime(mTimeFormat));
                jo.put("server_web_renew", edServerWebRenew.getText().toString());
                if (isAddOrEdited)jo.put("isAddOrEdited",true);
                oca.onAdd(jo);
                a.dismiss();
            } catch (Exception e) {
                util.showToast("Server Dialog",e.getMessage());
            }
        });
    }

    private void setValidadeDate(final Context mComtext) {
        sName_xp.setVisibility(sName_xp.getText().toString().isEmpty()?View.GONE:View.VISIBLE);
        Calendar c = Calendar.getInstance();
        final long time_hoje = c.getTimeInMillis();
        c.setTimeInMillis(time_hoje+(1000*60*60*24));
        int mYear = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH);
        int mDay = c.get(Calendar.DAY_OF_MONTH);
        mValidade = c.getTimeInMillis();
        final DatePickerDialog dialog = new DatePickerDialog(mComtext,
                (p1, year, monthOfYear, dayOfMonth) -> {
                    Calendar c12 = Calendar.getInstance();
                    c12.set(year, monthOfYear, dayOfMonth);
                    mValidade = c12.getTimeInMillis();
                },
                mYear, mMonth, mDay);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "ok",
                (dialog2, which) -> {
                    DateFormat df = DateFormat.getDateInstance();
                    DatePicker date = dialog.getDatePicker();
                    Calendar c1 = Calendar.getInstance();
                    c1.set(date.getYear(), date.getMonth(), date.getDayOfMonth());
                    mValidade = c1.getTimeInMillis();
                    if (mValidade < time_hoje) {
                        mValidade = 0;
                        Toast.makeText(mComtext, "The selected day cannot be less than today", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        long dias = ((mValidade-time_hoje)/1000/60/60/24);
                        sName_xp.setVisibility(View.VISIBLE);
                        if (String.format("%s (%s)", dias, df.format(mValidade)).startsWith("1 (")){
                            sName_xp.setText(String.format("%s Day left Until (%s) ", dias, df.format(mValidade))+getCurrentTime(mTimeFormat));
                        }else{
                            sName_xp.setText(String.format("%s Days left Until (%s) ", dias, df.format(mValidade))+getCurrentTime(mTimeFormat));
                        }
                    }
                }
        );
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", (dialog1, which) -> {
            sName_xp.setVisibility(View.GONE);
            mValidade = 0;
            protegerCheck.setChecked(false);
            server_web_renew_lay.setVisibility(View.GONE);
        });
        dialog.setOnCancelListener(v1 -> {
            sName_xp.setVisibility(View.GONE);
            mValidade = 0;
            server_web_renew_lay.setVisibility(View.GONE);
            protegerCheck.setChecked(false);
        });
        dialog.show();
    }

    private String getCurrentTime(int time) {
        if (time != TIME_FORMAT_NONE) {
            Date d = new Date(System.currentTimeMillis());
            java.text.DateFormat timeformat;
            if (time == TIME_FORMAT_ISO)
                timeformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            else if (time == TIME_FORMAT_SHORT)
                timeformat = new SimpleDateFormat("HH:mm");
            else
                timeformat = android.text.format.DateFormat.getTimeFormat(c);

            return timeformat.format(d);
        } else {
            return "";
        }
    }

    public void init() {
        a.show();
    }
}