package com.cbtunnel.plus.thread;

import android.os.AsyncTask;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ExpireDate extends AsyncTask<String, String, String>
{

	private final String URL_JSON;
	private ExpireDateListener listener;
	public interface ExpireDateListener
	{
		void onExpireDate(String expire_date);
		void onDeviceNotMatch();
		void onAuthFailed();
		void onError();
	}

	public ExpireDate (String URL_JSON,ExpireDateListener listener){
		this.URL_JSON = URL_JSON;
		this.listener = listener;
	}

	public void start() {
		execute(URL_JSON);
	}

	@Override
	protected String doInBackground(String[] p1) {
		try {
			StringBuilder sb = new StringBuilder();
			String url = p1[0];
			URL mURL = new URL(url);
			HttpURLConnection connection = (HttpURLConnection)mURL.openConnection();
			connection.setReadTimeout(60000);
			connection.setReadTimeout(60000);
			connection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
			connection.addRequestProperty("User-Agent", "Mozilla");
			connection.addRequestProperty("Referer", "google.com");
			InputStream input = connection.getInputStream();
			Reader reader = new BufferedReader(new InputStreamReader(input));
			char[] buf = new char[1024];
			while (true) {
				int read = reader.read(buf);
				if (read <= 0) {
					break;
				}
				sb.append(buf, 0, read);
			}
			int status = connection.getResponseCode();
			if(status == HttpURLConnection.HTTP_OK){
				return sb.toString();
			} else {
				StringBuilder sb1 = new StringBuilder();
				String newUrl = connection.getHeaderField("Location");
				String cookies = connection.getHeaderField("Set-Cookie");
				connection = (HttpURLConnection) new URL(newUrl).openConnection();
				connection.setReadTimeout(60000);
				connection.setReadTimeout(60000);
				connection.setRequestProperty("Cookie", cookies);
				connection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
				connection.addRequestProperty("User-Agent", "Mozilla");
				connection.addRequestProperty("Referer", "google.com");
				InputStream input1 = connection.getInputStream();
				Reader reader1 = new BufferedReader(new InputStreamReader(input1));
				char[] buf1 = new char[1024];
				while (true) {
					int read1 = reader1.read(buf1);
					if (read1 <= 0) {
						break;
					}
					sb1.append(buf1, 0, read1);
				}
			}
			return sb.toString();
		} catch (Exception e) {
			return "error " + e.getMessage();
		}
	}

	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		if (result != null) {
			if (result.startsWith("error")) {
				listener.onError();
			} else {
				try {
					JSONObject js = new JSONObject(result);
					if (js.has("auth"))if (!js.getBoolean("auth")) {
						listener.onAuthFailed();
						return;
					}
					if (js.getString("device_match").equals("none")) {
						listener.onAuthFailed();
						return;
					}
					if (js.getString("device_match").equals("false")) {
						listener.onDeviceNotMatch();
						return;
					}
					listener.onExpireDate(js.getString("expiry"));
				} catch (Exception e) {
					listener.onError();
				}
			}
		}
	}
}