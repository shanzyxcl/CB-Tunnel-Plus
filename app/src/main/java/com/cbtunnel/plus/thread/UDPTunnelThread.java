package com.cbtunnel.plus.thread;

import android.content.Intent;

import com.trilead.ssh2.transport.TransportManager;
import com.cbtunnel.plus.R;
import com.cbtunnel.plus.config.ConfigUtil;
import com.cbtunnel.plus.config.SettingsConstants;
import com.cbtunnel.plus.core.vpnutils.StreamGobbler;
import com.cbtunnel.plus.core.vpnutils.VpnUtils;
import com.cbtunnel.plus.logger.hLogStatus;
import com.cbtunnel.plus.service.HarlieService;
import com.cbtunnel.plus.utils.FileUtils;
import com.cbtunnel.plus.utils.util;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class UDPTunnelThread extends Thread implements SettingsConstants {

    private static final String UDP_BIN = "libh3";
    private final HarlieService service;
    private Process dnsProcess;
    private File fileUDP;
    private static ConfigUtil config;

    private final OnUDPListener mListener;
    public interface OnUDPListener {
        void onReconnect();
        void onStop();
    }

    public UDPTunnelThread(HarlieService service,OnUDPListener mListener) {
        this.service = service;
        config = ConfigUtil.getInstance(service);
        if (mListener == null) {
            throw new NullPointerException();
        }
        this.mListener = mListener;
    }

    @Override
    public void run() {
        try {
            StringBuilder cmd1 = new StringBuilder();
            String a = service.getApplicationInfo().nativeLibraryDir;
            fileUDP = new File(a, UDP_BIN + ".so");
            String content = config.getUDPConfig();
            JSONObject jo = new JSONObject(content);
            String server = jo.getString("server").split(":")[0].replace("[host]", config.getSecureString(SERVER_KEY));
            server = TransportManager.createInetAddress(server).getHostAddress();
            jo.put("server", server + ":" + jo.getString("server").split(":")[1]);
            content = jo.toString();
            config.setServerHost(FileUtils.hideJson(server));
            File fileConf = makeConfig(service.getFilesDir(), content);
            cmd1.append(fileUDP.getCanonicalPath());
            cmd1.append(" -c " + fileConf.getPath() + " client");
            dnsProcess = Runtime.getRuntime().exec(cmd1.toString());
            hLogStatus.updateStateString(hLogStatus.VPN_GET_CONFIG, service.getResources().getString(R.string.state_wait));
            StreamGobbler.OnLineListener onLineListener = log -> {
                if (log.contains("Connected")) {
                    hLogStatus.updateStateString(hLogStatus.VPN_CONNECTED, service.getResources().getString(R.string.state_connected));
                    addLog(log.split("INFO]")[1].replace(config.getSecureString(SERVER_KEY),FileUtils.hideSTR(config.getSecureString(SERVER_KEY))).replace("0mConnected","<b>Connected</b>").replace("[","").replace("]"," "));
                    addLog(String.format("%s %s", config.getServerType(),service.getResources().getString(R.string.state_connected)));
                    service.VPNTunnel_handler(true);
                } else if (log.toLowerCase().contains("auth error")) {
                    addLog("<font color = #ff0000>Failed to authenticate, username or password expired");
                    service.startService(new Intent(service, HarlieService.class).setAction(HarlieService.STOP_SERVICE).putExtra("stateSTOP_SERVICE",service.getResources().getString(R.string.state_auth_failed)));
                } else if (log.toLowerCase().contains("out of retries")) {
                    hLogStatus.clearLog();
                    addLog(config.getServerType()+" Connection time out");
                    interrupt();
                    mListener.onReconnect();
                }
            };
            StreamGobbler stdoutGobbler = new StreamGobbler(dnsProcess.getInputStream(), onLineListener);
            StreamGobbler stderrGobbler = new StreamGobbler(dnsProcess.getErrorStream(), onLineListener);
            stdoutGobbler.start();
            stderrGobbler.start();

            dnsProcess.waitFor();
        } catch (IOException | InterruptedException ignored) {
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void interrupt() {
        try {
            if (dnsProcess != null) {
                dnsProcess.destroy();
                dnsProcess = null;
            }
        } catch (Exception ignored) {
        }
        try {
            if (fileUDP != null){
                VpnUtils.killProcess(fileUDP);
                fileUDP = null;
            }
        } catch (Exception ignored) {
        }
        super.interrupt();
    }

    private File makeConfig(File fileDir, String content) throws IOException {
        File f = new File(fileDir, "udpconfig.json");
        if (f.exists()) {
            f.delete();
        }
        saveTextFile(f, content);
        File cache = new File(fileDir, "udpconfig.json");
        if (!cache.exists()) {
            try {
                cache.createNewFile();
            } catch (Exception ignored) {
            }
        }
        return f;
    }

    private static void saveTextFile(File file, String contents) {
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file, false);
            writer.write(contents);
            writer.close();
        } catch (IOException ignored) {
        }
    }

    public void addLog(String msg) {
        if (util.isNetworkAvailable(service)&&hLogStatus.isTunnelActive()){
            hLogStatus.logInfo(msg);
        }
    }

}

