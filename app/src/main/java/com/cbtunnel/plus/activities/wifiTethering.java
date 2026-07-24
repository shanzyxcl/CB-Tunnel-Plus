package com.cbtunnel.plus.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import com.cbtunnel.plus.R;
import com.cbtunnel.plus.service.ProxyService;

public class wifiTethering extends OpenVPNClientBase {
        
    private EditText portEditText;
    private Button start, stop, restart, hdwifi;
    private ImageView wifiTetherButton;
    private TextView proxyStatusTextView, proxyURLTextView;
    private SharedPreferences sp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        getWindow().setStatusBarColor(getConfig().getColorAccent());
        sp = getSharedPreferences("Wifi_Tethering", Context.MODE_PRIVATE);
        setContentView(R.layout.activity_wifi);
        Toolbar mToolbar = findViewById(R.id.toolbar_main);
        mToolbar.setTitle(resString(R.string.app_name));
        mToolbar.setSubtitle("\"Wifi Tethering");
        mToolbar.setBackgroundColor(getConfig().getColorAccent());
        mToolbar.setTitleTextColor(getConfig().getAppThemeUtil()? Color.BLACK:Color.WHITE);
        mToolbar.setSubtitleTextColor(getConfig().getAppThemeUtil()? Color.BLACK:Color.WHITE);
        mToolbar.setNavigationIcon(getConfig().getAppThemeUtil()? R.drawable.arrow_d:R.drawable.arrow_l);
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationOnClickListener(v -> {
            stop();
            finish();
        });
        initializeViews();
        initializeListeners();
    }

    private void initializeListeners() {
        wifiTetherButton.setOnClickListener(v -> launchHotspotSettings());
        restart.setOnClickListener(v -> restartapp());
        hdwifi.setOnClickListener(v -> showDialog(String.valueOf(Html.fromHtml("Cách Phát Wifi Qua Proxy")),String.valueOf(Html.fromHtml("</strong> " + "1. Kết Nối VPN ,Chọn Mục Phát Wifi ( Proxy ) Trên Ứng Dụng<br>2. Kích Hoạt Phát Wifi Trên Máy Hoặc Điểm Phát Sóng <br>3. Bạn Nhập Cổng Như Sau :<br>Đối Với AZZPHUC PRO ( SSH) Là <font color=#f70217>1080 , 8080</font> \nĐối Với V2Ray , V2FlyNG Là <font color=#f70217>10809</font><br>4. Bấm Bắt Đầu , Yêu Cầu Đã Kết Nối 4G VPN , Đã Bật Phát Wifi Bạn Sẽ Thấy Dòng <font color=#f70217>192.168.xx.x</font>: Cổng Đã Nhập)<br>5. Trên Máy Bắt Các Bạn Kết Nối Wifi Đó , Chọn Mục Proxy ( Ở Trạng Thái Không Có ) , Chọn Thủ Công<br>6. Nhập IP <font color=#f70217>192.x.x.x</font> ( Tên Máy Chủ , Server) , Nhập Cổng Sau Đó Lưu Và Kết Nối Lại Wifi<br>7. Nếu <font color=#f70217>Lỗi</font> , Cổng Bận Bạn Hãy Buộc Dừng App . Chúc Các Bạn Thành Công" + "</strong>"))));
        start.setOnClickListener(v -> {
            start();
        });
        stop.setOnClickListener(v -> stop());
    }

    private void showDialog(String t,final String message){
        View inflate = LayoutInflater.from(this).inflate(R.layout.notif2, null);
        final AlertDialog cBuiler = new AlertDialog.Builder(this).create();
        RelativeLayout btn = inflate.findViewById(R.id.appButton2);
        TextView title = inflate.findViewById(R.id.notiftext1);
        TextView ms = inflate.findViewById(R.id.confimsg);
        TextView cancel = inflate.findViewById(R.id.appButton2txt);
        ms.setTextColor(getConfig().gettextColor());
        inflate.findViewById(R.id.color_bg).setBackgroundColor(getConfig().getColorAccent());
        btn.setBackgroundTintList(ColorStateList.valueOf(getConfig().getColorAccent()));
        title.setText(t);
        cancel.setText("OKA'Y");
        inflate.findViewById(R.id.appButton1).setVisibility(View.GONE);
        btn.setOnClickListener(p1 -> cBuiler.dismiss());
        ms.setText(Html.fromHtml(message.replace("\n", "<br/>")));
        cBuiler.setView(inflate);
        cBuiler.setCancelable(false);
        cBuiler.getWindow().getAttributes().windowAnimations = R.style.alertDialog;
        cBuiler.show();
    }

    private void start() {
        if (!portEditText.getText().toString().matches("\\d+")) {
            proxyStatusTextView.setText("Enter port (eg: 8080);");
            proxyURLTextView.setText("");
            return;
        }
        int port = Integer.parseInt(portEditText.getText().toString());
        String ip = getIPAddress(true);
        if (!ip.trim().startsWith("192.")) {
            launchHotspotSettings();
            return;
        }
        try {
            if (!(new CheckingPortTask().execute(port).get())) {
                restart.setVisibility(View.GONE);
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            proxyStatusTextView.setText("There are a few bugs");
            proxyURLTextView.setText("");
            return;
        }
        Intent intent = new Intent(wifiTethering.this, ProxyService.class);
        intent.putExtra("port", port);
        sp.edit().putString("port", portEditText.getText().toString()).apply();
        startService(intent);
        proxyStatusTextView.setText("Proxy is running on:");
        proxyURLTextView.setText(String.format("%s:%d", getIPAddress(true), port));
        start.setVisibility(View.GONE);
        stop.setVisibility(View.VISIBLE);
        portEditText.setEnabled(false);

    }

    private void stop() {
        stopService(new Intent(wifiTethering.this, ProxyService.class));
        proxyStatusTextView.setText("Stopped");
        proxyURLTextView.setText("");
        if (ProxyService.isRunning){
            start.setVisibility(View.VISIBLE);
            stop.setVisibility(View.GONE);
            portEditText.setEnabled(false);
        } else {
            start.setVisibility(View.VISIBLE);
            stop.setVisibility(View.GONE);
        }
        portEditText.setEnabled(true);
    }

    private void initializeViews() {
        portEditText = findViewById(R.id.portEditText);
        start = findViewById(R.id.start);
        stop = findViewById(R.id.stop);
        wifiTetherButton = findViewById(R.id.WiFiTetherButton);
        proxyStatusTextView = findViewById(R.id.proxyStatus);
        proxyURLTextView = findViewById(R.id.proxyURL);
        hdwifi = findViewById(R.id.hdwifi);
        restart = findViewById(R.id.restart);
        wifiTetherButton.setColorFilter(getConfig().getAppThemeUtil()? Color.WHITE : Color.BLACK, PorterDuff.Mode.SRC_IN);
        if (ProxyService.isRunning){
            start.setVisibility(View.GONE);
            stop.setVisibility(View.VISIBLE);
            portEditText.setEnabled(false);
        } else {
            start.setVisibility(View.VISIBLE);
            stop.setVisibility(View.GONE);
        }
        portEditText.setText(sp.getString("port", "8080"));
        findViewById(R.id.start_bg).setBackgroundTintList(ColorStateList.valueOf(getConfig().getColorAccent()));
        findViewById(R.id.stop_bg).setBackgroundTintList(ColorStateList.valueOf(getConfig().getColorAccent()));
        hdwifi.setBackgroundTintList(ColorStateList.valueOf(getConfig().getColorAccent()));
        restart.setBackgroundTintList(ColorStateList.valueOf(getConfig().getColorAccent()));
    }

    public void restartapp() {
        recreate();        
    }


    public String getIPAddress(boolean useIPv4) {
        try {
            boolean isIPv4;
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        isIPv4 = sAddr.indexOf(':') < 0;

                        if (useIPv4) {
                            if (isIPv4) {
                                return sAddr;
                            }
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%');
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        } // for now eat exceptions
        return "";
    }

    private void launchHotspotSettings() {
        Intent tetherSettings = new Intent();
        tetherSettings.setClassName("com.android.settings", "com.android.settings.TetherSettings");
        startActivity(tetherSettings);
    }

    private static class CheckingPortTask extends AsyncTask<Integer, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Integer... port) {
            try {
                ServerSocket serverSocket = new ServerSocket(port[0]);
                serverSocket.close();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }


}
