package com.cbtunnel.plus.core.vpnutils;

import android.annotation.SuppressLint;
import android.content.Context;
import com.cbtunnel.plus.R;
import com.cbtunnel.plus.logger.hLogStatus;
import com.cbtunnel.plus.utils.FileUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import io.michaelrocks.paranoid.Obfuscate;

@Obfuscate
public class Pdnsd extends Thread {

    public final static String PDNSD_SERVER = "server {\n label= \"%1$s\";\n ip = %2$s;\n port = %3$d;\n uptest = none;\n }\n";
    private final static String PDNSD_BIN = "libpdnsd";

    private Process mProcess;
    private File filePdnsd;

    private final Context mContext;
    private final String[] mDnsHosts;
    private final int mDnsPort;
    private final String mPdnsdHost;
    private final int mPdnsdPort;

    public Pdnsd(Context context, String[] dnsHosts, int dnsPort, String pdnsdHost, int pdnsdPort) {
        mContext = context;
        mDnsHosts = dnsHosts;
        mDnsPort = dnsPort;
        mPdnsdHost = pdnsdHost;
        mPdnsdPort = pdnsdPort;
    }

    @Override
    public void run() {
        try {
            String a = mContext.getApplicationInfo().nativeLibraryDir;
            filePdnsd = new File(a, PDNSD_BIN + ".so");

            File fileConf = makePdnsdConf(mContext.getFilesDir(), mDnsHosts, mDnsPort, mPdnsdHost, mPdnsdPort);

            String cmdString = filePdnsd.getCanonicalPath() + " -v9 -c " + fileConf;

            mProcess = Runtime.getRuntime().exec(cmdString);

            StreamGobbler.OnLineListener onLineListener = log -> android.util.Log.e("Pdnsd: ",log);

            StreamGobbler stdoutGobbler = new StreamGobbler(mProcess.getInputStream(), onLineListener);
            StreamGobbler stderrGobbler = new StreamGobbler(mProcess.getErrorStream(), onLineListener);
            stdoutGobbler.start();
            stderrGobbler.start();

            mProcess.waitFor();

        } catch (IOException e) {
            addLog("Pdnsd Error: " + e.getMessage());
        } catch(Exception e){
            hLogStatus.logDebug("Pdnsd Error: "+ e);
        }

    }

    @Override
    public synchronized void interrupt()
    {
        super.interrupt();

        if (mProcess != null)
            mProcess.destroy();

        try {
            if (filePdnsd != null)
                VpnUtils.killProcess(filePdnsd);
        } catch(Exception ignored) {
        }

        mProcess = null;
        filePdnsd = null;
    }

    @SuppressLint("DefaultLocale")
    private File makePdnsdConf(File fileDir, String[] dnsHosts, int dnsPort, String pdnsdHost, int pdnsdPort) throws IOException {
        String content = FileUtils.readFromRaw(mContext, R.raw.pdnsd_local);
        StringBuilder server_dns = new StringBuilder();
        for (int i = 0; i < dnsHosts.length; i++){
            String dnsHost = dnsHosts[i];
            server_dns.append(String.format(PDNSD_SERVER, "server" + i+1, dnsHost, dnsPort));
        }

        String conf = String.format(content, server_dns.toString(), fileDir.getCanonicalPath(), pdnsdHost, pdnsdPort);

        hLogStatus.logDebug("pdnsd conf:" + conf);

        File f = new File(fileDir,"pdnsd.conf");
        if (f.exists()) {
            f.delete();
        }
        saveTextFile(f, conf);
        File cache = new File(fileDir,"pdnsd.cache");
        if (!cache.exists()) {
            try {
                cache.createNewFile();
            } catch (Exception ignored) {
            }
        }
        return f;
    }

    private void saveTextFile(File file, String contents) {
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file, false);
            writer.write(contents);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void addLog(String log) {
        hLogStatus.logInfo(log);
    }

}
