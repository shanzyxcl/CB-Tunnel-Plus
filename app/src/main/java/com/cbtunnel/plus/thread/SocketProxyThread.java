package com.cbtunnel.plus.thread;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.ProxyInfo;
import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.regex.*;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import com.cbtunnel.plus.R;
import com.cbtunnel.plus.config.ConfigUtil;
import com.cbtunnel.plus.config.SettingsConstants;
import com.cbtunnel.plus.core.vpnutils.VpnUtils;
import com.cbtunnel.plus.logger.hLogStatus;
import com.cbtunnel.plus.service.HarlieService;
import com.cbtunnel.plus.service.VPNTunnelService;
import com.cbtunnel.plus.utils.SSLUtil;
import io.michaelrocks.paranoid.Obfuscate;

@Obfuscate
public class SocketProxyThread extends Thread implements SettingsConstants {
    private CountDownLatch mTunnelThreadStopSignal;
    private final HarlieService service;
    public static HttpsURLConnection huc;
    public static BackServer mBackServerThread;
    private ServerSocket ss;
    private Socket client;
    public static Socket server;
    public static SSLSocket mSSLSocket;
    private static int mPayload_type;
    private final OnWSTunnelListener mListener;
    public interface OnWSTunnelListener {
        void onStop();
    }
    private final int mProxyAddress;
    private final int tunnel;
    private final ConfigUtil mConfig;
    private int coA = 0;
    private int coB = 0;
    private int coC = 0;
    private int coD = 0;
    private String cow;
    private String[] cox;
    private String[] coy;
    private String[] coz;

    @SuppressLint({"NewApi", "DefaultLocale"})
    public SocketProxyThread(HarlieService service, OnWSTunnelListener mListener) {
        this.service = service;
        mConfig = ConfigUtil.getInstance(service);
        mProxyAddress = Integer.parseInt(mConfig.getProxyAddress().split(":")[1]);
        mPayload_type = mConfig.getPayloadType();
        tunnel = mConfig.getServerType().equals(SERVER_TYPE_SSH)? service.SSH_DNS:service.OVPN;
        if (mListener == null) {
            throw new NullPointerException();
        }
        this.mListener = mListener;
        try {
            ConnectivityManager cm = (ConnectivityManager) service.getSystemService(Context.CONNECTIVITY_SERVICE);
            ProxyInfo proxy = cm.getDefaultProxy();
            if (proxy != null) {
                addLogInfo("<b>Network Proxy:</b> " + String.format("%s:%d", proxy.getHost(), proxy.getPort()));
            }
        }catch (Exception ignored){}
    }

    private void connectSocket(String host, int port, boolean ssl) throws Exception {
        server = new Socket();
        if (ssl){
            server.bind(new InetSocketAddress(0));
        }
        server.connect(new InetSocketAddress(host, port));
        doVpnProtect(server);
    }

    private void connectSSL() throws Exception {
        SSLSocketFactory factory = new SSLUtil();
        addLogInfo("<font color = #FF9600>Setting up SNI...");
        String mSni = (mConfig.getSecureString(SNI_HOST_KEY).startsWith("http")) ? mConfig.getSecureString(SNI_HOST_KEY) : "https://" + mConfig.getSecureString(SNI_HOST_KEY);
        URL url = new URL(mSni);
        mSni = url.getHost();
        if (url.getPort() > 0) {
            mSni = mSni + ":" + url.getPort();
        }
        if (!url.getPath().equals("/")) {
            mSni = mSni + url.getPath();
        }
        huc = (HttpsURLConnection) url.openConnection(new Proxy(Proxy.Type.HTTP, mBackServerThread.getLocalSocketAddr()));
        huc.setHostnameVerifier(new HostnameVerifier() {
            @SuppressLint({"BadHostnameVerifier"})
            public boolean verify(String str, SSLSession sSLSession) {
                return true;
            }
        });
        huc.setSSLSocketFactory(factory);
        huc.connect();
    }


    private boolean connectSocket() throws Exception {
        try {
            String readRequest = new BufferedReader(new InputStreamReader(client.getInputStream())).readLine();
            int tunnel_type = mConfig.getPayloadType();
            while (true) {
                String payload = mConfig.getSecureString(CUSTOM_PAYLOAD_KEY);
                //addLogInfo ( payload );
                String proxy = mConfig.getSecureString(PROXY_IP_KEY);
                int proxyPort = Integer.parseInt(mConfig.getSecureString(PROXY_PORT_KEY));
                String[] hostname = readRequest.split(" ");
                String host = hostname[1].split(":")[0];
                int port = Integer.parseInt(hostname[1].split(":")[1]);
                if (tunnel_type == PAYLOAD_TYPE_DIRECT || tunnel_type == PAYLOAD_TYPE_OVPN_UDP){
                    connectSocket(host, port, false);
                    send200Status(client.getOutputStream());
                    return true;
                }
                else if (tunnel_type == PAYLOAD_TYPE_DIRECT_PAYLOAD){
                    connectSocket(host, port, false);
                    mPayloadInject(payload, server, readRequest);
                    send200Status(client.getOutputStream());
                    return true;
                }
                else if (tunnel_type == PAYLOAD_TYPE_HTTP_PROXY){
                    connectSocket(proxy, proxyPort, false);
                    mPayloadInject(payload, server, readRequest);
                    return true;
                }
                else if (tunnel_type == PAYLOAD_TYPE_SSL){
                    connectSocket(host, port, true);
                    connectSSL();
                    send200Status(client.getOutputStream());
                    return true;
                }
                else if (tunnel_type == PAYLOAD_TYPE_SSL_PAYLOAD){
                    connectSocket(host, port, true);
                    connectSSL();
                    mPayloadInject(payload, mSSLSocket, readRequest);
                    return true;
                }
                else if (tunnel_type == PAYLOAD_TYPE_SSL_PROXY){
                    connectSocket(proxy, proxyPort, true);
                    connectSSL();
                    mPayloadInject(payload, mSSLSocket, readRequest);
                    return true;
                }
                return false;
            }
        } catch (Exception e) {
            addLogInfo("<font color = #FF9600><b>Socket: </b>connection error!");
        }
        return false;
    }

    private void mPayloadInject(final String payload, final Socket socket, final String readLine) throws Exception{
        String trim;
        int i = 0;
        String proxy = mConfig.getSecureString(PROXY_IP_KEY);
        int proxyPort = Integer.parseInt(mConfig.getSecureString(PROXY_PORT_KEY));
        String remote = proxy+":"+proxyPort;
        String[] split = remote.trim().split(":");
        int i2 = 80;
        if (split.length > 1) {
            trim = split[0].trim();
            try {
                i2 = Integer.parseInt(split[1].trim());
            } catch (NumberFormatException e) {
                i2 = 80;
            }
        } else {
            trim = split[0].trim();
        }
        String[] split2;
        CharSequence charSequence;
        OutputStream outputStream = socket.getOutputStream();
        String[] split3 = readLine.split(" ");
        CharSequence charSequence2 = split3[1];
        if (split3[0].equals("CONNECT")) {
            split2 = split3[1].split(":");
            charSequence2 = split2[0];
            charSequence = split2.length < 2 ? "443" : split2[1];
        } else {
            charSequence = "80";
        }
        String cR = payload;
        if (cR.contains("[random]")) {
            Random random = new Random();
            split = cR.split(Pattern.quote("[random]"));
            cR = split[random.nextInt(split.length)];
        }
        this.cow = cR;
        cR = payload;
        if (cR.contains("[repeat]")) {
            String[] split4 = cR.split(Pattern.quote("[repeat]"));
            cR = split4[this.coD];
            if (this.coD + 1 > split4.length) {
                this.coD = 0;
            }
        }
        this.cow = cR;
        this.cow = payload.replace("realData", "netData");
        int indexOf = this.cow.indexOf("netData");
        if (indexOf < 0) {
            trim = this.cow.replace("[METHOD]", split3[0]).replace("[method]", split3[0]).replace("[SSH]", split3[1]).replace("[IP_PORT]", split3[1]).replace("[ip_port]", split3[1]).replace("[IP]", charSequence2).replace("[ip]", charSequence2).replace("[PORT]", charSequence).replace("[cr]", "\r").replace("[lf]", "\n").replace("[crlf]", "\r\n").replace("[lfcr] ", "\n\r").replace("[protocol]", split3[2]).replace("[host]", charSequence2).replace("[port]", charSequence).replace("[host_port]", split3[1]).replace("[ssh]", split3[1]).replace("[ua]", this. ua()).replace("[raw]", readLine + "\r\n\r\n").replace("[real_raw]", readLine + "\r\n\r\n").replace("[auth]", auth()).replace("\\r", "\r").replace("\\n", "\n");
        } else if (this.cow.substring(indexOf + 7, (indexOf + 7) + 1).equals("@")) {
            Matcher matcher = Pattern.compile("\\[.*?@(.*?)\\]").matcher(this.cow);
            cR = "";
            if (matcher.find()) {
                cR = matcher.group(1);
            }
            trim = this.cow.replace("[netData@" + cR.trim() + "]", split3[0] + " " + split3[1] + "@" + cR.trim() + " " + split3[2]).replace("[METHOD]", split3[0]).replace("[method]", split3[0]).replace("[SSH]", split3[1]).replace("[IP_PORT]", split3[1]).replace("[ip_port]", split3[1]).replace("[IP]", charSequence2).replace("[ip]", charSequence2).replace("[PORT]", charSequence).replace("[cr]", "\r").replace("[lf]", "\n").replace("[crlf]", "\r\n").replace("[lfcr] ", "\n\r").replace("[protocol]", split3[2]).replace("[host]", charSequence2).replace("[port]", charSequence).replace("[host_port]", split3[1]).replace("[ssh]", split3[1]).replace("[ua]", this. ua()).replace("[raw]", readLine + "\r\n\r\n").replace("[real_raw]", readLine + "\r\n\r\n").replace("[auth]", auth()).replace("\\r", "\r").replace("\\n", "\n");
        } else {
            int i3 = indexOf == 0 ? 1 : indexOf;
            Matcher matcher2 = Pattern.compile("\\[(.*?)@.*?\\]").matcher(this.cow);
            cR = "";
            if (matcher2.find()) {
                cR = matcher2.group(1);
            }
            trim = this.cow.substring(i3 + -1, i3).equals("@") ? this.cow.replace("[" + cR.trim() + "@netData]", split3[0] + " " + cR.trim() + "@" + split3[1] + " " + split3[2]).replace("[METHOD]", split3[0]).replace("[method]", split3[0]).replace("[SSH]", split3[1]).replace("[IP_PORT]", split3[1]).replace("[ip_port]", split3[1]).replace("[IP]", charSequence2).replace("[ip]", charSequence2).replace("[PORT]", charSequence).replace("[cr]", "\r").replace("[lf]", "\n").replace("[crlf]", "\r\n").replace("[lfcr] ", "\n\r").replace("[protocol]", split3[2]).replace("[host]", charSequence2).replace("[port]", charSequence).replace("[host_port]", split3[1]).replace("[ssh]", split3[1]).replace("[ua]", this. ua()).replace("[raw]", readLine + "\r\n\r\n").replace("[real_raw]", readLine + "\r\n\r\n").replace("[auth]", auth()).replace("\\r", "\r").replace("\\n", "\n") : this.cow.replace("[netData]", readLine).replace("[METHOD]", split3[0]).replace("[method]", split3[0]).replace("[SSH]", split3[1]).replace("[IP_PORT]", split3[1]).replace("[ip_port]", split3[1]).replace("[IP]", charSequence2).replace("[ip]", charSequence2).replace("[PORT]", charSequence).replace("[cr]", "\r").replace("[lf]", "\n").replace("[crlf]", "\r\n").replace("[lfcr] ", "\n\r").replace("[protocol]", split3[2]).replace("[host]", charSequence2).replace("[port]", charSequence).replace("[host_port]", split3[1]).replace("[ssh]", split3[1]).replace("[ua]", this. ua()).replace("[raw]", readLine + "\r\n\r\n").replace("[real_raw]", readLine + "\r\n\r\n").replace("[auth]", auth()).replace("\\r", "\r").replace("\\n", "\n");
        }
        Matcher matcher3 = Pattern.compile(".*?\\[rotation_method=(.*?)\\].*?").matcher(trim);
        while (matcher3.find()) {
            cR = matcher3.group(1);
            this.cox = cR.split(";");
            if (this.coA + 1 > this.cox.length) {
                this.coA = 0;
            }
            trim = trim.replace("[rotation_method=" + cR + "]", this.cox[this.coA]);
        }
        matcher3 = Pattern.compile(".*?\\[rotation=(.*?)\\].*?").matcher(trim);
        while (matcher3.find()) {
            cR = matcher3.group(1);
            this.coy = cR.split(";");
            if (this.coB + 1 > this.coy.length) {
                this.coB = 0;
            }
            trim = trim.replace("[rotation=" + cR + "]", this.coy[this.coB]);
        }
        matcher3 = Pattern.compile(".*?\\[rotate=(.*?)\\].*?").matcher(trim);
        while (matcher3.find()) {
            cR = matcher3.group(1);
            this.coz = cR.split(";");
            if (this.coC + 1 > this.coz.length) {
                this.coC = 0;
            }
            trim = trim.replace("[rotate=" + cR + "]", this.coz[this.coC]);
        }
        addLogInfo("<font color = #FF9600>Sending payload");
        addLogInfo("<font color = #FF9600>"+service.getResources().getString(R.string.state_proxy_inject));
        trim = d(trim);
        String[] split5;
        if (trim.contains("[split]")) {
            split5 = trim.split("\\[split\\]");
            while (i < split5.length) {
                outputStream.write(split5[i].getBytes());
                outputStream.flush();
                i++;
            }
        } else if (trim.contains("[splitNoDelay]")) {
            split5 = trim.split("\\[splitNoDelay\\]");
            while (i < split5.length) {
                outputStream.write(split5[i].getBytes());
                outputStream.flush();
                i++;
            }
        } else if (trim.contains("[instant_split]")) {
            split5 = trim.split("\\[instant_split\\]");
            while (i < split5.length) {
                outputStream.write(split5[i].getBytes());
                outputStream.flush();
                i++;
            }
        } else if (trim.contains("[delay]")) {
            split5 = trim.split("\\[delay\\]");
            while (i < split5.length) {
                outputStream.write(split5[i].getBytes());
                outputStream.flush();
                if (i != split5.length - 1) {
                    Thread.sleep(1000);
                }
                i++;
            }
        } else if (trim.contains("[delay_split]")) {
            split5 = trim.split("\\[delay_split\\]");
            while (i < split5.length) {
                outputStream.write(split5[i].getBytes());
                outputStream.flush();
                if (i != split5.length - 1) {
                    Thread.sleep(1000);
                }
                i++;
            }
        } else if (trim.contains("[split_delay]")) {
            split2 = trim.split("\\[split_delay\\]");
            for (int i4 = 0; i4 < split2.length; i4++) {
                outputStream.write(split2[i4].getBytes());
                outputStream.flush();
                if (i4 != split2.length - 1) {
                    Thread.sleep(1000);
                }
            }
        } else {
            outputStream.write(trim.getBytes());
            outputStream.flush();
            Thread.sleep(1000);
        }
        this.coB++;
        this.coA++;
        this.coC++;
        this.coD++;
    }

    private String d(String str) {
        String str2 = str;
        String str3 = str2;
        if (str2.contains("[cr*")) {
            str3 = a(str2, "[cr*", "\r");
        }
        String str4 = str3;
        if (str3.contains("[lf*")) {
            str4 = a(str3, "[lf*", "\n");
        }
        str2 = str4;
        if (str4.contains("[crlf*")) {
            str2 = a(str4, "[crlf*", "\r\n");
        }
        String str5 = str2;
        if (str2.contains("[lfcr*")) {
            str5 = a(str2, "[lfcr*", "\n\r");
        }
        return str5;
    }

    private String a(String str, String str2, String str3) {
        while (str.contains(str2)) {
            Matcher matcher = Pattern.compile("\\[.*?\\*(.*?[0-9])\\]").matcher(str);
            if (matcher.find()) {
                int intValue = Integer.valueOf(matcher.group(1)).intValue();
                String str7 = "";
                for (int i = 0; i < intValue; i++) {
                    str7 = new StringBuffer().append(str7).append(str3).toString();
                }
                String str8 = str;
                str = str8.replace(new StringBuffer().append(str2).append(String.valueOf(intValue)).append("]").toString(), str7);
            }
        }
        return str;
    }

    private String ua() {
        String property = System.getProperty("http.agent");
        return property == null ? "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.130 Safari/537.36" : property;
    }

    private String auth() {
        String str = "";
        try {
        } catch (Exception e) {
            hLogStatus.logInfo(e.getMessage());
        }
        return str;
    }

    private void send200Status(OutputStream output) throws Exception {
        output.write("HTTP/1.0 200 Connection Established\r\n\r\n".getBytes());
        output.flush();
    }

    @Override
    public void run() {
        super.run();
        mTunnelThreadStopSignal = new CountDownLatch(1);
        try {
            ss = new ServerSocket(mProxyAddress);
            if (mPayload_type == PAYLOAD_TYPE_SSL || mPayload_type == PAYLOAD_TYPE_SSL_PAYLOAD || mPayload_type == PAYLOAD_TYPE_SSL_PROXY) {
                if (mBackServerThread!=null){
                    mBackServerThread.interrupt();
                }
            }
            service.mHandler.sendEmptyMessage(tunnel);
            while (HarlieService.isVPNRunning()) {
                client = ss.accept();
                if (mPayload_type == PAYLOAD_TYPE_SSL || mPayload_type == PAYLOAD_TYPE_SSL_PAYLOAD || mPayload_type == PAYLOAD_TYPE_SSL_PROXY) {
                    mBackServerThread = new BackServer();
                    mBackServerThread.start();
                }
                if (client != null && !client.isClosed() && connectSocket()) {
                    client.setKeepAlive(true);
                    if (mSSLSocket != null && mSSLSocket.isConnected()) {
                        mSSLSocket.setKeepAlive(true);
                        server.setKeepAlive(true);
                        doVpnProtect(mSSLSocket);
                        HTTPInjectorThread.connect(client, mSSLSocket, "16384", "32768");
                    } else if (server != null && server.isConnected()) {
                        server.setKeepAlive(true);
                        doVpnProtect(server);
                        HTTPInjectorThread.connect(client, server, "16384", "32768");
                    }
                }
            }
        } catch (Exception e) {
            String msg = e.toString();
            if (msg.contains("bind failed")) {
                interrupt();
                addLogInfo(e.toString());
                mListener.onStop();
            }
        }
        if (!HarlieService.mStopping) {
            try {
                mTunnelThreadStopSignal.await();
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void doVpnProtect(Socket socket) {
        if (tunnel==service.SSH_DNS){
            new VPNTunnelService().protect(socket);
        }else{
            VpnUtils.isProtected(socket);
        }
    }

    private void addLogInfo(String mLog){
        hLogStatus.logInfo(mLog);
    }


    @Override
    public void interrupt(){
        try {
            if (ss != null) {
                ss.close();
                ss = null;
            }
        } catch (Exception ignored) {
        }
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception ignored) {
        }
        try {
            if (server != null) {
                server.close();
                server = null;
            }
        } catch (Exception ignored) {
        }
        try {
            if (mSSLSocket != null) {
                mSSLSocket.close();
                mSSLSocket = null;
            }
        } catch (Exception ignored) {
        }
        try {
            if (huc != null) {
                huc.disconnect();
            }
        } catch (Exception ignored) {
        }
        try {
            if (mBackServerThread != null) {
                mBackServerThread.interrupt();
                mBackServerThread = null;
            }
        } catch (Exception ignored) {
        }
        try {
            if (mSSLSocket != null) {
                mSSLSocket.close();
                mSSLSocket = null;
            }
        } catch (Exception ignored) {
        }
        if (mTunnelThreadStopSignal != null) mTunnelThreadStopSignal.countDown();
        super.interrupt();
    }



}


