package com.cbtunnel.plus.thread;

import android.content.Context;
import android.content.Intent;
import com.cbtunnel.plus.config.ConfigUtil;
import com.cbtunnel.plus.config.SettingsConstants;
import com.cbtunnel.plus.core.vpnutils.StreamGobbler;
import com.cbtunnel.plus.core.vpnutils.VpnUtils;
import com.cbtunnel.plus.logger.hLogStatus;
import com.cbtunnel.plus.service.HarlieService;
import com.cbtunnel.plus.utils.FileUtils;
import com.cbtunnel.plus.utils.util;
import java.io.File;
import java.io.IOException;
import io.michaelrocks.paranoid.Obfuscate;

@Obfuscate

public class DNSTunnelThread extends Thread implements SettingsConstants {

    private static final String DNS_BIN = "libh2";
    private Process dnsProcess;
    private File filedns;
    private final Context context;
    private final ConfigUtil mConfig;

    private final OnDNSTunnelListener mListener;
    public interface OnDNSTunnelListener {
        void onStop();
    }

    public DNSTunnelThread(Context context,OnDNSTunnelListener mListener) {
        this.context = context;
        mConfig = ConfigUtil.getInstance(context);
        if (mListener == null) {
            throw new NullPointerException();
        }
        this.mListener = mListener;
    }

    @Override
    public final void run(){
        try {
            String a = context.getApplicationInfo().nativeLibraryDir;
            filedns = new File(a, DNS_BIN + ".so");

            StringBuilder cmd1 = new StringBuilder();

            cmd1.append(filedns.getCanonicalPath());
            cmd1.append(" -udp ").append(mConfig.getSecureString(DNS_ADDRESS_KEY)).append(":53").append(" -pubkey ").append(mConfig.getSecureString(DNS_PUBLIC_KEY)).append(" ").append(mConfig.getSecureString(DNS_NAME_SERVER_KEY)).append(" ").append("127.0.0.1:2222");

            dnsProcess = Runtime.getRuntime().exec(cmd1.toString());

            StreamGobbler.OnLineListener onLineListener = log -> {
                if (log.contains("begin stream") && !log.contains("network is unreachable")) {
                    addLogInfo(log);
                }
                if(log.contains("address of UDP DNS resolver")&&mConfig.getSecureString(DNS_PUBLIC_KEY).isEmpty()&&hLogStatus.isTunnelActive()){
                    addLogInfo("Connection error!");
                    context.startService(new Intent(context, HarlieService.class).setAction(HarlieService.RECONNECT_SERVICE));
                }
            };

            StreamGobbler stdoutGobbler = new StreamGobbler(dnsProcess.getInputStream(), onLineListener);
            StreamGobbler stderrGobbler = new StreamGobbler(dnsProcess.getErrorStream(), onLineListener);

            stdoutGobbler.start();
            stderrGobbler.start();

            dnsProcess.waitFor();
        } catch (IOException | InterruptedException e) {
            interrupt();
            addLogInfo("DNS Thread error: " + e.getMessage());
            mListener.onStop();
        } catch (Exception ex) {
            hLogStatus.logDebug("DNS Thread Error"+ ex.getMessage());
        }

    }


    private void addLogInfo(String mLog){
        if (util.isNetworkAvailable(context)&&hLogStatus.isTunnelActive()){
            if (mLog.contains(mConfig.getSecureString(DNS_ADDRESS_KEY))||mLog.contains(mConfig.getSecureString(DNS_NAME_SERVER_KEY))){
                mLog = mLog.replace(mConfig.getSecureString(DNS_ADDRESS_KEY), FileUtils.hideSTR(mConfig.getSecureString(DNS_ADDRESS_KEY))).replace(mConfig.getSecureString(DNS_NAME_SERVER_KEY),FileUtils.hideSTR(mConfig.getSecureString(DNS_NAME_SERVER_KEY))).trim();
            }
            if(!mLog.contains("network is unreachable")&& util.isNetworkAvailable(context)&&!mLog.contains("error: null")){
                hLogStatus.logInfo(mLog);
            }
        }
    }

    @Override
    public void interrupt(){
        try {
            if (dnsProcess != null){
                dnsProcess.destroy();
                dnsProcess = null;
            }
        } catch (Exception ignored) {
        }
        try {
            if (filedns != null){
                VpnUtils.killProcess(filedns);
                filedns = null;
            }
        } catch (Exception ignored) {
        }
        super.interrupt();
    }


}


