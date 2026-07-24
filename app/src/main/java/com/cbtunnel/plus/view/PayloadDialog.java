package com.cbtunnel.plus.view;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import com.google.android.material.textfield.TextInputLayout;
import com.cbtunnel.plus.harliesApplication;
import com.cbtunnel.plus.R;
import com.cbtunnel.plus.adapter.pAdapter;
import com.cbtunnel.plus.config.ConfigUtil;
import com.cbtunnel.plus.utils.FileUtils;
import com.cbtunnel.plus.utils.SpinnerListener;
import org.json.JSONObject;
import java.util.ArrayList;
import com.cbtunnel.plus.config.SettingsConstants;
import com.cbtunnel.plus.utils.util;
import android.widget.TextView;

public class PayloadDialog implements SettingsConstants{
    private final SharedPreferences mPref;
    private final AlertDialog a;
    private final Context c;
    private RadioGroup server_type;
    private Spinner pLogo,proto_spin;
    private CheckBox ckUseDefProxy, isReplace;
    private View v;
    private EditText etNetworkName,etNetworkPayload,etNetworkInfo,etSquidProxy,etSquidPort,etNetworkFrontQuery,etNetworkBackQuery;
    private ImageButton btnPayloadGen;
    private final ConfigUtil mConfig;
    private boolean isAddOrEdited = false;

    public PayloadDialog(Context c) {
        this.c = c;
        mConfig = ConfigUtil.getInstance(c);
        mPref = harliesApplication.getPrivateSharedPreferences();
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
        /*v = LayoutInflater.from(c).inflate(R.layout.dialog_add_payload, null);*/
        v.findViewById(R.id.color_bg).setBackgroundColor(mConfig.getColorAccent());
        ((TextView)v.findViewById(R.id.cancel_tv)).setTextColor(mConfig.getColorAccent());
        v.findViewById(R.id.save).setBackgroundTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
        pLogo = v.findViewById(R.id.pLogo);
        proto_spin = v.findViewById(R.id.proto_spin);
        server_type = v.findViewById(R.id.server_type);
        etNetworkName = v.findViewById(R.id.etNetworkName);
        etNetworkPayload = v.findViewById(R.id.etNetworkPayload);
        etNetworkInfo = v.findViewById(R.id.etNetworkInfo);
        ckUseDefProxy = v.findViewById(R.id.ckUseDefProxy);
        etSquidProxy = v.findViewById(R.id.etSquidProxy);
        etSquidPort = v.findViewById(R.id.etSquidPort);
        etNetworkFrontQuery = v.findViewById(R.id.etNetworkFrontQuery);
        etNetworkBackQuery = v.findViewById(R.id.etNetworkBackQuery);
        btnPayloadGen = v.findViewById(R.id.btnPayloadGen);
        btnPayloadGen.setColorFilter(mConfig.getColorAccent(), PorterDuff.Mode.SRC_IN);
        isReplace = v.findViewById(R.id.isReplace);
        isReplace.setTextColor(mConfig.getColorAccent());
        ckUseDefProxy.setTextColor(mConfig.getColorAccent());
        isReplace.setButtonTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
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
        server_type.check(R.id.cf_radio);
        proto_spin.setEnabled(false);
        proto_spin.setSelection(mPref.getInt(network_spin_mSelection_key,0));
        proto_spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position==0){
                    ((TextInputLayout)v.findViewById(R.id.etNetworkPayloadInput)).setHint("Payload");
                    v.findViewById(R.id.excluded_udp_layout).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.radio_type).setVisibility(View.VISIBLE);
                    btnPayloadGen.setVisibility(View.GONE);
                }else if (position==1){
                    ((TextInputLayout)v.findViewById(R.id.etNetworkPayloadInput)).setHint("Config.json");
                    v.findViewById(R.id.excluded_udp_layout).setVisibility(View.GONE);
                    v.findViewById(R.id.radio_type).setVisibility(View.VISIBLE);
                    btnPayloadGen.setVisibility(View.VISIBLE);
                }else if (position==2){
                    ((TextInputLayout)v.findViewById(R.id.etNetworkPayloadInput)).setHint("DNS Address");
                    v.findViewById(R.id.excluded_udp_layout).setVisibility(View.GONE);
                    v.findViewById(R.id.radio_type).setVisibility(View.GONE);
                    btnPayloadGen.setVisibility(View.GONE);
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
            }else {
                etSquidProxy.setText("");
                etSquidProxy.setEnabled(true);
            }
        });
        btnPayloadGen.setOnClickListener(p1 -> {
            final AlertDialog u = new AlertDialog.Builder(c,mConfig.getAlertDialog()).create();
            View view = LayoutInflater.from(c).inflate(R.layout.dialog_udp_maker, null);
            final EditText etUDPPort = view.findViewById(R.id.etUDPPort);
            final EditText etServerObfs = view.findViewById(R.id.etServerObfs);
            final EditText etRetry = view.findViewById(R.id.etRetry);
            final EditText etRetryInterval = view.findViewById(R.id.etRetryInterval);
            final EditText etServerUpMBPS = view.findViewById(R.id.etServerUpMBPS);
            final EditText etServerDownMBPS = view.findViewById(R.id.etServerDownMBPS);
            final EditText etServerRWConn = view.findViewById(R.id.etServerRWConn);
            final EditText etServerRW = view.findViewById(R.id.etServerRW);
            ((TextView)view.findViewById(R.id.gen_title)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
            view.findViewById(R.id.color_bg).setBackgroundColor(mConfig.getColorAccent());
            ((TextView)view.findViewById(R.id.generatetv)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
            ((TextView)view.findViewById(R.id.cancel_tv)).setTextColor(mConfig.getColorAccent());
            view.findViewById(R.id.generate).setBackgroundTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
            int[] udply = {R.id.TextInputLayout1,R.id.TextInputLayout2,R.id.TextInputLayout3,R.id.TextInputLayout4,R.id.TextInputLayout5,R.id.TextInputLayout6,R.id.TextInputLayout7,R.id.TextInputLayout8};
            for (int tl : udply) {
                ((TextInputLayout) view.findViewById(tl)).setBoxStrokeColor(mConfig.getColorAccent());
            }
            view.findViewById(R.id.generate).setOnClickListener(p11 -> {
                try {
                    String mObfs = etServerObfs.getText().toString();
                    String ServerPort = "[host]:"+etUDPPort.getText().toString();
                    String mUser = "[user]";
                    int up = Integer.parseInt(etServerUpMBPS.getText().toString());
                    int dw = Integer.parseInt(etServerDownMBPS.getText().toString());
                    int rt = Integer.parseInt(etRetry.getText().toString());
                    int rt_in = Integer.parseInt(etRetryInterval.getText().toString());
                    String mSocks5 = "127.0.0.1:1080";
                    String mHttp = "127.0.0.1:8989";
                    boolean mInsecure = true;
                    String mCa = "";
                    int mRecv_window_conn =  Integer.parseInt(etServerRWConn.getText().toString());
                    int mRecv_window =  Integer.parseInt(etServerRW.getText().toString());
                    etNetworkPayload.setText(String.format(UDP_CLI, ServerPort, mObfs, mUser, up, dw, rt, rt_in, mSocks5, mHttp, mInsecure, mCa, mRecv_window_conn, mRecv_window));
                    u.dismiss();
                } catch (Exception e) {
                    util.showToast("UDP Generator",e.getMessage());
                }
            });
            view.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View p1) {
                    u.dismiss();
                }
            });
            u.setCancelable(false);
            u.getWindow().getAttributes().windowAnimations = R.style.alertDialog;
            u.setView(view);
            u.show();
        });
        try {
            String[] list = c.getAssets().list("networks");
            ArrayList<String> plg = new ArrayList<>();
            for (String s : list) {
                plg.add(s.replace("icon_", "").replace(".png", ""));
            }
            pLogo.setAdapter(new pAdapter(c,plg));
        } catch (Exception e) {
            util.showToast("Payload Dialog",e.getMessage());
        }
        isAddOrEdited = true;
        a.setView(v);
    }

    public void edit(JSONObject json) {
        /*v=LayoutInflater.from(c).inflate(R.layout.dialog_add_payload, null);*/
        v.findViewById(R.id.color_bg).setBackgroundColor(mConfig.getColorAccent());
        ((TextView)v.findViewById(R.id.cancel_tv)).setTextColor(mConfig.getColorAccent());
        v.findViewById(R.id.save).setBackgroundTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
        pLogo = v.findViewById(R.id.pLogo);
        proto_spin = v.findViewById(R.id.proto_spin);
        server_type = v.findViewById(R.id.server_type);
        etNetworkName = v.findViewById(R.id.etNetworkName);
        etNetworkPayload = v.findViewById(R.id.etNetworkPayload);
        etNetworkInfo = v.findViewById(R.id.etNetworkInfo);
        ckUseDefProxy = v.findViewById(R.id.ckUseDefProxy);
        etSquidProxy = v.findViewById(R.id.etSquidProxy);
        etSquidPort = v.findViewById(R.id.etSquidPort);
        etNetworkFrontQuery = v.findViewById(R.id.etNetworkFrontQuery);
        etNetworkBackQuery = v.findViewById(R.id.etNetworkBackQuery);
        btnPayloadGen = v.findViewById(R.id.btnPayloadGen);
        btnPayloadGen.setColorFilter(mConfig.getColorAccent(), PorterDuff.Mode.SRC_IN);
        isReplace = v.findViewById(R.id.isReplace);
        isReplace.setTextColor(mConfig.getColorAccent());
        ckUseDefProxy.setTextColor(mConfig.getColorAccent());
        isReplace.setButtonTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
        ckUseDefProxy.setButtonTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
        ((TextView)v.findViewById(R.id.notiftext1)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
        ((TextView)v.findViewById(R.id.savetv)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
        int[] title = {R.id.title1,R.id.title2,R.id.title3,R.id.title4};
        for (int t : title) {
            ((TextView) v.findViewById(t)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
            v.findViewById(t).setBackgroundColor(mConfig.getColorAccent());
        }
        int[] txtly = {R.id.TextInputLayout1,R.id.TextInputLayout2,R.id.TextInputLayout3,R.id.TextInputLayout4,R.id.TextInputLayout5,R.id.TextInputLayout6,R.id.etNetworkPayloadInput};
        for (int tl : txtly) {
            ((TextInputLayout) v.findViewById(tl)).setBoxStrokeColor(mConfig.getColorAccent());
        }
        proto_spin.setEnabled(false);
        proto_spin.setSelection(mPref.getInt(network_spin_mSelection_key,0));
        proto_spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position==0){
                    ((TextInputLayout)v.findViewById(R.id.etNetworkPayloadInput)).setHint("Payload");
                    v.findViewById(R.id.excluded_udp_layout).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.radio_type).setVisibility(View.VISIBLE);
                    btnPayloadGen.setVisibility(View.GONE);
                }else if (position==1){
                    ((TextInputLayout)v.findViewById(R.id.etNetworkPayloadInput)).setHint("Config.json");
                    v.findViewById(R.id.excluded_udp_layout).setVisibility(View.GONE);
                    v.findViewById(R.id.radio_type).setVisibility(View.VISIBLE);
                    btnPayloadGen.setVisibility(View.VISIBLE);
                }else if (position==2){
                    ((TextInputLayout)v.findViewById(R.id.etNetworkPayloadInput)).setHint("DNS Address");
                    v.findViewById(R.id.excluded_udp_layout).setVisibility(View.GONE);
                    v.findViewById(R.id.radio_type).setVisibility(View.GONE);
                    btnPayloadGen.setVisibility(View.GONE);
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
            }else {
                etSquidProxy.setText("");
                etSquidProxy.setEnabled(true);
            }
        });
        btnPayloadGen.setOnClickListener(p1 -> {
            final AlertDialog u = new AlertDialog.Builder(c,mConfig.getAlertDialog()).create();
            View view = LayoutInflater.from(c).inflate(R.layout.dialog_udp_maker, null);
            final EditText etUDPPort = view.findViewById(R.id.etUDPPort);
            final EditText etServerObfs = view.findViewById(R.id.etServerObfs);
            final EditText etRetry = view.findViewById(R.id.etRetry);
            final EditText etRetryInterval = view.findViewById(R.id.etRetryInterval);
            final EditText etServerUpMBPS = view.findViewById(R.id.etServerUpMBPS);
            final EditText etServerDownMBPS = view.findViewById(R.id.etServerDownMBPS);
            final EditText etServerRWConn = view.findViewById(R.id.etServerRWConn);
            final EditText etServerRW = view.findViewById(R.id.etServerRW);
            ((TextView)view.findViewById(R.id.gen_title)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
            view.findViewById(R.id.color_bg).setBackgroundColor(mConfig.getColorAccent());
            ((TextView)view.findViewById(R.id.generatetv)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
            ((TextView)view.findViewById(R.id.cancel_tv)).setTextColor(mConfig.getColorAccent());
            view.findViewById(R.id.generate).setBackgroundTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
            int[] udply = {R.id.TextInputLayout1,R.id.TextInputLayout2,R.id.TextInputLayout3,R.id.TextInputLayout4,R.id.TextInputLayout5,R.id.TextInputLayout6,R.id.TextInputLayout7,R.id.TextInputLayout8};
            for (int tl : udply) {
                ((TextInputLayout) view.findViewById(tl)).setBoxStrokeColor(mConfig.getColorAccent());
            }
            view.findViewById(R.id.generate).setOnClickListener(p11 -> {
                try {
                    String mObfs = etServerObfs.getText().toString();
                    String ServerPort = "[host]:"+etUDPPort.getText().toString();
                    String mUser = "[user]";
                    int up = Integer.parseInt(etServerUpMBPS.getText().toString());
                    int dw = Integer.parseInt(etServerDownMBPS.getText().toString());
                    int rt = Integer.parseInt(etRetry.getText().toString());
                    int rt_in = Integer.parseInt(etRetryInterval.getText().toString());
                    String mSocks5 = "127.0.0.1:1080";
                    String mHttp = "127.0.0.1:8989";
                    boolean mInsecure = true;
                    String mCa = "";
                    int mRecv_window_conn =  Integer.parseInt(etServerRWConn.getText().toString());
                    int mRecv_window =  Integer.parseInt(etServerRW.getText().toString());
                    etNetworkPayload.setText(String.format(UDP_CLI, ServerPort, mObfs, mUser, up, dw, rt, rt_in, mSocks5, mHttp, mInsecure, mCa, mRecv_window_conn, mRecv_window));
                    u.dismiss();
                } catch (Exception e) {
                    util.showToast("UDP Generator",e.getMessage());
                }
            });
            view.findViewById(R.id.cancel).setOnClickListener(p112 -> u.dismiss());
            u.setCancelable(false);
            u.getWindow().getAttributes().windowAnimations = R.style.alertDialog;
            u.setView(view);
            u.show();
        });
        try {
            String[] list = c.getAssets().list("networks");
            ArrayList<String> plg = new ArrayList<>();
            for (String s : list) {
                plg.add(s.replace("icon_", "").replace(".png", ""));
            }
            pLogo.setAdapter(new pAdapter(c,plg));
        } catch (Exception e) {
            util.showToast("Payload Dialog",e.getMessage());
        }
        try {
            String[] list = c.getAssets().list("networks");
            for (int i = 0; i < list.length; i++) {
                if (list[i].replace("icon_","").replace(".png", "").equals(json.getString("FLAG"))) {
                    pLogo.setSelection(i);
                }
            }
            proto_spin.setSelection(json.getInt("proto_spin"));
            etNetworkName.setText(json.getString("Name"));
            etNetworkPayload.setText(FileUtils.showJson(json.getString("NetworkPayload")));
            etNetworkInfo.setText(json.getString("Info"));
            ckUseDefProxy.setChecked(json.getBoolean("UseDefProxy"));
            isReplace.setChecked(json.has("AutoReplace")?json.getBoolean("AutoReplace"):false);
            etSquidPort.setText(json.getString("SquidPort"));
            etNetworkFrontQuery.setText(FileUtils.showJson(json.getString("NetworkFrontQuery")));
            etNetworkBackQuery.setText(FileUtils.showJson(json.getString("NetworkBackQuery")));
            isAddOrEdited = json.has("isAddOrEdited");
            int[] rb = {R.id.cf_radio,R.id.ws_radio,R.id.http_radio};
            for (int r : rb) {
                ((RadioButton) v.findViewById(r)).setButtonTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
            }
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
            }else {
                etSquidProxy.setText(FileUtils.showJson(json.getString("SquidProxy")));
                etSquidProxy.setEnabled(true);
            }
        } catch (Exception e) {
            util.showToast("Payload Dialog",e.getMessage());
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
                jo.put("proto_spin", proto_spin.getSelectedItemPosition());
                jo.put("server_type", mServerType());
                jo.put("Name", etNetworkName.getText().toString());
                jo.put("NetworkPayload", FileUtils.hideJson(etNetworkPayload.getText().toString()));
                jo.put("Info", etNetworkInfo.getText().toString());
                jo.put("UseDefProxy", ckUseDefProxy.isChecked());
                jo.put("AutoReplace", isReplace.isChecked());
                jo.put("SquidProxy", FileUtils.hideJson(etSquidProxy.getText().toString()));
                jo.put("SquidPort", etSquidPort.getText().toString());
                jo.put("NetworkFrontQuery", FileUtils.hideJson(etNetworkFrontQuery.getText().toString()));
                jo.put("NetworkBackQuery", FileUtils.hideJson(etNetworkBackQuery.getText().toString()));
                if (isAddOrEdited)jo.put("isAddOrEdited",true);
                oca.onAdd(jo);
                a.dismiss();
            } catch (Exception e) {
                util.showToast("Payload Dialog",e.getMessage());
            }
        });
    }

    public void init() {
        a.show();
    }
}