package com.cbtunnel.plus.thread;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import com.cbtunnel.plus.logger.hLogStatus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import io.michaelrocks.paranoid.Obfuscate;

@Obfuscate
public class checkUpdate {

    private final Listener listener;
    private final String URL_JSON;
    public interface Listener {
        void onCompleted(String config);
        void onError(String ex);
    }

    public checkUpdate(String mUrl,  Listener listener) {
        this.listener = listener;
        this.URL_JSON = mUrl;
    }

    public void start(){
        new FetchJSON().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class FetchJSON extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(URL_JSON).build();
                Response response = client.newCall(request).execute();
                return response.body().string();
            } catch (Exception e) {
                hLogStatus.logDebug(e.getMessage());
                return "error: "+e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if(result!=null){
                if (result.startsWith("error")) {
                    listener.onError(result);
                } else
                    listener.onCompleted(result);
            }
        }
    }

}

