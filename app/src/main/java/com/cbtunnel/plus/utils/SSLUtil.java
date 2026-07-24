package com.cbtunnel.plus.utils;

import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.io.*;
import java.util.*;
import android.annotation.*;
import com.cbtunnel.plus.logger.hLogStatus;
import com.cbtunnel.plus.thread.SocketProxyThread;

import javax.net.ssl.*;

public class SSLUtil extends SSLSocketFactory
{
	private final SSLContext mSSLContext;

	public SSLUtil() throws Exception
	{
		mSSLContext = SSLContext.getInstance("TLS");
		mSSLContext.init(null, new TrustManager[]{new MyX509TrustManager()}, new SecureRandom());
	}

	private void createSSLSocket(String host, int port, boolean z) throws IOException
	{
		SocketProxyThread.mSSLSocket = (SSLSocket) mSSLContext.getSocketFactory().createSocket(SocketProxyThread.server, host, port, z);
		LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>();
		String protocol = "TLS";
		if (protocol.equals("SSL") || protocol.equals("TLS")) {
			Collections.addAll(linkedHashSet, SocketProxyThread.mSSLSocket.getEnabledProtocols());
		} else {
			linkedHashSet.add(protocol);
		}
		SocketProxyThread.mSSLSocket.setEnabledProtocols(linkedHashSet.toArray(new String[linkedHashSet.size()]));
		SocketProxyThread.mSSLSocket.addHandshakeCompletedListener(new HandshakeTunnelCompletedListener(host, port, SocketProxyThread.mSSLSocket));
	}

	public Socket createSocket(String host, int port) throws IOException
	{
		createSSLSocket(host, port, true);
		return SocketProxyThread.mSSLSocket;
	}

	public Socket createSocket(String str, int i, InetAddress inetAddress, int i2)
	{
		return null;
	}

	public Socket createSocket(InetAddress inetAddress, int i)
	{
		return null;
	}

	public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2)
	{
		return null;
	}

	public Socket createSocket(Socket socket, String host, int port, boolean z) throws IOException
	{
		createSSLSocket(host, port, z);
		return SocketProxyThread.mSSLSocket;
	}

	public String[] getDefaultCipherSuites()
	{
		return new String[0];
	}

	public String[] getSupportedCipherSuites()
	{
		return new String[0];
	}
	public class MyX509TrustManager implements X509TrustManager {
		@SuppressLint({"TrustAllX509TrustManager"})
		public void checkClientTrusted(X509Certificate[] x509CertificateArr, String str) {
		}

		@SuppressLint({"TrustAllX509TrustManager"})
		public void checkServerTrusted(X509Certificate[] x509CertificateArr, String str) {
		}

		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}

	class HandshakeTunnelCompletedListener implements HandshakeCompletedListener {
		private final String host;
		private final int port;
		private final SSLSocket sSLSocket;
		HandshakeTunnelCompletedListener( String str, int i, SSLSocket sSLSocket) {
			this.host = str;
			this.port = i;
			this.sSLSocket = sSLSocket;
		}

		public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
			hLogStatus.logInfo("Starting SSL Handshake.");
			hLogStatus.logInfo(new StringBuffer().append("<b>Established ").append(handshakeCompletedEvent.getSession().getProtocol()).append(" connection with ").append(FileUtils.hideSTR(host)).append(":").append(this.port).append(" using ").append(handshakeCompletedEvent.getCipherSuite()).append("</b>").toString());
			hLogStatus.logInfo("SSL: Using cipher " + handshakeCompletedEvent.getSession().getCipherSuite());
			hLogStatus.logInfo("SSL: Using protocol " + handshakeCompletedEvent.getSession().getProtocol());
			hLogStatus.logInfo("SSL: Handshake finished");
		}
	}
}
