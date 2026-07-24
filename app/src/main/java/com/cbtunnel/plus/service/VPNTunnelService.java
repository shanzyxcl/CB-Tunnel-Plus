package com.cbtunnel.plus.service;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import com.trilead.ssh2.transport.TransportManager;
import com.cbtunnel.plus.R;
import com.cbtunnel.plus.config.ConfigUtil;
import com.cbtunnel.plus.config.SettingsConstants;
import com.cbtunnel.plus.core.CIDRIP;
import com.cbtunnel.plus.core.NetworkSpace;
import com.cbtunnel.plus.core.vpnutils.Pdnsd;
import com.cbtunnel.plus.core.vpnutils.Tun2Socks;
import com.cbtunnel.plus.core.vpnutils.VpnUtils;
import com.cbtunnel.plus.logger.hLogStatus;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class VPNTunnelService extends VpnService implements SettingsConstants {
    public static final String START_VPN_SERVICE = "com.cbtunnel.plus.service.START_VPN_SERVICE";
    public static final String STOP_VPN_SERVICE = "com.cbtunnel.plus.service.STOP_VPN_SERVICE";
    private ConfigUtil mConfig;
    private int mMtu = 1500;
    private Thread mBuilderThread;
    private Pdnsd mPdnsd;
    private static final String VPN_INTERFACE_NETMASK = "255.255.255.0";
    private Tun2Socks mTun2Socks;
    private static final int DNS_RESOLVER_PORT = 53;
    public final AtomicBoolean isServiceRunning = new AtomicBoolean(false);
    private final IBinder mBinder = new LocalBinder();
    private VpnUtils.PrivateAddress mPrivateAddress;
    private AtomicReference<ParcelFileDescriptor> mTunFd;
    private AtomicBoolean mRoutingThroughTunnel;
    private NetworkSpace mRoutes;

    public class LocalBinder extends Binder {
        public VPNTunnelService getService() {
            return VPNTunnelService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(START_VPN_SERVICE)) return mBinder;
        else return super.onBind(intent);
    }

    public void onCreate() {
        super.onCreate();
        StrictMode.ThreadPolicy.Builder builder = new StrictMode.ThreadPolicy.Builder();
        StrictMode.setThreadPolicy(builder.permitAll().build());
        mConfig = ConfigUtil.getInstance(this);
        mTunFd = new AtomicReference<>();
        mRoutingThroughTunnel = new AtomicBoolean(false);
        mRoutes = new NetworkSpace();
    }

    public int onStartCommand(Intent intent, int flags, int startId)  {
        if (intent == null || intent.getAction() == null) {
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case START_VPN_SERVICE:
                    addLogInfo("Starting VPN Service");
                    submit_establish_builder();
                    break;
                case STOP_VPN_SERVICE:
                    submit_destroy_builder();
                    break;
            }
        }
        return START_STICKY;
    }

    private void submit_establish_builder(){
        if (mBuilderThread != null) {
            mBuilderThread.interrupt();
            mBuilderThread = null;
        }
        boolean enableTethering = mConfig.getIsTetheringSubnet();
        boolean dnsByPass = mConfig.getServerType().equals(SERVER_TYPE_DNS);
        boolean enabledFilter = mConfig.getIsFilterApps();
        boolean filterBypassMode = mConfig.getIsFilterBypassMode();
        boolean forwardDns = mConfig.getVpnDnsForward();
        String[] dnsResolver = mDnsResolvers();
        String excludeIps = excludeIps();
        Set<String> filterApps = mConfig.getPackageFilterApps();
        mBuilderThread = new Thread(() -> {
            try{
                startVpn(forwardDns,dnsResolver,excludeIps,enabledFilter,filterBypassMode,filterApps,enableTethering,dnsByPass);
            }catch (Exception ex){
                destroyAll();
            }
        });
        mBuilderThread.start();
    }

    private void destroyAll(){
        if (isServiceRunning.get())addLogInfo("application is not prepared or revoked");
        submit_destroy_builder();
        if(hLogStatus.isTunnelActive())startService(new Intent(VPNTunnelService.this, HarlieService.class).setAction(HarlieService.STOP_SERVICE).putExtra("stateSTOP_SERVICE", getResources().getString(R.string.state_disconnected)));
    }

    private void submit_destroy_builder(){
        new Thread(() -> {
            boolean isV2RAY = mConfig.getServerType().equals(SERVER_TYPE_V2RAY);
            try {
                if (mTun2Socks != null) {
                    mTun2Socks.interrupt();
                    mTun2Socks = null;
                }
            }catch (Exception ignored){}
            try {
                if (mPdnsd != null) {
                    mPdnsd.interrupt();
                    mPdnsd = null;
                }
            }catch (Exception ignored){}
            try {
                ParcelFileDescriptor tunFd = mTunFd.getAndSet(null);
                if (tunFd != null) {
                    tunFd.close();
                }
            }catch (Exception ignored){}
            try {
                if (mBuilderThread != null) {
                    mBuilderThread.interrupt();
                    mBuilderThread = null;
                }
            }catch (Exception ignored){}
            if (isServiceRunning.get() && !isV2RAY){
                addLogInfo("<b>VPNService stopped</b>");
            }
            isServiceRunning.set(false);
        }).start();
    }


    private String excludeIps() {
        try {
            if (mConfig.getServerType().equals(SERVER_TYPE_UDP_HYSTERIA_V1)) {
                return mConfig.getSecureString(SERVER_KEY);
            }
            if (mConfig.getServerType().equals(SERVER_TYPE_DNS)) {
                return null;
            }
            String serverIP = mConfig.getSecureString(SERVER_KEY);
            if (mConfig.getPayloadType()==PAYLOAD_TYPE_HTTP_PROXY) {
                serverIP = mConfig.getSecureString(PROXY_IP_KEY);
            }
            if (mConfig.getServerType().equals(SERVER_TYPE_OVPN) && !mConfig.getSocketTYPE().equals("cf")){
                serverIP = mConfig.getSecureString(SERVER_KEY);
            }
            InetAddress addr = TransportManager.createInetAddress(serverIP);
            return addr.getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }


    private String[] mDnsResolvers(){
        boolean forwardDns = mConfig.getVpnDnsForward();
        if (forwardDns)
            return mConfig.getVpnDnsResolver();
        else{
            List<String> lista = VpnUtils.getNetworkDnsServer(this);
            return new String[]{lista.get(0)};
        }
    }


    @SuppressLint("ObsoleteSdkInt")
    private void startVpn(boolean forwardDns, String[] dnsResolver, String excludeIps, boolean enabledFilter, boolean filterBypassMode, Set<String> filterApps, boolean enableTethering, boolean isSlowDNS) throws Exception {
        String socksServerAddress = "127.0.0.1:"+mConfig.getLocalPort();
        String udpResolver = (this.mConfig.getVpnUdpForward())?this.mConfig.getVpnUdpResolver():null;
        boolean mUdpDnsRelay = (forwardDns && udpResolver == null || !forwardDns && udpResolver != null);
        StringBuilder routeMessage = new StringBuilder("Routes: ");
        StringBuilder routeExcludeMessage = new StringBuilder("Routes Excluded: ");

        mPrivateAddress = VpnUtils.selectPrivateAddress();

        if (excludeIps!=null){
            mRoutes.addIP(new CIDRIP(excludeIps, 32), false);
        }

        Locale previousLocale = Locale.getDefault();

        final String errorMessage = "startVpn failed";
        try {
            Locale.setDefault(new Locale("en"));

            ParcelFileDescriptor tunFd;

            Builder builder = new Builder().addAddress(mPrivateAddress.mIpAddress, mPrivateAddress.mPrefixLength);
            mRoutes.addIP(new CIDRIP("0.0.0.0", 0), true);
            mRoutes.addIP(new CIDRIP("10.0.0.0", 8), false);
            mRoutes.addIP(new CIDRIP(mPrivateAddress.mSubnet, mPrivateAddress.mPrefixLength), false);

            if (enableTethering) {
                mRoutes.addIP(new CIDRIP("192.168.42.0", 23), false);
                mRoutes.addIP(new CIDRIP("192.168.44.0", 24), false);
                mRoutes.addIP(new CIDRIP("192.168.49.0", 24), false);
            }

            for (String dns : dnsResolver) {
                try {
                    builder.addDnsServer(dns);
                    mRoutes.addIP(new CIDRIP(dns, 32), forwardDns);
                } catch (IllegalArgumentException iae) {
                    addLogInfo(String.format("Error Adding dns %s, %s", dns, iae.getLocalizedMessage()));
                }
            }

            String release = Build.VERSION.RELEASE;
            if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT && !release.startsWith("4.4.3")
                    && !release.startsWith("4.4.4") && !release.startsWith("4.4.5") && !release.startsWith("4.4.6"))
                    && mMtu < 1280) {
                addLogInfo(String.format(Locale.US, "Forcing MTU to 1280 instead of %d to workaround Android Bug #70916", mMtu));
                mMtu = 1280;
            }
            builder.setMtu(mMtu);

            Collection<NetworkSpace.ipAddress> include_routes = mRoutes.getNetworks(true);
            for (NetworkSpace.ipAddress ip : include_routes) {
                routeMessage.append(String.format("%s/%s", ip.getIPv4Address(), ip.networkMask));
                routeMessage.append(", ");
            }
            routeMessage.deleteCharAt(routeMessage.lastIndexOf(", "));

            Collection<NetworkSpace.ipAddress> exclude_routes = mRoutes.getNetworks(false);
            for (NetworkSpace.ipAddress ip : exclude_routes) {
                routeExcludeMessage.append(String.format("%s/%s", ip.getIPv4Address(), ip.networkMask));
                routeExcludeMessage.append(", ");
            }
            routeExcludeMessage.deleteCharAt(routeExcludeMessage.lastIndexOf(", "));

            addLogInfo(routeMessage.toString());
            if (excludeIps!=null)
                addLogInfo(routeExcludeMessage.toString().replace(excludeIps, "******"));
            else
                addLogInfo(routeExcludeMessage.toString());

            NetworkSpace.ipAddress multicastRange = new NetworkSpace.ipAddress(new CIDRIP("224.0.0.0", 3), true);

            for (NetworkSpace.ipAddress route : mRoutes.getPositiveIPList()) {
                try {
                    if (multicastRange.containsNet(route))
                        addLogInfo("VPN: Ignoring multicast route: " + route);
                    else
                        builder.addRoute(route.getIPv4Address(), route.networkMask);
                } catch (IllegalArgumentException ia) {
                    addLogInfo("Route rejected: " + route + " " + ia.getLocalizedMessage());
                }
            }

            if (isSlowDNS) {
                builder.addDisallowedApplication(getPackageName());
            }
            if (enabledFilter) {
                for (String app_pacote : filterApps) {
                    try {
                        if (filterBypassMode) {
                            builder.addDisallowedApplication(app_pacote);
                            addLogInfo(String.format("VPN disabled for<font color = #FF9600> %s", app_pacote));
                        }
                        else {
                            builder.addAllowedApplication(app_pacote);
                            addLogInfo(String.format("VPN enabled for<font color = #FF9600> %s", app_pacote));
                        }
                    } catch(PackageManager.NameNotFoundException e) {
                        addLogInfo("App " + app_pacote + " not found. Apps filter will not work, check settings.");
                    }
                }
            }

            tunFd = builder
                    .setSession(String.format("%s - %s", getString(R.string.app_name), mConfig.getServerName()))
                    .setConfigureIntent(ConfigUtil.getPendingIntent(VPNTunnelService.this))
                    .establish();

            if (tunFd == null) {
                destroyAll();
                return;
            }

            mTunFd.set(tunFd);
            mRoutingThroughTunnel.set(false);

            if (routeThroughTunnel(socksServerAddress,dnsResolver,forwardDns,udpResolver,mUdpDnsRelay)){
                mRoutes.clear();
                isServiceRunning.set(true);
                addLogInfo("<font color = #68B86B><b>VPNService Connected</b>");
            }
        }
        catch (IllegalArgumentException | SecurityException | IllegalStateException e)
        {
            throw new Exception(errorMessage, e);
        } finally {
            Locale.setDefault(previousLocale);
        }
    }


    @SuppressLint("DefaultLocale")
    private boolean routeThroughTunnel(final String socksServerAddress, final String[] dnsResolver, boolean forwardDns, final String udpResolver, final boolean transparentDns){
        if (!mRoutingThroughTunnel.compareAndSet(false, true)) {
            destroyAll();
            return false;
        }
        final ParcelFileDescriptor tunFd = mTunFd.get();
        if (tunFd == null) {
            destroyAll();
            return false;
        }

        String dnsgwRelay = null;
        if (forwardDns) {
            int pdnsdPort = VpnUtils.findAvailablePort(8091, 10);
            dnsgwRelay = String.format("%s:%d", mPrivateAddress.mIpAddress, pdnsdPort);
            mPdnsd = new Pdnsd(VPNTunnelService.this, dnsResolver, DNS_RESOLVER_PORT, mPrivateAddress.mIpAddress, pdnsdPort);
            mPdnsd.start();
        }
        mTun2Socks = new Tun2Socks(VPNTunnelService.this, tunFd, mMtu, mPrivateAddress.mRouter, VPN_INTERFACE_NETMASK, socksServerAddress, udpResolver, dnsgwRelay, transparentDns);
        mTun2Socks.start();
        return true;
    }

    private void addLogInfo(String msg){
        String hst = mConfig.getSecureString(SERVER_KEY);
        String prx = mConfig.getSecureString(PROXY_IP_KEY);
        String dns = mConfig.getSecureString(DNS_NAME_SERVER_KEY);
        if (!msg.contains(hst) || !msg.contains(prx) || !msg.contains(dns)){
            hLogStatus.logInfo(msg);
        }
    }

}


