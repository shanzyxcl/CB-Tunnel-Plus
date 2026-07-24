package com.cbtunnel.plus.activities;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.view.View;
import com.google.android.material.textfield.TextInputEditText;
import android.os.Bundle;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import com.cbtunnel.plus.R;

public class advanceSettings extends OpenVPNClientBase {

    //private final CheckBox[] mCheckBox = new CheckBox[5];
    private CheckBox dns,udp;

    @Override    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getConfig().getColorAccent());
        setContentView(R.layout.activity_ssh);
        Toolbar mToolbar = findViewById(R.id.toolbar);
        mToolbar.setTitle("Options");
        mToolbar.setBackgroundColor(getConfig().getColorAccent());
        mToolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> advanceSettings.this.finish());
        /*final int[] checkbx = new int[]{R.id.ovpn_protocol ,R.id.udp_protocol, R.id.ssh_protocol, R.id.dns_protocol ,R.id.v2ray_protocol};
        ((CheckBox)findViewById(checkbx[getPref().getInt(manual_tunnel_radio_key, 0)])).setChecked(true);
        for(int i1=0;i1<checkbx.length;i1++){
            mCheckBox[i1]=findViewById(checkbx[i1]);
            mCheckBox[i1].setOnClickListener(v -> {
                int p1 = v.getId();
                getEditor().putInt(manual_tunnel_radio_key,getCheckedPosition1(p1)).apply();
                getEditor().putInt(SERVER_POSITION,0).apply();
                getEditor().putInt(NETWORK_POSITION,0).apply();
                if (p1==R.id.ovpn_protocol){
                    mCheckBox[0].setChecked(true);
                    mCheckBox[1].setChecked(false);
                    mCheckBox[2].setChecked(false);
                    mCheckBox[3].setChecked(false);
                    mCheckBox[4].setChecked(false);
                }else if (p1==R.id.udp_protocol){
                    mCheckBox[0].setChecked(false);
                    mCheckBox[1].setChecked(true);
                    mCheckBox[2].setChecked(false);
                    mCheckBox[3].setChecked(false);
                    mCheckBox[4].setChecked(false);
                }else if (p1==R.id.ssh_protocol){
                    mCheckBox[0].setChecked(false);
                    mCheckBox[1].setChecked(false);
                    mCheckBox[2].setChecked(true);
                    mCheckBox[3].setChecked(false);
                    mCheckBox[4].setChecked(false);
                }else if (p1==R.id.dns_protocol){
                    mCheckBox[0].setChecked(false);
                    mCheckBox[1].setChecked(false);
                    mCheckBox[2].setChecked(false);
                    mCheckBox[3].setChecked(true);
                    mCheckBox[4].setChecked(false);
                }else if (p1==R.id.v2ray_protocol){
                    mCheckBox[0].setChecked(false);
                    mCheckBox[1].setChecked(false);
                    mCheckBox[2].setChecked(false);
                    mCheckBox[3].setChecked(false);
                    mCheckBox[4].setChecked(true);
                }
                advanceSettings.this.finish();
            });
        }*/
        dns = findViewById(R.id.dns);
        dns.setChecked(getConfig().getVpnDnsForward());
        dns.setOnClickListener(v -> {
            if (dns.isChecked()){
                getConfig().setVpnDnsForward(true);
            }else{
                getConfig().setVpnDnsForward(false);
            }
        });
        udp = findViewById(R.id.udp);
        udp.setChecked(getConfig().getVpnUdpForward());
        udp.setOnClickListener(v -> {
            if (udp.isChecked()){
                udpDialog();
            }else{
                getConfig().setVpnUdpForward(false);
            }
        });
        findViewById(R.id.dnsforward).setOnClickListener(v -> {
            View inflate = LayoutInflater.from(advanceSettings.this).inflate(R.layout.dialog_dns, null);
            final AlertDialog cBuiler = new AlertDialog.Builder(advanceSettings.this).create();
            Spinner dnsSpinner = inflate.findViewById(R.id.preload_pay1);
            final TextInputEditText dns_primary = inflate.findViewById(R.id.custom_dns_primary);
            final TextInputEditText dns_secondary = inflate.findViewById(R.id.custom_dns_secondary);
            dnsSpinner.setSelection(getDPrefs().getInt("DIALOG_DNS_POSITION",0));
            dnsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    dns_primary.setText(getConfig().get_primary(position));
                    dns_secondary.setText(getConfig().get_secondary(position));
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            inflate.findViewById(R.id.update_dns).setOnClickListener(p1 -> {
                getDEditor().putInt("DIALOG_DNS_POSITION",dnsSpinner.getSelectedItemPosition()).apply();
                getConfig().setVpnDnsResolver(dns_primary.getText().toString()+":"+dns_secondary.getText().toString());
                cBuiler.dismiss();
            });
            cBuiler.setView(inflate);
            cBuiler.setCancelable(true);
            cBuiler.show();
        });
        findViewById(R.id.udpforwarder).setOnClickListener(v -> {
            View inflate = LayoutInflater.from(advanceSettings.this).inflate(R.layout.dialog_udp, null);
            final AlertDialog cBuiler = new AlertDialog.Builder(advanceSettings.this).create();
            final TextInputEditText ms = inflate.findViewById(R.id.custom_udpresolver);
            ms.setText(getConfig().getVpnUdpResolver());
            inflate.findViewById(R.id.update_udp).setOnClickListener(p1 -> {
                getConfig().setVpnDnsResolver(ms.getText().toString().trim());
                cBuiler.dismiss();
            });
            cBuiler.setView(inflate);
            cBuiler.setCancelable(true);
            cBuiler.show();
        });
    }

    /*private int getCheckedPosition1(int p1){
        if (p1==R.id.ovpn_protocol){
            return 0;
        }else if (p1==R.id.udp_protocol){
            return 1;
        }else if (p1==R.id.ssh_protocol){
            return 2;
        }else if (p1==R.id.dns_protocol){
            return 3;
        }else if (p1==R.id.v2ray_protocol){
            return 4;
        }
        return 0;
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.dev_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if (item.getItemId()==R.id.menu_share){
            getConfig().launchMarketDetails();
        }
        return true;
    }

    private void udpDialog(){
        View inflate = LayoutInflater.from(this).inflate(R.layout.notification_dialog, null);
        final AlertDialog cBuiler = new AlertDialog.Builder(this).create();
        ((AppCompatImageView)inflate.findViewById(R.id.notification_icon)).setImageResource(R.drawable.icon_icon);
        ((TextView)inflate.findViewById(R.id.notification_title)).setText(resString(R.string.app_name));
        ((TextView)inflate.findViewById(R.id.notification_message)).setText("A UDP Helper Address is a special router \nconfiguration used to forward broadcast network \ntraffic from a client machine on one subnet to a \nserver in another subnet.");
        TextView no = inflate.findViewById(R.id.notification_btn_no);
        TextView yes = inflate.findViewById(R.id.notification_btn_yes);
        no.setText("No");
        yes.setText("Yes");
        no.setOnClickListener(p1 -> {
            if (udp!=null){
                udp.setChecked(false);
                getConfig().setVpnUdpForward(false);
            }
            cBuiler.dismiss();
        });
        yes.setOnClickListener(p1 -> {
            getConfig().setVpnUdpForward(true);
            cBuiler.dismiss();
        });
        cBuiler.setView(inflate);
        cBuiler.setCancelable(false);
        cBuiler.show();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(advanceSettings.this, OpenVPNClient.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        advanceSettings.this.finish();
        super.onBackPressed();
    }
        
}
