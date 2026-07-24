package com.cbtunnel.plus.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import androidx.appcompat.app.AlertDialog;
import android.widget.TextView;
import com.google.android.material.textfield.TextInputLayout;
import com.cbtunnel.plus.config.ConfigUtil;
import com.cbtunnel.plus.utils.FileUtils;
import com.cbtunnel.plus.utils.SpinnerListener;
import com.cbtunnel.plus.adapter.pAdapter;
import org.json.JSONObject;
import java.util.ArrayList;
import com.cbtunnel.plus.R;
import com.cbtunnel.plus.config.SettingsConstants;
import com.cbtunnel.plus.utils.util;

public class SSLDialog implements SettingsConstants{

    private final AlertDialog a;
    private final Context c;
    private RadioGroup server_type;
    private Spinner pLogo,sslmethod;
    private CheckBox ckUseDefProxy;
    private View v;
    private EditText etSSLName,etSSLSNI,etSSLPayload,etSSLInfo,etSquidProxy,etSquidPort,etSSLPort;
    private final ConfigUtil mConfig;
    private boolean isAddOrEdited = false;
    public SSLDialog(Context c) {
        this.c = c;
        mConfig = ConfigUtil.getInstance(c);
        a = new AlertDialog.Builder(c,mConfig.getFullAlertDialog()).create();
        a.setCancelable(false);
    }

    private String mServerType(){
        if(server_type.getCheckedRadioButtonId()==R.id.cf_radio){
            return "cf";
        }
        if(server_type.getCheckedRadioButtonId()==R.id.ws_radio){
            return "ws";
        }
        return "http";
    }
    
    public void add() {
        v = LayoutInflater.from(c).inflate(R.layout.dialog_add_ssl, null);
        v.findViewById(R.id.color_bg).setBackgroundColor(mConfig.getColorAccent());
        ((TextView)v.findViewById(R.id.cancel_tv)).setTextColor(mConfig.getColorAccent());
        v.findViewById(R.id.save).setBackgroundTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
        pLogo = v.findViewById(R.id.pLogo);
        sslmethod = v.findViewById(R.id.sslmethod);
        server_type = v.findViewById(R.id.server_type);
        etSSLName = v.findViewById(R.id.etSSLName);
        etSSLSNI = v.findViewById(R.id.etSSLSNI);
        etSSLPayload = v.findViewById(R.id.etSSLPayload);
        etSSLInfo = v.findViewById(R.id.etSSLInfo);
        ckUseDefProxy = v.findViewById(R.id.ckUseDefProxy);
        etSquidProxy = v.findViewById(R.id.etSquidProxy);
        etSquidPort = v.findViewById(R.id.etSquidPort);
        etSSLPort = v.findViewById(R.id.etSSLPort);
        ckUseDefProxy.setTextColor(mConfig.getColorAccent());
        ckUseDefProxy.setButtonTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
        ((TextView)v.findViewById(R.id.savetv)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
        ((TextView)v.findViewById(R.id.notiftext1)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
        int[] title = {R.id.title1,R.id.title2,R.id.title3,R.id.title4};
        for (int t : title) {
            ((TextView) v.findViewById(t)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
            v.findViewById(t).setBackgroundColor(mConfig.getColorAccent());
        }
        int[] rb = {R.id.cf_radio,R.id.ws_radio,R.id.http_radio};
        for (int r : rb) {
            ((RadioButton) v.findViewById(r)).setButtonTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
        }
        int[] txtly = {R.id.TextInputLayout1,R.id.TextInputLayout2,R.id.TextInputLayout3,R.id.TextInputLayout4,R.id.TextInputLayout5,R.id.TextInputLayout6,R.id.etNetworkPayloadInput};
        for (int tl : txtly) {
            ((TextInputLayout) v.findViewById(tl)).setBoxStrokeColor(mConfig.getColorAccent());
        }
        ckUseDefProxy.setChecked(true);
        etSquidProxy.setEnabled(false);
        etSquidPort.setEnabled(false);
        server_type.check(R.id.cf_radio);
        sslmethod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position==0){
                    v.findViewById(R.id.etSSLPayload_ly).setVisibility(View.GONE);
                    v.findViewById(R.id.sslproxylay).setVisibility(View.GONE);
                }else if (position==1){
                    v.findViewById(R.id.etSSLPayload_ly).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.sslproxylay).setVisibility(View.GONE);
                }
                else if (position==2){
                    v.findViewById(R.id.etSSLPayload_ly).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.sslproxylay).setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        ckUseDefProxy.setOnClickListener(p1 -> {
            if (ckUseDefProxy.isChecked()){
                etSquidProxy.setText("[Default]");
                etSquidProxy.setEnabled(false);
                etSquidPort.setEnabled(false);
            }else {
                etSquidProxy.setText("");
                etSquidProxy.setEnabled(true);
                etSquidPort.setEnabled(true);
            }
        });
       
        try {
            String[] list = c.getAssets().list("networks");
            ArrayList<String> plg = new ArrayList<>();
            for (String s : list) {
                plg.add(s.replace("icon_", "").replace(".png", ""));
            }
            pLogo.setAdapter(new pAdapter(c,plg));
        } catch (Exception e) {
            util.showToast("SSL Dialog",e.getMessage());
        }
        isAddOrEdited = true;
        a.setView(v);
    }

    public void edit(JSONObject json) {
        v=LayoutInflater.from(c).inflate(R.layout.dialog_add_ssl, null);
        v.findViewById(R.id.color_bg).setBackgroundColor(mConfig.getColorAccent());
        ((TextView)v.findViewById(R.id.cancel_tv)).setTextColor(mConfig.getColorAccent());
        v.findViewById(R.id.save).setBackgroundTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
        pLogo = v.findViewById(R.id.pLogo);
        sslmethod = v.findViewById(R.id.sslmethod);
        server_type = v.findViewById(R.id.server_type);
        etSSLName = v.findViewById(R.id.etSSLName);
        etSSLSNI = v.findViewById(R.id.etSSLSNI);
        etSSLPayload = v.findViewById(R.id.etSSLPayload);
        etSSLInfo = v.findViewById(R.id.etSSLInfo);
        ckUseDefProxy = v.findViewById(R.id.ckUseDefProxy);
        etSquidProxy = v.findViewById(R.id.etSquidProxy);
        etSquidPort = v.findViewById(R.id.etSquidPort);
        etSSLPort = v.findViewById(R.id.etSSLPort);
        ckUseDefProxy.setTextColor(mConfig.getColorAccent());
        ((TextView)v.findViewById(R.id.savetv)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
        ((TextView)v.findViewById(R.id.notiftext1)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
        int[] title = {R.id.title1,R.id.title2,R.id.title3,R.id.title4};
        for (int t : title) {
            ((TextView) v.findViewById(t)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
            v.findViewById(t).setBackgroundColor(mConfig.getColorAccent());
        }
        int[] rb = {R.id.cf_radio,R.id.ws_radio,R.id.http_radio};
        for (int r : rb) {
            ((RadioButton) v.findViewById(r)).setButtonTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
        }
        int[] txtly = {R.id.TextInputLayout1,R.id.TextInputLayout2,R.id.TextInputLayout3,R.id.TextInputLayout4,R.id.TextInputLayout5,R.id.TextInputLayout6,R.id.etNetworkPayloadInput};
        for (int tl : txtly) {
            ((TextInputLayout) v.findViewById(tl)).setBoxStrokeColor(mConfig.getColorAccent());
        }
        sslmethod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position==0){
                    v.findViewById(R.id.etSSLPayload_ly).setVisibility(View.GONE);
                    v.findViewById(R.id.sslproxylay).setVisibility(View.GONE);
                }else if (position==1){
                    v.findViewById(R.id.etSSLPayload_ly).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.sslproxylay).setVisibility(View.GONE);
                }
                else if (position==2){
                    v.findViewById(R.id.etSSLPayload_ly).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.sslproxylay).setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        ckUseDefProxy.setOnClickListener(p1 -> {
            if (ckUseDefProxy.isChecked()){
                etSquidProxy.setText("[Default]");
                etSquidProxy.setEnabled(false);
                etSquidPort.setEnabled(false);
            }else {
                etSquidProxy.setText("");
                etSquidProxy.setEnabled(true);
                etSquidPort.setEnabled(true);
            }
        });
        
        try {
            String[] list = c.getAssets().list("networks");
            ArrayList<String> plg = new ArrayList<>();
            for (String s : list) {
                plg.add(s.replace("icon_", "").replace(".png", ""));
            }
            pLogo.setAdapter(new pAdapter(c,plg));
        } catch (Exception e) {
            util.showToast("SSL Dialog",e.getMessage());
        }
        try {
            String[] list = c.getAssets().list("networks");
            for (int i = 0; i < list.length; i++) {
                if (list[i].replace("icon_","").replace(".png", "").equals(json.getString("FLAG"))) {
                    pLogo.setSelection(i);
                }
            }
            sslmethod.setSelection(json.getInt("proto_spin")-3);
            etSSLName.setText(json.getString("Name"));
            etSSLSNI.setText(FileUtils.showJson(json.getString("SSLSNI")));
            etSSLPayload.setText(FileUtils.showJson(json.getString("SSLPayload")));
            etSSLInfo.setText(json.getString("Info"));
            ckUseDefProxy.setChecked(json.getBoolean("UseDefProxy"));
            etSquidPort.setText(json.getString("SquidPort"));
            etSSLPort.setText(json.getString("SSLPort"));
            isAddOrEdited = json.has("isAddOrEdited");
            if(json.getString("server_type").equals("cf")){
                server_type.check(R.id.cf_radio);
            }
            if(json.getString("server_type").equals("ws")){
                server_type.check(R.id.ws_radio);
            }
            if(json.getString("server_type").equals("http")){
                server_type.check(R.id.http_radio);
            }
            if (json.getBoolean("UseDefProxy")){
                etSquidProxy.setText("[Default]");
                etSquidProxy.setEnabled(false);
                etSquidPort.setEnabled(false);
            }else {
                etSquidProxy.setText(FileUtils.showJson(json.getString("SquidProxy")));
                etSquidProxy.setEnabled(true);
                etSquidPort.setEnabled(true);
            }
        } catch (Exception e) {
            util.showToast("SSL Dialog",e.getMessage());
        }
        a.setView(v);
    }

    public void onPayloadAdd(final SpinnerListener oca) {
        v.findViewById(R.id.cancel).setOnClickListener(p1 -> a.dismiss());
        v.findViewById(R.id.save).setOnClickListener(p1 -> {
        JSONObject jo=new JSONObject();
        try {
            int position1 = pLogo.getSelectedItemPosition();
            String[] list = c.getAssets().list("networks");
            jo.put("FLAG", list[position1].replace("icon_","").replace(".png",""));
            jo.put("proto_spin", sslmethod.getSelectedItemPosition()+3);
            jo.put("server_type", mServerType());
            jo.put("Name", etSSLName.getText().toString());
            jo.put("SSLSNI", FileUtils.hideJson(etSSLSNI.getText().toString()));
            jo.put("SSLPayload", FileUtils.hideJson(etSSLPayload.getText().toString()));
            jo.put("Info", etSSLInfo.getText().toString());
            jo.put("UseDefProxy", ckUseDefProxy.isChecked());
            jo.put("SquidProxy", FileUtils.hideJson(etSquidProxy.getText().toString()));
            jo.put("SquidPort", etSquidPort.getText().toString());
            jo.put("SSLPort", etSSLPort.getText().toString());
            if (isAddOrEdited)jo.put("isAddOrEdited",true);
            oca.onAdd(jo);
            a.dismiss();
        } catch (Exception e) {
            util.showToast("SSL Dialog",e.getMessage());
        }
    });
    }

    public void init() {
        a.show();
    }
}