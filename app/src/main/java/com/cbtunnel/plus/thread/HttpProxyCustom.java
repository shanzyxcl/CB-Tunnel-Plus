package com.cbtunnel.plus.thread;

import com.trilead.ssh2.ProxyData;
import com.trilead.ssh2.crypto.Base64;
import com.trilead.ssh2.transport.ClientServerHello;
import com.trilead.ssh2.transport.TransportManager;
import com.cbtunnel.plus.R;
import com.cbtunnel.plus.core.vpnutils.TunnelUtils;
import com.cbtunnel.plus.logger.hLogStatus;
import com.cbtunnel.plus.service.HarlieService;
import com.cbtunnel.plus.service.VPNTunnelService;
import com.cbtunnel.plus.utils.FileUtils;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import javax.net.ssl.SSLSocket;
import io.michaelrocks.paranoid.Obfuscate;

@Obfuscate
public class HttpProxyCustom implements ProxyData
{
    private final String proxyHost;
    private final String proxyPass;
    private final int proxyPort;
    private final String proxyUser;
    private final String requestPayload;
    private final String requestSNI;
    private boolean modoDropbear = false;
    private Socket sock;
    private final HarlieService service;

    public HttpProxyCustom(String proxyHost, int proxyPort, String proxyUser, String proxyPass, String requestPayload ,String requestSNI, boolean modoDropbear, HarlieService service) {
        if (proxyHost == null) {
            throw new IllegalArgumentException("proxyHost must be non-null");
        } else if (proxyPort < 0) {
            throw new IllegalArgumentException("proxyPort must be non-negative");
        } else {
            this.proxyHost = proxyHost;
            this.proxyPort = proxyPort;
            this.proxyUser = proxyUser;
            this.proxyPass = proxyPass;
            this.requestPayload = requestPayload;
            this.modoDropbear = modoDropbear;
            this.requestSNI = requestSNI;
            this.service = service;
        }
    }

    @Override
    public Socket openConnection(String hostname, int port, int connectTimeout, int readTimeout) throws IOException {
        sock = new Socket();
        if (requestSNI!=null && requestPayload==null)sock.bind(new InetSocketAddress(0));
        InetAddress addr = TransportManager.createInetAddress(this.proxyHost);
        sock.connect(new InetSocketAddress(addr, this.proxyPort), connectTimeout);
        sock.setSoTimeout(readTimeout);
        sock.setKeepAlive(true);
        sock.setTcpNoDelay(true);
        new VPNTunnelService().protect(sock);
        if (requestSNI != null) {
            sock = doSSLHandshake(hostname,port,requestSNI);
        }
        addLogInfo("<font color = #FF9600>Sending payload");

        String requestPayload = getRequestPayload(hostname, port);

        OutputStream out = sock.getOutputStream();

        if (!TunnelUtils.injectSplitPayload(requestPayload, out)) {
            try {
                out.write(requestPayload.getBytes("ISO-8859-1"));
            } catch (UnsupportedEncodingException e2) {
                out.write(requestPayload.getBytes());
            }
            out.flush();
        }

        addLogInfo("<font color = #FF9600>"+service.getResources().getString(R.string.state_proxy_inject));

        if (modoDropbear) {
            return sock;
        }

        byte[] buffer = new byte[1024];
        InputStream in = sock.getInputStream();

        int len = ClientServerHello.readLineRN(in, buffer);

        String httpReponseFirstLine = "";
        try {
            httpReponseFirstLine = new String(buffer, 0, len, "ISO-8859-1");
        } catch (UnsupportedEncodingException e3) {
            httpReponseFirstLine = new String(buffer, 0, len);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<b>");
        sb.append(httpReponseFirstLine);
        sb.append("</b>");
        addLogInfo(sb.toString());
        len = Integer.parseInt(httpReponseFirstLine.substring(9, 12));
        if (len != 200) {
            String asw = String.valueOf(len);
            String replacw = httpReponseFirstLine.replace(httpReponseFirstLine, "HTTP/1.1 200 Ok");
            addLogInfo("<b>Proxy: </b><font color = #FF9600>Auto Replace Header");
            StringBuilder su = new StringBuilder();
            su.append("<b>");
            su.append(replacw);
            su.append("</b>");
            //Status.logInfo(su.toString());
            len = Integer.parseInt(asw.replace(asw, "200"));
        } else {
            return sock;
        }

        String httpReponseAll = httpReponseFirstLine;
        while ((len = ClientServerHello.readLineRN(in, buffer)) != 0) {
            httpReponseAll += "\n";
            try {
                httpReponseAll += new String(buffer, 0, len, "ISO-8859-1");
            } catch (UnsupportedEncodingException e3) {
                httpReponseAll += new String(buffer, 0, len);
            }
        }

        if (!httpReponseAll.isEmpty())
            hLogStatus.logDebug(httpReponseAll);

        if (httpReponseFirstLine.startsWith("HTTP/") == false) throw new IOException("The proxy did not send back a valid HTTP response.");
        if (httpReponseFirstLine.length() < 14) throw new IOException("The proxy did not send back a valid HTTP response.");
        if (httpReponseFirstLine.charAt(8) != ' ') throw new IOException("The proxy did not send back a valid HTTP response.");
        if (httpReponseFirstLine.charAt(12) != ' ') throw new IOException("The proxy did not send back a valid HTTP response.");
        if (len < 0 || len > 999) {
            throw new IOException("The proxy did not send back a valid HTTP response.");
        } else if (len != 200) {
            String stringBuffer = new StringBuffer().append(new StringBuffer().append("HTTP/1.0 200 Connection established").append("\r\n").toString()).append("\r\n").toString();
            out.write(stringBuffer.getBytes());
            out.flush();
            return sock;
        } else {
            return sock;
        }
    }


    private String getRequestPayload(String hostname, int port) {
        String payload = this.requestPayload;
        if (payload != null) {
            payload = TunnelUtils.formatCustomPayload(hostname, port, payload);
        }
        else {
            StringBuffer sb = new StringBuffer();

            sb.append("CONNECT ");
            sb.append(hostname);
            sb.append(':');
            sb.append(port);
            sb.append(" HTTP/1.0\r\n");
            if (!(this.proxyUser == null || this.proxyPass == null)) {
                char[] encoded;
                String credentials = this.proxyUser + ":" + this.proxyPass;
                try {
                    encoded = Base64.encode(credentials.getBytes("ISO-8859-1"));
                } catch (UnsupportedEncodingException e) {
                    encoded = Base64.encode(credentials.getBytes());
                }
                sb.append("Proxy-Authorization: Basic ");
                sb.append(encoded);
                sb.append("\r\n");
            }
            sb.append("\r\n");

            payload = sb.toString();
        }

        return payload;
    }

    private SSLSocket doSSLHandshake(String host, int port, String sni) throws IOException {
        try {
            addLogInfo("<font color = #FF9600>Setting up SNI...");
            TLSSocketFactory tsf = new TLSSocketFactory();
            SSLSocket socket = (SSLSocket) tsf.createSocket(host, port);
            try {
                socket.getClass().getMethod("setHostname", String.class).invoke(socket, sni);
            } catch (Throwable e) {
                //addLogInfo("ignore any error, we just can't set the hostname...");
            }
            socket.setEnabledProtocols(socket.getSupportedProtocols());
            socket.addHandshakeCompletedListener(new HandshakeTunnelCompletedListener(host, port, socket));
            socket.startHandshake();
            return socket;
        } catch (Exception e) {
            IOException iOException = new IOException(new StringBuffer().append("Could not do SSL handshake: ").append(e).toString());
            throw iOException;
        }
    }

    public class HandshakeTunnelCompletedListener implements HandshakeCompletedListener {
        private final String host;
        private final int port;
        private final SSLSocket sSLSocket;
        public HandshakeTunnelCompletedListener(String str, int i, SSLSocket sSLSocket) {
            this.host = str;
            this.port = i;
            this.sSLSocket = sSLSocket;
        }
        public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
            addLogInfo("Starting SSL Handshake.");
            addLogInfo(new StringBuffer().append("<b>Established ").append(handshakeCompletedEvent.getSession().getProtocol()).append(" connection with ").append(FileUtils.hideSTR(host)).append(":").append(port).append(" using ").append(handshakeCompletedEvent.getCipherSuite()).append("</b>").toString());
            addLogInfo("SSL: Using cipher " + handshakeCompletedEvent.getSession().getCipherSuite());
            addLogInfo("SSL: Using protocol " + handshakeCompletedEvent.getSession().getProtocol());
            addLogInfo("SSL: Handshake finished");
        }
    }

    @Override
    public void close() {
        if (sock == null) return;
        try {
            sock.close();
        } catch (IOException ignored) {
        }
    }


    private void addLogInfo(String info){
        hLogStatus.logInfo(info);
    }
}

