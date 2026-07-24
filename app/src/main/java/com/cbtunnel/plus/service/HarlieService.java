package com.cbtunnel.plus.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.StrictMode;
import android.util.Log;
import com.cbtunnel.plus.harliesApplication;
import com.v2ray.ang.util.SpeedtestUtil;
import com.v2ray.ang.util.Utils;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.cbtunnel.plus.R;
import com.cbtunnel.plus.config.ConfigUtil;
import com.cbtunnel.plus.config.SettingsConstants;
import com.cbtunnel.plus.logger.ConnectionStatus;
import com.cbtunnel.plus.logger.hLogStatus;
import com.cbtunnel.plus.thread.SSHTunnelThread;
import com.cbtunnel.plus.thread.SocketProxyThread;
import com.cbtunnel.plus.thread.UDPTunnelThread;
import com.cbtunnel.plus.utils.util;
import com.cbtunnel.plus.view.StatisticGraphData;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import io.michaelrocks.paranoid.Obfuscate;

@Obfuscate
public class HarlieService extends Service implements Handler.Callback, SettingsConstants, hLogStatus.StateListener, hLogStatus.ByteCountListener {
    public static final String START_SERVICE = "com.cbtunnel.plus:startTunnel";
    public static final String STOP_SERVICE = "com.cbtunnel.plus:stopTunnel";
    public static final String RECONNECT_SERVICE = "com.cbtunnel.plus:reconnecTunnel";
    public static final String RESTART_SERVICE = "com.cbtunnel.plus:restartTunnel";
    private final int NOTIFICATION_ID = 123;
    public int OVPN = 0,SSH_DNS = 1,V2RAY = 2,UDP = 3,PROXY_TUNNEL = 4;
    public static final String NOTIFICATION_CHANNEL_BG_ID = "NOTIFICATION_CHANNEL_ID";
    private PowerManager.WakeLock wakeLock;
    private NotificationManager nm;
    private NotificationCompat.Builder mNotifyBuilder = null;
    public static boolean isRunning = false;
    private ConfigUtil mConfig;
    private UDPTunnelThread mUDPTunnelThread;
    public static boolean mStopping = false;
    private HarlieService.InjectorListener InjectorListener;
    private static boolean mDisplayBytecount = false;
    private SSHTunnelThread mSSHTunnelThread;
    private SharedPreferences mPref;
    public Handler mHandler;
    private static String mServer_type;
    private static String mStateReceiver;
    public static boolean isVPNRunning(){
        return isRunning&&hLogStatus.isTunnelActive();
    }
    public interface InjectorListener {
        void startOpenVPN();
    }

    public void setInjectorListener(InjectorListener InjectorListener) {
        this.InjectorListener = InjectorListener;
    }

    @Override
    public IBinder onBind(Intent p1) {
        return new MyBinder();
    }

    public class MyBinder extends Binder {
        public HarlieService getService() {
            return HarlieService.this;
        }
    }

    @SuppressLint("WakelockTimeout")
    private void setWakelock() {
        try {
            this.wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(1, "SocksIP::Tag");
            this.wakeLock.acquire();
        } catch (Exception e) {
            Log.d("WAKELOCK", e.toString());
        }
    }

    private void unsetWakelock() {
        try {
            if (this.wakeLock != null && this.wakeLock.isHeld()) {
                Log.e("WAKELOCK", "is disabled");
                this.wakeLock.release();
            }
        } catch (Exception e) {
            Log.d("WAKELOCK", e.toString());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);
        mHandler = new Handler(HarlieService.this);
        mPref = harliesApplication.getPrivateSharedPreferences();
        mConfig = ConfigUtil.getInstance(HarlieService.this);
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        Bundle bundle = intent.getExtras();
        if (action != null) {
            mConfig = ConfigUtil.getInstance(HarlieService.this);
            mServer_type = mConfig.getServerType();
            switch (action) {
                case START_SERVICE:
                    mStateReceiver = "";
                    hLogStatus.addStateListener(HarlieService.this);
                    hLogStatus.addByteCountListener(HarlieService.this);
                    start_notification();
                    isRunning = true;
                    mStopping = false;
                    mConfig.initializeStartingMsg();
                    setWakelock();
                    startRun();
                    break;
                case STOP_SERVICE:
                    if (bundle!=null){
                        mStateReceiver = bundle.getString("stateSTOP_SERVICE");
                    }
                    closeAll();
                    break;
                case RECONNECT_SERVICE:
                    if (bundle!=null){
                        mStateReceiver = bundle.getString("mStateReceiver");
                        network_reconnect();
                        break;
                    }
                    if (util.isNetworkAvailable(HarlieService.this)){
                        if (isVPNRunning()){
                            hLogStatus.clearLog();
                            hLogStatus.updateStateString(hLogStatus.VPN_RECONNECTING, getResources().getString(R.string.state_reconnecting));
                        }
                        if (mServer_type.equals(SERVER_TYPE_OVPN) && isVPNRunning()) {
                            startService(new Intent(HarlieService.this, OpenVPNService.class).setAction(OpenVPNService.ACTION_RECONNECT));
                            break;
                        }
                        mStateReceiver = getResources().getString(R.string.state_reconnecting);
                        network_reconnect();
                    }
                    break;
                case "START_PINGER":
                    startPingStatus();
                    break;
                case "CONNECTION_TEST_FAILD":
                    util.showToast(getResources().getString(R.string.app_name), getResources().getString(R.string.connection_test_fail));
                    break;
                case RESTART_SERVICE:
                    startService(new Intent(HarlieService.this, OpenVPNService.class).setAction(OpenVPNService.ACTION_DISCONNECT).putExtra(OpenVPNService.INTENT_PREFIX + ".STOP", true));
                    addLogInfo(getResources().getString(R.string.state_reconnecting));
                    mHandler.sendEmptyMessage(PROXY_TUNNEL);
                    break;
            }
        }
        return START_STICKY;
    }


    private void closeAll() {
        if (isVPNRunning()) {
            addLogInfo(String.format("<b>%s %s</b>", getResources().getString(R.string.app_name),getResources().getString(R.string.state_stopping)));
        }
        isRunning = false;
        mStopping = true;
        unsetWakelock();
        StatisticGraphData.getStatisticData().getDataTransferStats().stop();
        VPNTunnel_handler(false);
        Utils.stopV2RayVService(HarlieService.this);
        startService(new Intent(HarlieService.this, OpenVPNService.class).setAction(OpenVPNService.ACTION_DISCONNECT).putExtra(OpenVPNService.INTENT_PREFIX + ".STOP", true));
        new Thread(HarlieService.this::stopAll).start();
        if (mStateReceiver.equals(getResources().getString(R.string.state_auth_failed))){
            hLogStatus.updateStateString(hLogStatus.VPN_AUTH_FAILED, getResources().getString(R.string.state_auth_failed));
        } else {
            hLogStatus.updateStateString(hLogStatus.VPN_DISCONNECTED, getResources().getString(R.string.state_disconnected));
        }
        endTunnelService();
    }


    private void startRun() {
        hLogStatus.updateStateString(hLogStatus.VPN_CONNECTING, getResources().getString(R.string.state_connecting));
        while (isVPNRunning()){
            if (mServer_type.equals(SERVER_TYPE_SSH) && mConfig.getPayloadType()==PAYLOAD_TYPE_HTTP_PROXY || mServer_type.equals(SERVER_TYPE_SSH) && mConfig.getPayloadType()==PAYLOAD_TYPE_SSL) {
                addLogInfo(getResources().getString(R.string.state_connecting));
                mHandler.sendEmptyMessage(PROXY_TUNNEL);
                break;
            } else if (mServer_type.equals(SERVER_TYPE_DNS) || mServer_type.equals(SERVER_TYPE_SSH)) {
                addLogInfo(getResources().getString(R.string.state_connecting));
                mHandler.sendEmptyMessage(SSH_DNS);
                break;
            } else if (mServer_type.equals(SERVER_TYPE_UDP_HYSTERIA_V1)) {
                addLogInfo(getResources().getString(R.string.state_connecting));
                addLogInfo(getResources().getString(R.string.state_get_config));
                addLogInfo(getResources().getString(R.string.state_assign_ip));
                addLogInfo(getResources().getString(R.string.state_wait));
                mHandler.sendEmptyMessage(UDP);
                break;
            } else if (mServer_type.equals(SERVER_TYPE_V2RAY)){
                addLogInfo(getResources().getString(R.string.state_connecting));
                addLogInfo(getResources().getString(R.string.state_get_config));
                addLogInfo(getResources().getString(R.string.state_assign_ip));
                addLogInfo(getResources().getString(R.string.state_wait));
                mHandler.sendEmptyMessage(V2RAY);
                break;
            } else {
                mHandler.sendEmptyMessage(mConfig.mUseProxy()? PROXY_TUNNEL:OVPN);
                break;
            }
        }
    }


    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (msg.what==OVPN){
            InjectorListener.startOpenVPN();
            return false;
        } else if (msg.what==SSH_DNS){
            startSSHTunnel();
            return false;
        } else if (msg.what==V2RAY){
            Utils.startV2RayService(HarlieService.this);
            return false;
        } else if (msg.what==UDP){
            startUDPTunnel();
            return false;
        } else if (msg.what==PROXY_TUNNEL){
            starSocketProxy();
            return false;
        }
        return true;
    }

    private void network_reconnect(){
        if (mStopping || !isVPNRunning()) {
            return;
        }
        VPNTunnel_handler(false);
        Utils.stopV2RayVService(HarlieService.this);
        if (!util.isNetworkAvailable(HarlieService.this)){
            hLogStatus.updateStateString(hLogStatus.VPN_PAUSE, getResources().getString(R.string.state_pause));
            addLogInfo(getResources().getString(R.string.state_pause));
        }else if (util.isNetworkAvailable(HarlieService.this) && mConfig.getIsScreenOn()){
            if (mStateReceiver.equals(getResources().getString(R.string.state_pause))){
                hLogStatus.updateStateString(hLogStatus.VPN_PAUSE, getResources().getString(R.string.state_pause));
            }
            if (mStateReceiver.equals(getResources().getString(R.string.state_resume))) {
                hLogStatus.updateStateString(hLogStatus.VPN_RESUME, getResources().getString(R.string.state_resume));
            }
            if (mStateReceiver.equals(getResources().getString(R.string.state_reconnecting))) {
                hLogStatus.updateStateString(hLogStatus.VPN_RECONNECTING, getResources().getString(R.string.state_reconnecting));
            }
            addLogInfo(mStateReceiver.isEmpty()? getResources().getString(R.string.state_reconnecting):mStateReceiver);
            try {
                switch (mServer_type){
                    case SERVER_TYPE_UDP_HYSTERIA_V1:
                        addLogInfo(getResources().getString(R.string.state_get_config));
                        addLogInfo(getResources().getString(R.string.state_assign_ip));
                        addLogInfo(getResources().getString(R.string.state_wait));
                        mHandler.sendEmptyMessage(UDP);
                        break;
                    case SERVER_TYPE_V2RAY:
                        addLogInfo(getResources().getString(R.string.state_get_config));
                        addLogInfo(getResources().getString(R.string.state_assign_ip));
                        addLogInfo(getResources().getString(R.string.state_wait));
                        mHandler.sendEmptyMessage(V2RAY);
                        break;
                    case SERVER_TYPE_DNS:
                    case SERVER_TYPE_SSH:
                        if (mSSHTunnelThread!=null){
                            mSSHTunnelThread.reconnectSSH();
                        }
                        break;
                }
            } catch (Exception e) {
                closeAll();
            }
        }
    }


    private void stopAll() {
         try {
             if (thPing != null) {
                 thPing.interrupt();
                 thPing = null;
             }
        } catch (Exception ignored) {
        }
         try {
             if (mSSHTunnelThread != null) {
                 mSSHTunnelThread.interrupt();
                 mSSHTunnelThread = null;
             }
        } catch (Exception ignored) {
        }
         try {
             if (mUDPTunnelThread != null) {
                 mUDPTunnelThread.interrupt();
                 mUDPTunnelThread = null;
             }
        } catch (Exception ignored) {
        }
         try {
             if (mSocketProxyThread != null) {
                 mSocketProxyThread.interrupt();
                 mSocketProxyThread = null;
             }
        } catch (Exception ignored) {
        }
    }


    private void startPingStatus() {
        if (isVPNRunning()){
            mPref.edit().putBoolean("TIMEOUT_TRIES_KEY",true).apply();
            try {
                startPinger();
            } catch (Exception e) {
                hLogStatus.logInfo("startPinger error:"+e.getMessage());
            }
        }
    }
    private Thread thPing;
    private int TIMEOUT_TRIES = 0;
    private void startPinger() throws Exception {
        TIMEOUT_TRIES = 0;
        int timePing = mConfig.getPingThread();
        if (!isVPNRunning()) {
            throw new Exception();
        }
        hLogStatus.logInfo("starting pinger");
        thPing = new Thread() {
            @Override
            public void run() {
                while (isVPNRunning()) {
                    TIMEOUT_TRIES++;
                    if (mPref.getBoolean("TIMEOUT_TRIES_KEY", false) && TIMEOUT_TRIES==Integer.parseInt(mPref.getString("ping_timeout","10"))){
                        hLogStatus.logInfo("<font color = #FF9600>Ping timeout");
                        thPing.interrupt();
                        thPing = null;
                        break;
                    }
                    try {
                        makePinger();
                    } catch(InterruptedException e) {
                        break;
                    }
                }
                TIMEOUT_TRIES = 0;
                mPref.edit().putBoolean("TIMEOUT_TRIES_KEY",true).apply();
                hLogStatus.logInfo("pinger stopped");
            }
            private synchronized void makePinger() throws InterruptedException {
                try {
                    String newPing;
                    long ping = SpeedtestUtil.getPing(mPref.getString("ping_destination", "www.google.com"), String.valueOf(mConfig.getPingThread()));
                    if (ping >= 400 || ping == 0 || ping == 1 || ping == -1){
                        newPing = "Ping status (<font color = #BA000F>"+ping+"ms</font>)";
                    }else {
                        newPing = "Ping status (<font color = #68B86B>"+ping+"ms</font>)";
                    }
                    mPref.edit().putBoolean("TIMEOUT_TRIES_KEY",false).apply();
                    hLogStatus.logInfo(newPing);
                } catch(Exception e) {
                    Log.e("makePinger", "ping error", e);
                }
                if (timePing == 0)
                    return;
                if (timePing > 0)
                    sleep(timePing*1000);
                else {
                    hLogStatus.logInfo("ping invalid");
                    throw new InterruptedException();
                }
            }
        };
        thPing.start();
    }


    private SocketProxyThread mSocketProxyThread;
    private void starSocketProxy() {
        if (mSocketProxyThread != null) {
            mSocketProxyThread.interrupt();
            mSocketProxyThread = null;
        }
        mSocketProxyThread = new SocketProxyThread(HarlieService.this, () -> closeAll());
        mSocketProxyThread.start();
    }

    private void startSSHTunnel() {
        if (mSSHTunnelThread != null) {
            mSSHTunnelThread.interrupt();
            mSSHTunnelThread = null;
        }
        mSSHTunnelThread = new SSHTunnelThread(HarlieService.this, this::closeAll);
        mSSHTunnelThread.start();
    }


    private void startUDPTunnel() {
        if (mUDPTunnelThread != null) {
            mUDPTunnelThread.interrupt();
            mUDPTunnelThread = null;
        }
        mUDPTunnelThread = new UDPTunnelThread(this, new UDPTunnelThread.OnUDPListener() {
            @Override
            public void onReconnect() {
                network_reconnect();
            }
            @Override
            public void onStop() {
                closeAll();
            }
        });
        mUDPTunnelThread.start();
    }

    public void VPNTunnel_handler(boolean on) {
        try {
            Intent intent = new Intent(HarlieService.this, VPNTunnelService.class);
            if (on) {
                startService(intent.setAction(VPNTunnelService.START_VPN_SERVICE));
            } else {
                startService(intent.setAction(VPNTunnelService.STOP_VPN_SERVICE));
            }
        } catch (Exception e) {
            addLogInfo("<font color = #d50000>Something wen't wrong in VPNService.");
        }
    }

    @Override
    @SuppressLint("StringFormatMatches")
    public void updateByteCount(long in, long out, long diffIn, long diffOut) {
        if (mDisplayBytecount) {
            String netstat = String.format(getResources().getString(R.string.statusline_bytecount,
                    ConfigUtil.render_bandwidth(in, false),
                    ConfigUtil.render_bandwidth(diffIn, true),
                    ConfigUtil.render_bandwidth(out, false),
                    ConfigUtil.render_bandwidth(diffOut, true)));
            update_notification_event(netstat, ConnectionStatus.LEVEL_CONNECTED);
        }
    }

    @Override
    public void updateState(String state, String logMessage, int localizedResId, ConnectionStatus level) {
        String stateMsg = getString(hLogStatus.getLocalizedState(hLogStatus.getLastState()));
        mDisplayBytecount = (level.equals(ConnectionStatus.LEVEL_CONNECTED));
        update_notification_event(stateMsg, level);
    }


     private String getConnection_name(){
          if (mServer_type.equals(SERVER_TYPE_V2RAY)){
              return mConfig.getServerName() + " • " + "V2Ray/Xray";          
          }      
          return mConfig.getServerName() + " • " + mConfig.getPayloadName();
    }   
        

    private void start_notification() {
        mNotifyBuilder = new NotificationCompat.Builder(HarlieService.this, NOTIFICATION_CHANNEL_BG_ID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_BG_ID, getResources().getString(R.string.channel_name_background), importance);
            notificationChannel.setDescription(getResources().getString(R.string.channel_description_background));
            notificationChannel.enableVibration(true);
            notificationChannel.setLightColor(Color.CYAN);
            nm.createNotificationChannel(notificationChannel);
            mNotifyBuilder.setChannelId(NOTIFICATION_CHANNEL_BG_ID);
        }
        mNotifyBuilder.setContentIntent(ConfigUtil.getPendingIntent(this)).
                setSmallIcon(R.drawable.cloud_off).
                setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon_icon)).
                setContentTitle(getConnection_name()).
                setContentText("Status: " + getResources().getString(R.string.state_connecting)).
                setOnlyAlertOnce(true).
                setOngoing(true).
                setWhen(new Date().getTime()).
                setPriority(NotificationCompat.PRIORITY_DEFAULT);
        addVpnActionsToNotification(mNotifyBuilder);
        jbNotificationExtras(mNotifyBuilder);
        lpNotificationExtras(mNotifyBuilder);
        nm.notify(NOTIFICATION_ID, mNotifyBuilder.build());
        startForeground(NOTIFICATION_ID, mNotifyBuilder.getNotification());
    }

    @SuppressLint("WrongConstant")
    private void addVpnActionsToNotification(NotificationCompat.Builder nbuilder) {
        final int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 268435456;
        PendingIntent reconnectSSHService = PendingIntent.getService(HarlieService.this, 0, new Intent(HarlieService.this, HarlieService.class).setAction(RECONNECT_SERVICE),flags);
        PendingIntent disconnectSSHService = PendingIntent.getService(HarlieService.this, 0, new Intent(HarlieService.this, HarlieService.class).setAction(STOP_SERVICE),flags);
        nbuilder.addAction(R.drawable.cloud_on, "Reconnect", reconnectSSHService);
        nbuilder.addAction(R.drawable.cloud_off, "Disconnect", disconnectSSHService);
    }

    private void update_notification_event(String str, ConnectionStatus status){
        int icon = getIconByConnectionStatus(status);
        if (mNotifyBuilder != null) {
            if(status.equals(ConnectionStatus.LEVEL_CONNECTED)){
                mNotifyBuilder.setTicker(getResources().getString(R.string.state_connected));
            }
            mNotifyBuilder.setSmallIcon(icon);
            mNotifyBuilder.setContentTitle(getConnection_name());
            mNotifyBuilder.setContentText((mDisplayBytecount)?str:"Status: "+str);
            nm.notify(NOTIFICATION_ID, mNotifyBuilder.build());
            startForeground(NOTIFICATION_ID, mNotifyBuilder.getNotification());
        }
    }

    private void lpNotificationExtras(NotificationCompat.Builder nbuilder) {
        nbuilder.setCategory(Notification.CATEGORY_SERVICE);
        nbuilder.setLocalOnly(true);
    }

    private void jbNotificationExtras(NotificationCompat.Builder nbuilder) {
        try {
            if (NotificationManager.IMPORTANCE_LOW != 0) {
                Method setpriority = nbuilder.getClass().getMethod("setPriority", int.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setpriority.invoke(nbuilder, NotificationManager.IMPORTANCE_LOW);
                }
                Method setUsesChronometer = nbuilder.getClass().getMethod("setUsesChronometer", boolean.class);
                setUsesChronometer.invoke(nbuilder, true);
            }
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException ignored) {
        }
    }

    private void endTunnelService(){
        new Thread(() -> {
            stopForeground(true);
            nm.cancel(NOTIFICATION_ID);
            hLogStatus.removeStateListener(HarlieService.this);
            hLogStatus.removeByteCountListener(HarlieService.this);
        }).start();
    }

    private int getIconByConnectionStatus(ConnectionStatus level) {
        if (level.equals(ConnectionStatus.LEVEL_CONNECTED)) {
            return R.drawable.cloud_on;
        }
        return R.drawable.cloud_off;
    }


    public void addLogInfo(String msg){
        String hst = mConfig.getSecureString(SERVER_KEY);
        String prx = mConfig.getSecureString(PROXY_IP_KEY);
        if (!msg.contains("Socket close")||!msg.contains(hst)||!msg.contains(prx)){
            hLogStatus.logInfo(msg.trim());
        }
    }



}
