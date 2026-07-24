package com.cbtunnel.plus.service;

import com.cbtunnel.plus.R;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.security.KeyChain;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.NonNull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import javax.crypto.Cipher;
import net.openvpn.openvpn.BuildConfig;
import net.openvpn.openvpn.CPUUsage;
import net.openvpn.openvpn.ClientAPI_Config;
import net.openvpn.openvpn.ClientAPI_ConnectionInfo;
import net.openvpn.openvpn.ClientAPI_DynamicChallenge;
import net.openvpn.openvpn.ClientAPI_EvalConfig;
import net.openvpn.openvpn.ClientAPI_Event;
import net.openvpn.openvpn.ClientAPI_ExternalPKICertRequest;
import net.openvpn.openvpn.ClientAPI_ExternalPKISignRequest;
import net.openvpn.openvpn.ClientAPI_InterfaceStats;
import net.openvpn.openvpn.ClientAPI_LLVector;
import net.openvpn.openvpn.ClientAPI_LogInfo;
import net.openvpn.openvpn.ClientAPI_MergeConfig;
import net.openvpn.openvpn.ClientAPI_OpenVPNClient;
import net.openvpn.openvpn.ClientAPI_ProvideCreds;
import net.openvpn.openvpn.ClientAPI_ServerEntry;
import net.openvpn.openvpn.ClientAPI_ServerEntryVector;
import net.openvpn.openvpn.ClientAPI_Status;
import net.openvpn.openvpn.ClientAPI_TransportStats;
import net.openvpn.openvpn.JellyBeanHack;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.cbtunnel.plus.config.ConfigUtil;
import com.cbtunnel.plus.config.SettingsConstants;
import com.cbtunnel.plus.core.ConfigParser;
import com.cbtunnel.plus.core.Connection;
import com.cbtunnel.plus.core.VpnProfile;
import com.cbtunnel.plus.core.vpnutils.VpnUtils;
import com.cbtunnel.plus.harliesApplication;
import com.cbtunnel.plus.logger.hLogStatus;
import com.cbtunnel.plus.thread.OpenVPNClientThread;
import com.cbtunnel.plus.utils.FileUtils;
import com.cbtunnel.plus.utils.PasswordUtil;
import com.cbtunnel.plus.utils.PrefUtil;
import com.cbtunnel.plus.utils.util;
import io.michaelrocks.paranoid.Obfuscate;

@Obfuscate
public class OpenVPNService extends VpnService implements VpnUtils.VPNProtectListener, SettingsConstants, Callback, OpenVPNClientThread.EventReceiver {
    public static final String ACTION_BASE = "net.openvpn.openvpn.";
    public static final String ACTION_BIND = "net.openvpn.openvpn.BIND";
    public static final String ACTION_CONNECT = "net.openvpn.openvpn.CONNECT";
    public static final String ACTION_PAUSE = "net.openvpn.openvpn.PAUSE_SERVICE";
    public static final String ACTION_RESUME = "net.openvpn.openvpn.RESUME_SERVICE";
    public static final String ACTION_RECONNECT = "net.openvpn.openvpn.RECONNECT_SERVICE";
    public static final String ACTION_DELETE_PROFILE = "net.openvpn.openvpn.DELETE_PROFILE";
    public static final String ACTION_DISCONNECT = "net.openvpn.openvpn.DISCONNECT";
    public static final String ACTION_IMPORT_PROFILE = "net.openvpn.openvpn.IMPORT_PROFILE";
    public static final String ACTION_REFRESH_PROFILE = "net.openvpn.openvpn.ADD_PROFILE";
    public static final String ACTION_IMPORT_PROFILE_VIA_PATH = "net.openvpn.openvpn.ACTION_IMPORT_PROFILE_VIA_PATH";
    public static final String ACTION_RENAME_PROFILE = "net.openvpn.openvpn.RENAME_PROFILE";
    public static final String ACTION_SUBMIT_PROXY_CREDS = "net.openvpn.openvpn.ACTION_SUBMIT_PROXY_CREDS";
    public static final int EV_PRIO_HIGH = 3;
    public static final int EV_PRIO_INVISIBLE = 0;
    public static final int EV_PRIO_LOW = 1;
    public static final int EV_PRIO_MED = 2;
    private static final int GCI_REQ_ESTABLISH = 0;
    public static final String INTENT_PREFIX = "net.openvpn.openvpn";
    private static final int MSG_EVENT = 1;
    private static final int MSG_LOG = 2;
    private static final String TAG = "OpenVPNService";
    public static final int log_deque_max = 250;
    private boolean active = false;
    private final ArrayDeque<EventReceiver> clients = new ArrayDeque();
    private CPUUsage cpu_usage;
    private Profile current_profile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private HashMap event_info;
    private JellyBeanHack jellyBeanHack;
    private EventMsg last_event;
    private EventMsg last_event_prof_manage;
    private final ArrayDeque<LogMsg> log_deque = new ArrayDeque();
    private final IBinder mBinder = new LocalBinder();
    private Handler mHandler;
    private OpenVPNClientThread mThread;
    private PrefUtil prefs;
    private ProfileList profile_list;
    private PasswordUtil pwds;
    private boolean shutdown_pending = false;
    private long thread_started = 0;
    private ConfigUtil mConfig;
    private String str;
    private SharedPreferences mPref;
    private Object[] objArr;
    private static boolean mReconnecting = true;

    @Override
    public boolean protectSocket(Socket socket) {
        return protect(socket);
    }

    public interface EventReceiver {
        void event(EventMsg eventMsg);
        void log(LogMsg logMsg);
    }

    public static class Challenge {
        private String challenge;
        private boolean echo;
        private boolean response_required;

        public String get_challenge() {
            return this.challenge;
        }

        public boolean get_echo() {
            return this.echo;
        }

        public boolean get_response_required() {
            return this.response_required;
        }

        public String toString() {
            Object[] objArr = new Object[OpenVPNService.EV_PRIO_HIGH];
            objArr[OpenVPNService.GCI_REQ_ESTABLISH] = this.challenge;
            objArr[OpenVPNService.MSG_EVENT] = Boolean.valueOf(this.echo);
            objArr[OpenVPNService.MSG_LOG] = Boolean.valueOf(this.response_required);
            return String.format("%s/%b/%b", objArr);
        }
    }
    public enum Transition {
        NO_CHANGE,
        TO_CONNECTED,
        TO_DISCONNECTED
    }
    public static class ConnectionStats {
        public long bytes_in;
        public long bytes_out;
        public int duration;
        public int last_packet_received;
    }

    private static class DynamicChallenge {
        public Challenge challenge;
        public String cookie;
        public long expires;

        private DynamicChallenge() {
            this.challenge = new Challenge();
        }

        public boolean is_expired() {
            return SystemClock.elapsedRealtime() > this.expires;
        }

        public long expire_delay() {
            return this.expires - SystemClock.elapsedRealtime();
        }

        public String toString() {
            Object[] objArr = new Object[OpenVPNService.EV_PRIO_HIGH];
            objArr[OpenVPNService.GCI_REQ_ESTABLISH] = this.challenge.toString();
            objArr[OpenVPNService.MSG_EVENT] = this.cookie;
            objArr[OpenVPNService.MSG_LOG] = Long.valueOf(this.expires);
            return String.format("%s/%s/%s", objArr);
        }
    }

    private static class EventInfo {
        public int flags;
        public int icon_res_id;
        public int priority;
        public int progress;
        public int res_id;

        public EventInfo(int res_id_arg, int icon_res_id_arg, int progress_arg, int priority_arg, int flags_arg) {
            this.res_id = res_id_arg;
            this.icon_res_id = icon_res_id_arg;
            this.progress = progress_arg;
            this.priority = priority_arg;
            this.flags = flags_arg;
        }
    }

    public static class EventMsg {
        public static final int F_ERROR = 1;
        public static final int F_EXCLUDE_SELF = 16;
        public static final int F_FROM_JAVA = 2;
        public static final int F_PROF_IMPORT = 32;
        public static final int F_PROF_MANAGE = 4;
        public static final int F_UI_RESET = 8;
        public ClientAPI_ConnectionInfo conn_info;
        public long expires = 0;
        public int flags = OpenVPNService.GCI_REQ_ESTABLISH;
        public int icon_res_id = -1;
        public String info;
        public String name;
        public int priority = F_ERROR;
        public String profile_override;
        public int progress = OpenVPNService.GCI_REQ_ESTABLISH;
        public int res_id = -1;
        public EventReceiver sender;
        public Transition transition = Transition.NO_CHANGE;

        public enum Transition {
            NO_CHANGE,
            TO_CONNECTED,
            TO_DISCONNECTED
        }

        public static EventMsg disconnected() {
            EventMsg e = new EventMsg();
            e.flags = F_FROM_JAVA;
            e.res_id = R.string.state_disconnected;
            e.icon_res_id = R.drawable.ic_cloud_off;
            e.name = "DISCONNECTED";
            e.info = BuildConfig.FLAVOR;
            return e;
        }

        public boolean is_expired() {
            if (this.expires != 0 && SystemClock.elapsedRealtime() > this.expires) {
                return true;
            }
            return false;
        }

        public boolean is_reflected(EventReceiver caller) {
            if (this.sender == null) {
                return false;
            }
            if ((this.flags & F_EXCLUDE_SELF) == 0 && this.sender == caller) {
                return false;
            }
            return true;
        }

        @SuppressLint("DefaultLocale")
        public String toStringFull() {
            return String.format("EVENT: name=%s info='%s' trans=%s flags=%d progress=%d prio=%d res=%d", new Object[]{this.name, this.info, this.transition, Integer.valueOf(this.flags), Integer.valueOf(this.progress), Integer.valueOf(this.priority), Integer.valueOf(this.res_id)});
        }

        /*public void mReconnect() {
            if (!mReconnecting && HTTPInjectorThread.f!=200 && HarlieService.isVPNRunning()) {
                new Handler().postDelayed(() -> {
                    hLogStatus.clearLog();
                    harliesApplication.getApp().startService(new Intent(harliesApplication.getApp(),HarlieService.class).setAction(HarlieService.RESTART_SERVICE));
                }, 1000);
            }
        }

        public Handler reconnectHandler;
        public Runnable reconnectRunnable;*/

        @NonNull
        public String toString() {
            StringBuffer buffer = new StringBuffer();
            Object[] objArr = new Object[F_ERROR];
            objArr[OpenVPNService.GCI_REQ_ESTABLISH] = this.name;
            buffer.append(String.format("EVENT: %s", objArr));
            if (this.info.length() > 0) {
                objArr = new Object[F_ERROR];
                objArr[OpenVPNService.GCI_REQ_ESTABLISH] = this.info;
                buffer.append(String.format(" info='%s'", objArr));
            }
            if (this.transition != Transition.NO_CHANGE) {
                objArr = new Object[F_ERROR];
                objArr[OpenVPNService.GCI_REQ_ESTABLISH] = this.transition;
                buffer.append(String.format(" trans=%s", objArr));
            }
            /*String stateMsg = harliesApplication.getApp().getString(hLogStatus.getLocalizedState(hLogStatus.getLastState()));
            if (reconnectHandler == null) {
                reconnectHandler = new Handler();
            }
            if (reconnectRunnable == null) {
                reconnectRunnable = this::mReconnect;
            }
            if (String.format("%s", objArr).equals(new String(new byte[]{87, 65, 73, 84}))) {
                mReconnecting = false;
                reconnectHandler.postDelayed(reconnectRunnable, 9000);
            } else {
                mReconnecting = true;
                reconnectHandler.removeCallbacks(reconnectRunnable);
            }
            if (!HarlieService.isVPNRunning() || stateMsg.equals(harliesApplication.getApp().getResources().getString(R.string.state_reconnecting))){
                mReconnecting = true;
                reconnectHandler.removeCallbacks(reconnectRunnable);
            }*/
            return buffer.toString();
        }
    }

    public static class InternalError extends RuntimeException {
    }

    public class LocalBinder extends Binder {
        public OpenVPNService getService() {
            return OpenVPNService.this;
        }
    }

    public static class LogMsg {
        String line;
    }

    public class Profile {
        private boolean allow_password_save;
        private boolean autologin;
        private DynamicChallenge dynamic_challenge;
        private String errorText;
        private boolean external_pki;
        private String external_pki_alias;
        public String location;
        private String name;
        public String orig_filename;
        private boolean private_key_password_required;
        private ServerList server_list;
        private Challenge static_challenge;
        private String userlocked_username;

        private Profile(String location_arg, String filename, boolean filename_is_url_encoded_profile_name, ClientAPI_EvalConfig ec) {
            this.location = location_arg;
            this.orig_filename = filename;
            if (filename_is_url_encoded_profile_name) {
                this.name = filename;
                if (ProfileFN.has_ovpn_ext(this.name)) {
                    this.name = ProfileFN.strip_ovpn_ext(this.name);
                }
                try {
                    this.name = URLDecoder.decode(this.name, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.e(OpenVPNService.TAG, "UnsupportedEncodingException when decoding profile filename", e);
                }
            } else {
                this.name = filename;
            }
            if (ec.getError()) {
                this.errorText = ec.getMessage();
                return;
            }
            this.userlocked_username = ec.getUserlockedUsername();
            this.autologin = ec.getAutologin();
            this.external_pki = ec.getExternalPki();
            this.private_key_password_required = ec.getPrivateKeyPasswordRequired();
            this.allow_password_save = ec.getAllowPasswordSave();
            String cs = ec.getStaticChallenge();
            if (cs.length() > 0) {
                Challenge chal = new Challenge();
                chal.challenge = cs;
                chal.echo = ec.getStaticChallengeEcho();
                chal.response_required = true;
                this.static_challenge = chal;
            }
            if (!filename_is_url_encoded_profile_name) {
                String profile_name = ec.getProfileName();
                String friendly_name = ec.getFriendlyName();
                String loc = null;
                if (!(this.location == null || this.location.equals("imported"))) {
                    loc = this.location;
                }
                boolean is_friendly = false;
                String f1 = profile_name;
                if (friendly_name.length() > 0) {
                    f1 = friendly_name;
                    is_friendly = true;
                }
                String f2 = filename;
                if (f2 != null && f2.equalsIgnoreCase("client.ovpn")) {
                    f2 = null;
                }
                if (ProfileFN.has_ovpn_ext(f2)) {
                    f2 = ProfileFN.strip_ovpn_ext(f2);
                }
                if (!(f2 == null || f1 == null || !f2.equals(f1))) {
                    f2 = null;
                }
                StringBuffer n = new StringBuffer();
                n.append(f1);
                if (this.autologin && !is_friendly && f2 == null) {
                    n.append(OpenVPNService.this.getText(R.string.autologin_suffix).toString());
                }
                if (!(loc == null && f2 == null)) {
                    n.append(" [");
                    if (loc != null) {
                        n.append(loc);
                        n.append(":");
                    }
                    if (f2 != null) {
                        n.append(f2);
                    }
                    n.append("]");
                }
                this.name = n.toString();
            }
            this.server_list = new ServerList();
            ClientAPI_ServerEntryVector sev = ec.getServerList();
            int n2 = (int) sev.size();
            for (int i = OpenVPNService.GCI_REQ_ESTABLISH; i < n2; i += OpenVPNService.MSG_EVENT) {
                ClientAPI_ServerEntry se = sev.get(i);
                ServerEntry e2 = new ServerEntry();
                e2.server = se.getServer();
                e2.friendly_name = se.getFriendlyName();
                this.server_list.list.add(e2);
            }
            this.external_pki_alias = OpenVPNService.this.prefs.get_string_by_profile(this.name, "epki_alias");
        }

        public String get_location() {
            return this.location;
        }

        public String get_filename() {
            if (this.location != null && this.location.equals("bundled")) {
                return this.orig_filename;
            }
            String ret = ProfileFN.encode_profile_fn(this.name);
            return ret == null ? this.orig_filename : ret;
        }

        public String get_error() {
            return this.errorText;
        }

        public String get_type_string() {
            if (get_autologin()) {
                return OpenVPNService.this.getText(R.string.profile_type_autologin).toString();
            }
            if (get_epki()) {
                return OpenVPNService.this.getText(R.string.profile_type_epki).toString();
            }
            return OpenVPNService.this.getText(R.string.profile_type_standard).toString();
        }

        public String get_name() {
            return this.name;
        }

        public String get_userlocked_username() {
            return this.userlocked_username;
        }

        public boolean get_autologin() {
            return this.autologin;
        }

        public ServerList get_server_list() {
            return this.server_list;
        }

        public boolean server_list_defined() {
            return this.server_list.list.size() > 0;
        }

        public boolean userlocked_username_defined() {
            return this.userlocked_username.length() > 0;
        }

        public boolean need_external_pki_alias() {
            return this.external_pki && this.external_pki_alias == null;
        }

        public boolean have_external_pki_alias() {
            return this.external_pki && this.external_pki_alias != null;
        }

        public boolean get_private_key_password_required() {
            return this.private_key_password_required;
        }

        public boolean get_allow_password_save() {
            return this.allow_password_save;
        }

        public boolean is_deleteable() {
            return (this.location == null || this.location.equals("bundled")) ? false : true;
        }

        public boolean is_renameable() {
            return is_deleteable();
        }


        public boolean is_dynamic_challenge() {
            expire_dynamic_challenge();
            return this.dynamic_challenge != null;
        }

        public long get_dynamic_challenge_expire_delay() {
            if (is_dynamic_challenge()) {
                return this.dynamic_challenge.expire_delay();
            }
            return 0;
        }

        public boolean challenge_defined() {
            expire_dynamic_challenge();
            return (this.static_challenge == null && this.dynamic_challenge == null) ? false : true;
        }

        public Challenge get_challenge() {
            expire_dynamic_challenge();
            if (this.dynamic_challenge != null) {
                return this.dynamic_challenge.challenge;
            }
            return this.static_challenge;
        }

        public void reset_dynamic_challenge() {
            this.dynamic_challenge = null;
        }

        private void expire_dynamic_challenge() {
            if (this.dynamic_challenge != null && this.dynamic_challenge.is_expired()) {
                this.dynamic_challenge = null;
            }
        }

        private boolean get_epki() {
            return this.external_pki;
        }

        private String get_epki_alias() {
            return this.external_pki_alias;
        }

        private void persist_epki_alias(String epki_alias) {
            this.external_pki_alias = epki_alias;
            OpenVPNService.this.prefs.set_string_by_profile(this.name, "epki_alias", epki_alias);
            OpenVPNService.this.jellyBeanHackPurge();
        }

        private void invalidate_epki_alias(String epki_alias) {
            if (this.external_pki_alias != null && this.external_pki_alias.equals(epki_alias)) {
                this.external_pki_alias = null;
                OpenVPNService.this.prefs.delete_key_by_profile(this.name, "epki_alias");
                OpenVPNService.this.jellyBeanHackPurge();
            }
        }

        public void forget_cert() {
            if (this.external_pki_alias != null) {
                this.external_pki_alias = null;
                OpenVPNService.this.prefs.delete_key_by_profile(this.name, "epki_alias");
                OpenVPNService.this.jellyBeanHackPurge();
            }
        }

        public String toString() {
            String str = "Profile name='%s' ofn='%s' userlock=%s auto=%b epki=%b/%s sl=%s sc=%s dc=%s";
            Object[] objArr = new Object[9];
            objArr[OpenVPNService.GCI_REQ_ESTABLISH] = this.name;
            objArr[OpenVPNService.MSG_EVENT] = this.orig_filename;
            objArr[OpenVPNService.MSG_LOG] = this.userlocked_username;
            objArr[OpenVPNService.EV_PRIO_HIGH] = Boolean.valueOf(this.autologin);
            objArr[4] = Boolean.valueOf(this.external_pki);
            objArr[5] = this.external_pki_alias;
            objArr[6] = this.server_list.toString();
            objArr[7] = this.static_challenge != null ? this.static_challenge.toString() : "null";
            objArr[8] = this.dynamic_challenge != null ? this.dynamic_challenge.toString() : "null";
            return String.format(str, objArr);
        }
    }

    public class MergedProfile extends Profile {
        public String profile_content;

        private MergedProfile(String location_arg, String filename, boolean filename_is_url_encoded_profile_name, ClientAPI_EvalConfig ec) {
            super(location_arg, filename, filename_is_url_encoded_profile_name, ec);
        }
    }

    private static class ProfileFN {
        private ProfileFN() {
        }

        public static boolean has_ovpn_ext(String fn) {
            if (fn == null) {
                return false;
            }
            if (fn.endsWith(".ovpn") || fn.endsWith(".OVPN")) {
                return true;
            }
            return false;
        }

        public static String strip_ovpn_ext(String fn) {
            if (fn == null || !has_ovpn_ext(fn)) {
                return fn;
            }
            return fn.substring(OpenVPNService.GCI_REQ_ESTABLISH, fn.length() - 5);
        }

        public static String encode_profile_fn(String name) {
            try {
                return URLEncoder.encode(name, "UTF-8") + ".ovpn";
            } catch (UnsupportedEncodingException e) {
                Log.e(OpenVPNService.TAG, "UnsupportedEncodingException when encoding profile filename", e);
                return null;
            }
        }
    }

    public class ProfileList extends ArrayList<Profile> {

        private class CustomComparator implements Comparator<Profile> {
            private CustomComparator() {
            }

            public int compare(Profile p1, Profile p2) {
                return p1.name.compareTo(p2.name);
            }
        }

        private void load_profiles(String location) throws IOException {
            String storage_title;
            String[] fnlist;
            boolean filename_is_url_encoded_profile_name;
            String str;
            Object[] objArr;
            if (location.equals("bundled")) {
                storage_title = "assets";
                fnlist = OpenVPNService.this.getResources().getAssets().list(BuildConfig.FLAVOR);
                filename_is_url_encoded_profile_name = true;
            } else {
                if (location.equals("imported")) {
                    storage_title = "app private storage";
                    fnlist = OpenVPNService.this.fileList();
                    filename_is_url_encoded_profile_name = true;
                } else {
                    throw new InternalError();
                }
            }
            int length = fnlist.length;
            for (int i = OpenVPNService.GCI_REQ_ESTABLISH; i < length; i += OpenVPNService.MSG_EVENT) {
                String fn = fnlist[i];
                if (ProfileFN.has_ovpn_ext(fn)) {
                    try {
                        String profile_content = OpenVPNService.this.read_file(location, fn);
                        ClientAPI_Config config = new ClientAPI_Config();
                        config.setContent(profile_content);
                        ClientAPI_EvalConfig ec = ClientAPI_OpenVPNClient.eval_config_static(config);
                        if (ec.getError()) {
                            str = OpenVPNService.TAG;
                            objArr = new Object[OpenVPNService.MSG_LOG];
                            objArr[OpenVPNService.GCI_REQ_ESTABLISH] = fn;
                            objArr[OpenVPNService.MSG_EVENT] = ec.getMessage();
                            Log.i(str, String.format("PROFILE: error evaluating %s: %s", objArr));
                        } else {
                            add(new Profile(location, fn, filename_is_url_encoded_profile_name, ec));
                        }
                    } catch (IOException e) {
                        try {
                            str = OpenVPNService.TAG;
                            objArr = new Object[OpenVPNService.MSG_LOG];
                            objArr[OpenVPNService.GCI_REQ_ESTABLISH] = fn;
                            objArr[OpenVPNService.MSG_EVENT] = storage_title;
                            Log.i(str, String.format("PROFILE: error reading %s from %s", objArr));
                        } catch (Exception e2) {
                            Log.e(OpenVPNService.TAG, "PROFILE: error enumerating assets", e2);
                            return;
                        }
                    }
                }
            }
        }

        public String[] profile_names() {
            String[] ret = new String[size()];
            for (int i = OpenVPNService.GCI_REQ_ESTABLISH; i < size(); i += OpenVPNService.MSG_EVENT) {
                ret[i] = ((Profile) get(i)).name;
            }
            return ret;
        }

        public Profile get_profile_by_name(String name) {
            if (name != null) {
                Iterator it = iterator();
                while (it.hasNext()) {
                    Profile prof = (Profile) it.next();
                    if (name.equals(prof.name)) {
                        return prof;
                    }
                }
            }
            return null;
        }

        public void forget_certs() {
            OpenVPNService.this.jellyBeanHackPurge();
            Iterator it = iterator();
            while (it.hasNext()) {
                ((Profile) it.next()).forget_cert();
            }
        }

        private void invalidate_epki_alias(String epki_alias) {
            Iterator it = iterator();
            while (it.hasNext()) {
                ((Profile) it.next()).invalidate_epki_alias(epki_alias);
            }
        }

        private void sort() {
            Collections.sort(this, new CustomComparator());
        }
    }


    public static class ServerEntry {
        private String friendly_name;
        private String server;

        public String display_name() {
            if (this.friendly_name.length() > 0) {
                return this.friendly_name;
            }
            return this.server;
        }

        public String toString() {
            Object[] objArr = new Object[OpenVPNService.MSG_LOG];
            objArr[OpenVPNService.GCI_REQ_ESTABLISH] = this.server;
            objArr[OpenVPNService.MSG_EVENT] = this.friendly_name;
            return String.format("%s/%s", objArr);
        }
    }

    public static class ServerList {
        private ArrayList<ServerEntry> list = new ArrayList();

        public String[] display_names() {
            int size = this.list.size();
            String[] ret = new String[size];
            for (int i = OpenVPNService.GCI_REQ_ESTABLISH; i < size; i += OpenVPNService.MSG_EVENT) {
                ret[i] = ((ServerEntry) this.list.get(i)).display_name();
            }
            return ret;
        }

        public String toString() {
            StringBuffer buffer = new StringBuffer();
            Iterator it = this.list.iterator();
            while (it.hasNext()) {
                buffer.append(((ServerEntry) it.next()).toString() + ",");
            }
            return buffer.toString();
        }
    }

    private class TunBuilder extends VpnService.Builder implements OpenVPNClientThread.TunBuilder {
        private TunBuilder() {
            super();
        }

        public boolean tun_builder_set_remote_address(String address, boolean ipv6) {
            try {
                String str = OpenVPNService.TAG;
                Object[] objArr = new Object[OpenVPNService.MSG_LOG];
                objArr[OpenVPNService.GCI_REQ_ESTABLISH] = address;
                objArr[OpenVPNService.MSG_EVENT] = Boolean.valueOf(ipv6);
                Log.d(str, String.format("BUILDER: set_remote_address %s ipv6=%b", objArr));
                return true;
            } catch (Exception e) {
                log_error("tun_builder_set_remote_address", e);
                return false;
            }
        }

        public boolean tun_builder_add_address(String address, int prefix_length, String gateway, boolean ipv6, boolean net30) {
            try {
                Log.d(OpenVPNService.TAG, String.format("BUILDER: add_address %s/%d %s ipv6=%b net30=%b", new Object[]{address, Integer.valueOf(prefix_length), gateway, Boolean.valueOf(ipv6), Boolean.valueOf(net30)}));
                addAddress(address, prefix_length);
                return true;
            } catch (Exception e) {
                log_error("tun_builder_add_address", e);
                return false;
            }
        }

        public boolean tun_builder_reroute_gw(boolean ipv4, boolean ipv6, long flags) {
            try {
                String str = OpenVPNService.TAG;
                Object[] objArr = new Object[OpenVPNService.EV_PRIO_HIGH];
                objArr[OpenVPNService.GCI_REQ_ESTABLISH] = Boolean.valueOf(ipv4);
                objArr[OpenVPNService.MSG_EVENT] = Boolean.valueOf(ipv6);
                objArr[OpenVPNService.MSG_LOG] = Long.valueOf(flags);
                Log.d(str, String.format("BUILDER: reroute_gw ipv4=%b ipv6=%b flags=%d", objArr));
                if ((65536 & flags) != 0) {
                    return true;
                }
                if (ipv4) {
                    addRoute("0.0.0.0", OpenVPNService.GCI_REQ_ESTABLISH);
                }
                if (!ipv6) {
                    return true;
                }
                addRoute("::", OpenVPNService.GCI_REQ_ESTABLISH);
                return true;
            } catch (Exception e) {
                log_error("tun_builder_add_route", e);
                return false;
            }
        }

        public boolean tun_builder_add_route(String address, int prefix_length, boolean ipv6) {
            try {
                String str = OpenVPNService.TAG;
                Object[] objArr = new Object[OpenVPNService.EV_PRIO_HIGH];
                objArr[OpenVPNService.GCI_REQ_ESTABLISH] = address;
                objArr[OpenVPNService.MSG_EVENT] = Integer.valueOf(prefix_length);
                objArr[OpenVPNService.MSG_LOG] = Boolean.valueOf(ipv6);
                Log.d(str, String.format("BUILDER: add_route %s/%d ipv6=%b", objArr));
                addRoute(address, prefix_length);
                return true;
            } catch (Exception e) {
                log_error("tun_builder_add_route", e);
                return false;
            }
        }

        public boolean tun_builder_exclude_route(String address, int prefix_length, boolean ipv6) {
            try {
                String str = OpenVPNService.TAG;
                Object[] objArr = new Object[OpenVPNService.EV_PRIO_HIGH];
                objArr[OpenVPNService.GCI_REQ_ESTABLISH] = address;
                objArr[OpenVPNService.MSG_EVENT] = Integer.valueOf(prefix_length);
                objArr[OpenVPNService.MSG_LOG] = Boolean.valueOf(ipv6);
                Log.d(str, String.format("BUILDER: exclude_route %s/%d ipv6=%b (NOT IMPLEMENTED)", objArr));
                return true;
            } catch (Exception e) {
                log_error("tun_builder_exclude_route", e);
                return false;
            }
        }

        public boolean tun_builder_add_dns_server(String address, boolean ipv6) {
            try {
                String str = OpenVPNService.TAG;
                Object[] objArr = new Object[OpenVPNService.MSG_LOG];
                objArr[OpenVPNService.GCI_REQ_ESTABLISH] = address;
                objArr[OpenVPNService.MSG_EVENT] = Boolean.valueOf(ipv6);
                Log.d(str, String.format("BUILDER: add_dns_server %s ipv6=%b", objArr));
                addDnsServer(address);
                return true;
            } catch (Exception e) {
                log_error("tun_builder_add_dns_server", e);
                return false;
            }
        }

        public boolean tun_builder_add_search_domain(String domain) {
            try {
                String str = OpenVPNService.TAG;
                Object[] objArr = new Object[OpenVPNService.MSG_EVENT];
                objArr[OpenVPNService.GCI_REQ_ESTABLISH] = domain;
                Log.d(str, String.format("BUILDER: add_search_domain %s", objArr));
                addSearchDomain(domain);
                return true;
            } catch (Exception e) {
                log_error("tun_builder_add_search_domain", e);
                return false;
            }
        }

        public boolean tun_builder_set_mtu(int mtu) {
            try {
                String str = OpenVPNService.TAG;
                Object[] objArr = new Object[OpenVPNService.MSG_EVENT];
                objArr[OpenVPNService.GCI_REQ_ESTABLISH] = Integer.valueOf(mtu);
                Log.d(str, String.format("BUILDER: set_mtu %d", objArr));
                setMtu(mtu);
                return true;
            } catch (Exception e) {
                log_error("tun_builder_set_mtu", e);
                return false;
            }
        }

        public boolean tun_builder_set_session_name(String name) {
            try {
                String str = OpenVPNService.TAG;
                Object[] objArr = new Object[OpenVPNService.MSG_EVENT];
                objArr[OpenVPNService.GCI_REQ_ESTABLISH] = name;
                Log.d(str, String.format("BUILDER: set_session_name %s", objArr));
                if (mConfig.getIsFilterApps()) {
                    for (String app_pacote : mConfig.getPackageFilterApps()) {
                        try {
                            if (mConfig.getIsFilterBypassMode()) {
                                addDisallowedApplication(app_pacote);
                                addLogInfo(String.format("VPN disabled for<font color = #FF9600> %s", app_pacote));
                            } else {
                                addAllowedApplication(app_pacote);
                                addLogInfo(String.format("VPN enabled for<font color = #FF9600> %s", app_pacote));
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            addLogInfo("App " + app_pacote + " not found. Apps filter will not work, check settings.");
                        }
                    }
                }
                setSession(String.format("%s - %s", getString(R.string.app_name), mConfig.getServerName()));
                return true;
            } catch (Exception e) {
                log_error("tun_builder_set_session_name", e);
                return false;
            }
        }

        public int tun_builder_establish() {
            try {
                Log.d(OpenVPNService.TAG, "BUILDER: establish");
                PendingIntent pi = ConfigUtil.getPendingIntent(OpenVPNService.this);
                if (pi != null) {
                    setConfigureIntent(pi);
                }
                return establish().detachFd();
            } catch (Exception e) {
                log_error("tun_builder_establish", e);
                return -1;
            }
        }

        public void tun_builder_teardown(boolean disconnect) {
            try {
                String str = OpenVPNService.TAG;
                Object[] objArr = new Object[OpenVPNService.MSG_EVENT];
                objArr[OpenVPNService.GCI_REQ_ESTABLISH] = Boolean.valueOf(disconnect);
                Log.d(str, String.format("BUILDER: teardown disconnect=%b", objArr));
            } catch (Exception e) {
                log_error("tun_builder_teardown", e);
            }
        }

        private void log_error(String meth_name, Exception e) {
            String str = OpenVPNService.TAG;
            Object[] objArr = new Object[OpenVPNService.MSG_LOG];
            objArr[OpenVPNService.GCI_REQ_ESTABLISH] = meth_name;
            objArr[OpenVPNService.MSG_EVENT] = e.toString();
            Log.d(str, String.format("BUILDER_ERROR: %s %s", objArr));
        }
    }

    static {
        System.loadLibrary("ovpncli");
        ClientAPI_OpenVPNClient.init_process();
        Log.d(TAG, ClientAPI_OpenVPNClient.crypto_self_test());
    }

    public ArrayDeque<LogMsg> log_history() {
        return this.log_deque;
    }


    public void jellyBeanHackPurge() {
        if (this.jellyBeanHack != null) {
            this.jellyBeanHack.resetPrivateKey();
        }
    }

    private void crypto_self_test() {
        String st = ClientAPI_OpenVPNClient.crypto_self_test();
        if (st.length() > 0) {
            String str = TAG;
            Object[] objArr = new Object[MSG_EVENT];
            objArr[GCI_REQ_ESTABLISH] = st;
            Log.d(str, String.format("SERV: crypto_self_test\n%s", objArr));
        }
    }

    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SERV: Service onCreate called");
        crypto_self_test();
        this.mHandler = new Handler(this);
        populate_event_info_map();
        this.prefs = new PrefUtil(harliesApplication.getDefaultSharedPreferences());
        this.pwds = new PasswordUtil(harliesApplication.getDefaultSharedPreferences());
        this.jellyBeanHack = JellyBeanHack.newJellyBeanHack();
        mConfig = ConfigUtil.getInstance(this);
        mPref = harliesApplication.getPrivateSharedPreferences();
    }

    public int onStartCommand(Intent intent, int flags, int startId)  {
        if (intent == null || intent.getAction() == null) {
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        if (action != null) {
            String prefix = INTENT_PREFIX;
            String str = TAG;
            Object[] objArr = new Object[MSG_EVENT];
            objArr[GCI_REQ_ESTABLISH] = action;
            Log.d(str, String.format("SERV: onStartCommand action=%s", objArr));
            mConfig = ConfigUtil.getInstance(this);
            mPref = harliesApplication.getPrivateSharedPreferences();
            switch (action) {
                case ACTION_CONNECT:
                    VpnUtils.setVPNProtectListener(this);
                    connect_action(prefix, intent, false);
                    break;
                case ACTION_DISCONNECT:
                    try {
                        disconnect_action(prefix, intent);
                    } catch (Exception ignored) {}
                    break;
                case ACTION_IMPORT_PROFILE:
                    import_profile_action(prefix, intent);
                    break;
                case ACTION_IMPORT_PROFILE_VIA_PATH:
                    import_profile_via_path_action(prefix, intent);
                    break;
                case ACTION_DELETE_PROFILE:
                    try {
                        delete_profile_action(prefix, intent);
                    } catch (IOException ig) {addLogInfo(ig.toString());}
                    break;
                case ACTION_RENAME_PROFILE:
                    try {
                        rename_profile_action(prefix, intent);
                    } catch (IOException ig) {addLogInfo(ig.toString());}
                    break;
                case ACTION_REFRESH_PROFILE:
                    try {
                        if(reload_profile_action(prefix, intent)){
                            refresh_profile_list();
                        }
                    } catch (IOException ig) {addLogInfo(ig.toString());}
                    break;
                case ACTION_PAUSE:
                    if (hLogStatus.isTunnelActive()) {
                        OpenVPNService.this.network_pause();
                    }
                    break;
                case ACTION_RESUME:
                    if (hLogStatus.isTunnelActive()) {
                        OpenVPNService.this.network_resume();
                    }
                    break;
                case ACTION_RECONNECT:
                    if (intent.getStringExtra(prefix + ".SECONDS")!=null){
                        int second = Integer.parseInt(intent.getStringExtra(prefix + ".SECOND"));
                        if (hLogStatus.isTunnelActive()) {
                            OpenVPNService.this.network_reconnect(second);
                        }
                        break;
                    }
                    if (hLogStatus.isTunnelActive()) {
                        OpenVPNService.this.network_reconnect(0);
                    }
                    break;
            }
        }
        return START_STICKY;
    }

    private void disconnect_action(String prefix, Intent intent) {
        boolean stop = intent.getBooleanExtra(prefix + ".STOP", false);
        stop_thread();
        if (stop) {
            stopSelf();
        }
    }

    private boolean reload_profile_action(String prefix, Intent intent) {
        return reload_profile_action(intent.getStringExtra(prefix + ".CONTENT"));
    }

    private String cert(JSONObject server,String server_ip,String server_port) throws JSONException {
        boolean isCustomConfig = server.getBoolean("MultiCert");
        if (isCustomConfig){
            String multi = FileUtils.showJson(server.getString("ovpnCertificate"));
            if (multi.contains("remote ")){
                return multi;
            }
            return multi + "\nremote " + server_ip + " " + server_port + "\n";
        }
        String single = FileUtils.showJson(mPref.getString(OPEN_VPN_CERT, ""));
        if (single.contains("remote ")){
            return single;
        }
        return single + "\nremote " + server_ip + " " + server_port + "\n";
    }
    private boolean reload_profile_action(String profile_content) {
        try {
            for (File f : getFilesDir().listFiles()) {
                if (f.getAbsolutePath().toLowerCase().endsWith(".ovpn")) {
                    f.delete();
                }
            }
            JSONArray s_js = new JSONArray(profile_content);
            for (int i = 0; i < s_js.length(); i++) {
                JSONObject server = s_js.getJSONObject(i);
                String server_name = server.getString("Name");
                String server_ip = FileUtils.showJson(server.getString("ServerIP"));
                String server_port = server.getString("TcpPort").contains(":") ? server.getString("TcpPort").split(":")[0] : server.getString("TcpPort");
                ConfigParser parser = new ConfigParser();
                parser.parseConfig(new StringReader(cert(server,server_ip,server_port)));
                VpnProfile vp = parser.convertProfile();
                Connection mConnection = vp.mConnections[0];
                mConnection.mServerName = server_ip;
                mConnection.mServerPort = server_port;
                mConnection.mUseCustomConfig = true;
                String encoded_name = String.format("%s.ovpn", URLEncoder.encode(server_name, "UTF-8"));
                String config = vp.getConfigFile(OpenVPNService.this, true);
                File dir = new File(getFilesDir(), encoded_name);
                OutputStream out = new FileOutputStream(dir);
                out.write(config.getBytes());
                out.flush();
                out.close();
            }
            return true;
        } catch (Exception e) {
            hLogStatus.logInfo("Profile error! Profile import failed: "+e);
            util.showToast("Profile import failed!",e.toString());
            return false;
        }
    }

    private boolean import_profile_via_path_action(String prefix, Intent intent) {
        ClientAPI_MergeConfig mc = ClientAPI_OpenVPNClient.merge_config_static(intent.getStringExtra(prefix + ".PATH"), true);
        String status = "PROFILE_" + mc.getStatus();
        if (status.equals("PROFILE_MERGE_SUCCESS")) {
            return import_profile(mc.getProfileContent(), mc.getBasename(), false);
        }
        gen_event(MSG_EVENT, status, mc.getErrorText());
        return false;
    }

    private boolean import_profile_action(String prefix, Intent intent) {
        return import_profile(intent.getStringExtra(prefix + ".CONTENT"), intent.getStringExtra(prefix + ".FILENAME"), intent.getBooleanExtra(prefix + ".MERGE", false));
    }

    private boolean import_profile(String profile_content, String filename, boolean merge) {
        if (ProfileFN.has_ovpn_ext(filename) && FileUtils.dirname(filename) == null) {
            if (merge) {
                ClientAPI_MergeConfig mc = ClientAPI_OpenVPNClient.merge_config_string_static(profile_content);
                String status = "PROFILE_" + mc.getStatus();
                if (status.equals("PROFILE_MERGE_SUCCESS")) {
                    profile_content = mc.getProfileContent();
                } else {
                    gen_event(MSG_EVENT, status, mc.getErrorText());
                    return false;
                }
            }
            ClientAPI_Config config = new ClientAPI_Config();
            config.setContent(profile_content);
            ClientAPI_EvalConfig ec = ClientAPI_OpenVPNClient.eval_config_static(config);
            if (ec.getError()) {
                Object[] objArr = new Object[MSG_LOG];
                objArr[GCI_REQ_ESTABLISH] = filename;
                objArr[MSG_EVENT] = ec.getMessage();
                gen_event(MSG_EVENT, "PROFILE_PARSE_ERROR", String.format("%s : %s", objArr));
                return false;
            }
            Profile prof = new Profile("imported", filename, false, ec);
            try {
                FileUtils.writeFileAppPrivate(this, prof.get_filename(), profile_content);
                String profile_name = prof.get_name();
                this.pwds.remove("auth", profile_name);
                this.pwds.remove("pk", profile_name);
                refresh_profile_list();
                gen_event(GCI_REQ_ESTABLISH, "PROFILE_IMPORT_SUCCESS", profile_name, profile_name);
                return true;
            } catch (IOException e) {
                gen_event(MSG_EVENT, "PROFILE_WRITE_ERROR", filename);
                return false;
            }
        }
        gen_event(MSG_EVENT, "PROFILE_FILENAME_ERROR", filename);
        return false;
    }

    private boolean delete_profile_action(String prefix, Intent intent) throws IOException {
        String profile_name = intent.getStringExtra(prefix + ".PROFILE");
        get_profile_list();
        Profile profile = this.profile_list.get_profile_by_name(profile_name);
        if (profile == null) {
            return false;
        }
        if (profile.is_deleteable()) {
            if (this.active && profile == this.current_profile) {
                stop_thread();
            }
            if (deleteFile(profile.get_filename())) {
                this.pwds.remove("auth", profile_name);
                this.pwds.remove("pk", profile_name);
                refresh_profile_list();
                gen_event(GCI_REQ_ESTABLISH, "PROFILE_DELETE_SUCCESS", profile.get_name());
                return true;
            }
            gen_event(MSG_EVENT, "PROFILE_DELETE_FAILED", profile.get_name());
            return false;
        }
        gen_event(MSG_EVENT, "PROFILE_DELETE_FAILED", profile_name);
        return false;
    }

    private boolean rename_profile_action(String prefix, Intent intent) throws IOException {
        String profile_name = intent.getStringExtra(prefix + ".PROFILE");
        String new_profile_name = intent.getStringExtra(prefix + ".NEW_PROFILE");
        get_profile_list();
        Profile profile = this.profile_list.get_profile_by_name(profile_name);
        if (profile == null) {
            return false;
        }
        if (!profile.is_renameable() || new_profile_name == null || new_profile_name.length() == 0) {
            Log.d(TAG, "PROFILE_RENAME_FAILED: rename preliminary checks");
            gen_event(MSG_EVENT, "PROFILE_RENAME_FAILED", profile_name);
            return false;
        }
        File dir = getFilesDir();
        Object[] objArr = new Object[MSG_LOG];
        objArr[GCI_REQ_ESTABLISH] = dir.getPath();
        objArr[MSG_EVENT] = profile.orig_filename;
        String from_path = String.format("%s/%s", objArr);
        objArr = new Object[MSG_LOG];
        objArr[GCI_REQ_ESTABLISH] = dir.getPath();
        objArr[MSG_EVENT] = ProfileFN.encode_profile_fn(new_profile_name);
        String to_path = String.format("%s/%s", objArr);
        if (FileUtils.renameFile(from_path, to_path)) {
            refresh_profile_list();
            Profile new_profile = this.profile_list.get_profile_by_name(new_profile_name);
            if (new_profile == null) {
                Log.d(TAG, "PROFILE_RENAME_FAILED: post-rename profile get");
                gen_event(MSG_EVENT, "PROFILE_RENAME_FAILED", profile_name);
                return false;
            }
            this.pwds.remove("auth", profile_name);
            this.pwds.remove("pk", profile_name);
            gen_event(GCI_REQ_ESTABLISH, "PROFILE_RENAME_SUCCESS", new_profile.get_name(), new_profile.get_name());
            return true;
        }
        String str = TAG;
        Object[] objArr2 = new Object[MSG_LOG];
        objArr2[GCI_REQ_ESTABLISH] = from_path;
        objArr2[MSG_EVENT] = to_path;
        Log.d(str, String.format("PROFILE_RENAME_FAILED: rename operation from='%s' to='%s'", objArr2));
        gen_event(MSG_EVENT, "PROFILE_RENAME_FAILED", profile_name);
        return false;
    }

    private Profile locate_profile(String profile_name) throws IOException {
        get_profile_list();
        Profile profile = this.profile_list.get_profile_by_name(profile_name);
        if (profile != null) {
            return profile;
        }
        gen_event(MSG_EVENT, "PROFILE_NOT_FOUND", profile_name);
        return null;
    }


    private boolean connect_action(final String prefix, final Intent intent, final boolean proxy_retry) {
        if (this.active) {
            stop_thread();
            new Handler(OpenVPNService.this).postDelayed(() -> {
                try {
                    OpenVPNService.this.do_connect_action(prefix, intent, proxy_retry);
                } catch (IOException e) {addLogInfo(e.toString());}
            }, 2000);
        } else {
            try {
                do_connect_action(prefix, intent, proxy_retry);
            } catch (IOException e) {
                addLogInfo(e.toString());
            }
        }
        return true;
    }

    private boolean do_connect_action(String prefix, Intent intent, boolean proxy_retry) throws IOException {
        String profile_name = intent.getStringExtra(prefix + ".PROFILE");
        String gui_version = intent.getStringExtra(prefix + ".GUI_VERSION");
        String server = intent.getStringExtra(prefix + ".SERVER");
        String proto = intent.getStringExtra(prefix + ".PROTO");
        String ipv6 = intent.getStringExtra(prefix + ".IPv6");
        String conn_timeout = intent.getStringExtra(prefix + ".CONN_TIMEOUT");
        String username = intent.getStringExtra(prefix + ".USERNAME");
        String password = intent.getStringExtra(prefix + ".PASSWORD");
        boolean cache_password = intent.getBooleanExtra(prefix + ".CACHE_PASSWORD", false);
        String pk_password = intent.getStringExtra(prefix + ".PK_PASSWORD");
        String response = intent.getStringExtra(prefix + ".RESPONSE");
        String epki_alias = intent.getStringExtra(prefix + ".EPKI_ALIAS");
        String compression_mode = intent.getStringExtra(prefix + ".COMPRESSION_MODE");
        password = util.pw_repl(username, password);
        Profile profile = locate_profile(profile_name);
        if (profile == null) {
            return false;
        }
        hLogStatus.updateStateString(hLogStatus.VPN_GET_CONFIG, getResources().getString(R.string.state_get_config));
        addLogInfo(getResources().getString(R.string.state_get_config));
        String location = profile.get_location();
        String filename = profile.get_filename();
        String port = mConfig.getSecureString(SERVER_PORT_KEY);
        try {
            String profile_content = read_file(location, filename);
            profile_content = profile_content.replace(getPort(profile_content), port);
            String remote = getIpOrHost(profile_content);
            profile_content = profile_content.replace(remote, mConfig.getSecureString(SERVER_KEY));
            String str = TAG;
            Object[] objArr = new Object[MSG_EVENT];
            objArr[GCI_REQ_ESTABLISH] = Integer.valueOf(profile_content.length());
            Log.d(str, String.format("SERV: profile file len=%d", objArr));
            if (mConfig.getPayloadType() == PAYLOAD_TYPE_OVPN_UDP) {
                profile_content = profile_content.replace(getPort(profile_content), "53");
                if (profile_content.contains("proto tcp")) {
                    profile_content = profile_content.replace("proto tcp", "proto udp");
                }
                if (profile_content.contains(" tcp-client")) {
                    profile_content = profile_content.replace(" tcp-client", "");
                }
            }
            return start_connection(profile, profile_content, gui_version, server, proto, ipv6, conn_timeout, username, password, cache_password, pk_password, response, epki_alias, compression_mode);
        } catch (IOException e) {
            Object[] objArr2 = new Object[MSG_LOG];
            objArr2[GCI_REQ_ESTABLISH] = location;
            objArr2[MSG_EVENT] = filename;
            gen_event(MSG_EVENT, "PROFILE_NOT_FOUND", String.format("%s/%s", objArr2));
            addLogInfo(e.toString());
            return false;
        }
    }

    private String getIpOrHost(String config) {
        String[] split = config.split("\n");
        for (String line : split) {
            if (line.toLowerCase().contains("remote ")) {
                String[] split_line = line.split(" ");
                return split_line[1];
            }
        }
        return "";
    }
    private String getPort(String config) {
        String[] split = config.split("\n");
        for (String line : split) {
            if (line.toLowerCase().contains("remote ")) {
                String[] split_line = line.split(" ");
                return split_line[2];
            }
        }
        return "";
    }

    private boolean start_connection(Profile profile, String profile_content, String gui_version, String server, String proto, String ipv6, String conn_timeout, String username, String password, boolean cache_password, String pk_password, String response, String epki_alias, String compression_mode) {
        if (this.active) {
            return false;
        }
        OpenVPNClientThread thread = new OpenVPNClientThread();
        ClientAPI_Config config = new ClientAPI_Config();
        profile_content += mConfig.getProxySetups();
        config.setContent(profile_content);
        config.setInfo(true);
        if (server != null) {
            config.setServerOverride(server);
        }
        if (proto != null) {
            config.setProtoOverride(proto);
        }
        if (ipv6 != null) {
            config.setIpv6(ipv6);
        }
        if (conn_timeout != null) {
            int ct = GCI_REQ_ESTABLISH;
            try {
                ct = Integer.parseInt(conn_timeout);
            } catch (NumberFormatException ignored) {
            }
            config.setConnTimeout(ct);
        }
        if (compression_mode != null) {
            config.setCompressionMode(compression_mode);
        }
        if (pk_password != null) {
            config.setPrivateKeyPassword(pk_password);
        }
        boolean tun_persist = this.prefs.get_boolean("tun_persist", false);
        if (tun_persist && VERSION.SDK_INT == 19) {
            Log.i(TAG, "Seamless Tunnel disabled for KitKat 4.4 - 4.4.2");
            tun_persist = false;
        }
        config.setTunPersist(tun_persist);
        config.setGoogleDnsFallback(this.prefs.get_boolean("google_dns_fallback", false));
        config.setForceAesCbcCiphersuites(this.prefs.get_boolean("force_aes_cbc_ciphersuites_v2", false));
        config.setAltProxy(this.prefs.get_boolean("alt_proxy", false));
        String tls_version_min_override = this.prefs.get_string("tls_version_min_override");
        if (tls_version_min_override != null) {
            config.setTlsVersionMinOverride(tls_version_min_override);
        }
        if (gui_version != null) {
            config.setGuiVersion(gui_version);
        }
        if (profile.get_epki()) {
            if (epki_alias != null) {
                profile.persist_epki_alias(epki_alias);
            } else {
                epki_alias = profile.get_epki_alias();
            }
            if (epki_alias != null) {
                if (epki_alias.equals("DISABLE_CLIENT_CERT")) {
                    config.setDisableClientCert(true);
                } else {
                    config.setExternalPkiAlias(epki_alias);
                }
            }
        }
        ClientAPI_EvalConfig ec = thread.eval_config(config);
        if (ec.getError()) {
            gen_event(MSG_EVENT, "CONFIG_FILE_PARSE_ERROR", ec.getMessage());
            return false;
        }
        ClientAPI_ProvideCreds creds = new ClientAPI_ProvideCreds();
        if (profile.is_dynamic_challenge()) {
            if (response != null) {
                creds.setResponse(response);
            }
            creds.setDynamicChallengeCookie(profile.dynamic_challenge.cookie);
            profile.reset_dynamic_challenge();
        } else if (ec.getAutologin() || username == null || username.length() != 0) {
            if (username != null) {
                creds.setUsername(username);
            }
            if (password != null) {
                creds.setPassword(password);
            }
            if (response != null) {
                creds.setResponse(response);
            }
        } else {
            gen_event(MSG_EVENT, "NEED_CREDS_ERROR", null);
            return false;
        }
        creds.setCachePassword(cache_password);
        creds.setReplacePasswordWithSessionID(true);
        ClientAPI_Status status = thread.provide_creds(creds);
        if (status.getError()) {
            gen_event(MSG_EVENT, "CREDS_ERROR", status.getMessage());
            return false;
        }
        String str = TAG;
        String str2 = "SERV: CONNECT prof=%s user=%s proxy=%s serv=%s proto=%s ipv6=%s to=%s resp=%s epki_alias=%s comp=%s";
        Object[] objArr = new Object[10];
        objArr[GCI_REQ_ESTABLISH] = profile.name;
        objArr[MSG_EVENT] = username;
        objArr[EV_PRIO_HIGH] = server;
        objArr[4] = proto;
        objArr[5] = ipv6;
        objArr[6] = conn_timeout;
        objArr[7] = response;
        objArr[8] = epki_alias;
        objArr[9] = compression_mode;
        Log.i(str, String.format(str2, objArr));
        this.current_profile = profile;
        set_autostart_profile_name(profile.get_name());
        gen_event(GCI_REQ_ESTABLISH, "CORE_THREAD_ACTIVE", null);
        thread.connect(this);
        this.mThread = thread;
        this.thread_started = SystemClock.elapsedRealtime();
        this.cpu_usage = new CPUUsage();
        this.active = true;
        return true;
    }



    public IBinder onBind(Intent intent) {
        if (intent == null || !intent.getAction().equals(ACTION_BIND)) {
            String str = TAG;
            Object[] objArr = new Object[MSG_EVENT];
            objArr[GCI_REQ_ESTABLISH] = intent;
            Log.d(str, String.format("SERV: onBind SUPER intent=%s", objArr));
            return super.onBind(intent);
        }
        str = TAG;
        objArr = new Object[MSG_EVENT];
        objArr[GCI_REQ_ESTABLISH] = intent;
        Log.d(str, String.format("SERV: onBind intent=%s", objArr));
        return this.mBinder;
    }

    public void client_attach(EventReceiver evr) {
        this.clients.remove(evr);
        this.clients.addFirst(evr);
        String str = TAG;
        Object[] objArr = new Object[MSG_EVENT];
        objArr[GCI_REQ_ESTABLISH] = Integer.valueOf(this.clients.size());
        Log.d(str, String.format("SERV: client attach n_clients=%d", objArr));
    }

    public void client_detach(EventReceiver evr) {
        this.clients.remove(evr);
        String str = TAG;
        Object[] objArr = new Object[MSG_EVENT];
        objArr[GCI_REQ_ESTABLISH] = Integer.valueOf(this.clients.size());
        Log.d(str, String.format("SERV: client detach n_clients=%d", objArr));
    }

    public void refresh_profile_list() throws IOException {
        ProfileList pl = new ProfileList();
        pl.load_profiles("bundled");
        pl.load_profiles("imported");
        pl.sort();
        Log.d(TAG, "SERV: refresh profiles:");
        Iterator it = pl.iterator();
        while (it.hasNext()) {
            Profile p = (Profile) it.next();
            String str = TAG;
            Object[] objArr = new Object[MSG_EVENT];
            objArr[GCI_REQ_ESTABLISH] = p.toString();
            Log.d(str, String.format("SERV: %s", objArr));
        }
        this.profile_list = pl;
    }

    public Profile get_current_profile() throws IOException {
        if (this.current_profile != null) {
            return this.current_profile;
        }
        ProfileList pl = get_profile_list();
        if (pl.size() >= MSG_EVENT) {
            return pl.get(GCI_REQ_ESTABLISH);
        }
        return null;
    }

    public ProfileList get_profile_list() throws IOException {
        if (this.profile_list == null) {
            refresh_profile_list();
        }
        return this.profile_list;
    }

    public static long max_profile_size() {
        return (long) ClientAPI_OpenVPNClient.max_profile_size();
    }

    public MergedProfile merge_parse_profile(String basename, String profile_content) {
        if (basename == null || profile_content == null) {
            return null;
        }
        ClientAPI_MergeConfig mc = ClientAPI_OpenVPNClient.merge_config_string_static(profile_content);
        String status = "PROFILE_" + mc.getStatus();
        if (status.equals("PROFILE_MERGE_SUCCESS")) {
            String merged_content = mc.getProfileContent();
            ClientAPI_Config config = new ClientAPI_Config();
            config.setContent(merged_content);
            MergedProfile mp = new MergedProfile("imported", basename, false, ClientAPI_OpenVPNClient.eval_config_static(config));
            mp.profile_content = merged_content;
            return mp;
        }
        ClientAPI_EvalConfig ec = new ClientAPI_EvalConfig();
        EventInfo evi = (EventInfo) this.event_info.get(status);
        if (evi != null) {
            status = resString(evi.res_id);
        }
        ec.setError(true);
        ec.setMessage(status + " : " + mc.getErrorText());
        return new MergedProfile("imported", basename, false, ec);
    }

    public void gen_ui_reset_event(boolean exclude_self, EventReceiver cli) {
        int flags = GCI_REQ_ESTABLISH;
        if (exclude_self) {
            flags = GCI_REQ_ESTABLISH | 16;
        }
        gen_event(flags, "UI_RESET", null, null, cli);
    }

    public void gen_proxy_context_expired_event() {
        gen_event(GCI_REQ_ESTABLISH, "PROXY_CONTEXT_EXPIRED", null);
    }

    public boolean is_active() {
        return this.active;
    }

    public EventMsg get_last_event() {
        if (this.last_event == null || this.last_event.is_expired()) {
            return null;
        }
        return this.last_event;
    }

    public EventMsg get_last_event_prof_manage() {
        if (this.last_event_prof_manage == null || this.last_event_prof_manage.is_expired()) {
            return null;
        }
        return this.last_event_prof_manage;
    }

    public void network_pause() {
        if (this.active) {
            hLogStatus.logInfo(resString(R.string.state_pause));
            this.mThread.pause(BuildConfig.FLAVOR);
        }
    }

    public void network_resume() {
        if (this.active) {
            hLogStatus.logInfo(resString(R.string.state_resume));
            this.mThread.resume();
        }
    }

    public void network_reconnect(int seconds) {
        if (this.active && util.isNetworkAvailable(this)) {
            hLogStatus.logInfo(resString(R.string.state_reconnecting));
            this.mThread.reconnect(seconds);
        }
    }

    public ConnectionStats get_connection_stats() {
        ConnectionStats cs = new ConnectionStats();
        ClientAPI_TransportStats stats = this.mThread.transport_stats();
        cs.last_packet_received = -1;
        if (this.active) {
            cs.duration = ((int) (SystemClock.elapsedRealtime() - this.thread_started)) / 1000;
            if (cs.duration < 0) {
                cs.duration = GCI_REQ_ESTABLISH;
            }
            cs.bytes_in = stats.getBytesIn();
            cs.bytes_out = stats.getBytesOut();
            int lpr_bms = stats.getLastPacketReceived();
            if (lpr_bms >= 0) {
                cs.last_packet_received = lpr_bms >> 10;
            }
        } else {
            cs.duration = GCI_REQ_ESTABLISH;
            cs.bytes_in = 0;
            cs.bytes_out = 0;
        }
        return cs;
    }

    public long get_tunnel_bytes_per_cpu_second() {
        if (this.cpu_usage != null) {
            double cpu_seconds = this.cpu_usage.usage();
            if (cpu_seconds > 0.0d) {
                ClientAPI_InterfaceStats stats = this.mThread.tun_stats();
                return (long) (((double) (stats.getBytesIn() + stats.getBytesOut())) / cpu_seconds);
            }
        }
        return 0;
    }


    private void stop_thread() {
        if (this.active) {
            this.mThread.stop();
            this.mThread.wait_thread_short();
            Log.d(TAG, "SERV: stop_thread succeeded");
        }
    }

    public boolean onUnbind(Intent intent) {
        String str = TAG;
        Object[] objArr = new Object[MSG_EVENT];
        objArr[GCI_REQ_ESTABLISH] = intent.toString();
        Log.d(str, String.format("SERV: onUnbind called intent=%s", objArr));
        return super.onUnbind(intent);
    }

    public void onDestroy() {
        Log.d(TAG, "SERV: onDestroy called");
        this.shutdown_pending = true;
        stop_thread();
        super.onDestroy();
    }

    public void onRevoke() {
        Log.d(TAG, "SERV: onRevoke called");
        stop_thread();
    }

    private void gen_event(int flags, String name, String extra_info) {
        gen_event(flags, name, extra_info, null, null);
    }

    private void gen_event(int flags, String name, String extra_info, String profile_override) {
        gen_event(flags, name, extra_info, profile_override, null);
    }

    private void gen_event(int flags, String name, String extra_info, String profile_override, EventReceiver sender) {
        EventInfo evi = (EventInfo) this.event_info.get(name);
        EventMsg evm = new EventMsg();
        evm.flags = flags | MSG_LOG;
        if (evi != null) {
            evm.progress = evi.progress;
            evm.priority = evi.priority;
            evm.res_id = evi.res_id;
            evm.icon_res_id = evi.icon_res_id;
            evm.sender = sender;
            evm.flags |= evi.flags;
        } else {
            evm.res_id = R.string.state_unknown;
        }
        evm.name = name;
        if (extra_info != null) {
            evm.info = extra_info;
        } else {
            evm.info = BuildConfig.FLAVOR;
        }
        if ((evm.flags & 4) != 0) {
            evm.expires = SystemClock.elapsedRealtime() + 60000;
        }
        evm.profile_override = profile_override;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(MSG_EVENT, evm));
    }

    @SuppressLint("DefaultLocale")
    public boolean handleMessage(Message msg) {
        EventMsg lastev = get_last_event();
        switch (msg.what) {
            case MSG_EVENT :
                EventMsg evm = (OpenVPNService.EventMsg) msg.obj;
                if (evm.res_id == R.string.state_auth_failed) {
                    addLogInfo("<font color = #ff0000>Failed to authenticate, username or password expired");
                    hLogStatus.updateStateString(hLogStatus.VPN_AUTH_FAILED, resString(R.string.state_auth_failed));
                    startService(new Intent(OpenVPNService.this, HarlieService.class).setAction(HarlieService.STOP_SERVICE).putExtra("stateSTOP_SERVICE",getResources().getString(R.string.state_auth_failed)));
                    if (this.current_profile != null) {
                        this.current_profile.get_name();
                        break;
                    }
                    break;
                } else if (evm.res_id ==  R.string.state_connected){
                    if (this.current_profile != null) {
                        hLogStatus.logInfo("Starting VPN Service");
                        hLogStatus.updateStateString(hLogStatus.VPN_CONNECTED, resString(R.string.state_connected));
                        hLogStatus.logInfo("<font color = #68B86B><strong>" + "VPNService Connected" + "</strong>");
                        break;
                    }
                } else if (evm.res_id == R.string.state_stopping) {
                    if (this.cpu_usage != null) {
                        this.cpu_usage.stop();
                    }
                    if (!this.shutdown_pending) {
                        set_autostart_profile_name(null);
                        break;
                    }
                    break;
                } else if (evm.res_id == R.string.state_disconnected) {
                    if (lastev != null) {
                        if ((lastev.flags & MSG_EVENT) != 0) {
                            evm.priority = GCI_REQ_ESTABLISH;
                        }
                    }
                    break;
                } else if (evm.res_id == R.string.dynamic_challenge){
                    if (this.current_profile != null) {
                        ClientAPI_DynamicChallenge dcsrc = new ClientAPI_DynamicChallenge();
                        if (ClientAPI_OpenVPNClient.parse_dynamic_challenge(evm.info, dcsrc)) {
                            DynamicChallenge dc = new DynamicChallenge();
                            dc.expires = SystemClock.elapsedRealtime() + 60000;
                            dc.cookie = evm.info;
                            dc.challenge.challenge = dcsrc.getChallenge();
                            dc.challenge.echo = dcsrc.getEcho();
                            dc.challenge.response_required = dcsrc.getResponseRequired();
                            this.current_profile.dynamic_challenge = dc;
                            evm.info = BuildConfig.FLAVOR;
                            break;
                        }
                    }
                    break;
                } else if (evm.res_id == R.string.pem_password_fail) {
                    evm.info = BuildConfig.FLAVOR;
                    if (this.current_profile != null) {
                        this.current_profile.get_name();
                        break;
                    }
                    break;
                }
                if (evm.res_id == R.string.epki_invalid_alias && this.profile_list != null) {
                    this.profile_list.invalidate_epki_alias(evm.info);
                }
                if ((evm.flags & 4) != 0) {
                    this.last_event_prof_manage = evm;
                } else if (evm.priority >= MSG_LOG) {
                    this.last_event = evm;
                }
                String msg_str = null;
                if (evm.res_id != R.string.ui_reset) {
                    msg_str = evm.toString();
                }
                if (msg_str != null) {
                    Log.i(TAG, msg_str);
                }
                if (evm.res_id == R.string.state_starting) {
                    log_message("<font color = #FF9600><b>OVPN Tunnel: </b>starting");
                }
                if (msg_str != null) {
                    log_message(msg_str);
                }
                if (evm.res_id == R.string.state_stopping) {
                    Object[] objArr = new Object[MSG_EVENT];
                    objArr[GCI_REQ_ESTABLISH] = Long.valueOf(get_tunnel_bytes_per_cpu_second());
                    log_message(String.format("Tunnel bytes per CPU second: %d", objArr));
                }
                if (evm.res_id == R.string.state_auth) {
                    hLogStatus.updateStateString(hLogStatus.VPN_AUTHENTICATING, resString(R.string.state_auth));
                }
                if (evm.res_id == R.string.state_add_routes) {
                    hLogStatus.updateStateString(hLogStatus.VPN_ADD_ROUTES, resString(R.string.state_add_routes));
                }
                if (evm.res_id == R.string.state_assign_ip) {
                    hLogStatus.updateStateString(hLogStatus.VPN_ASSIGN_IP, resString(R.string.state_assign_ip));
                }
                if (evm.res_id == R.string.state_get_config) {
                    hLogStatus.updateStateString(hLogStatus.VPN_GET_CONFIG, resString(R.string.state_get_config));
                }
                if (evm.res_id == R.string.state_nonetwork) {
                    hLogStatus.updateStateString(hLogStatus.VPN_NO_NETWORK, resString(R.string.state_nonetwork));
                }
                if (evm.res_id == R.string.state_wait) {
                    hLogStatus.updateStateString(hLogStatus.VPN_WAITING, resString(R.string.state_wait));
                }
                Iterator it = this.clients.iterator();
                while (it.hasNext()) {
                    EventReceiver cli = (EventReceiver) it.next();
                    if ((evm.flags & 16) == 0 || cli != evm.sender) {
                        cli.event(evm);
                    }
                }
                break;
            case MSG_LOG /*2*/:
                LogMsg lm = (LogMsg) msg.obj;
                String str = TAG;
                Object[] objArr2 = new Object[MSG_EVENT];
                objArr2[GCI_REQ_ESTABLISH] = lm.line;
                Log.i(str, String.format("LOG: %s", objArr2));
                log_message(lm);
                break;
            default:
                Log.d(TAG, "SERV: unhandled message");
                break;
        }
        return true;
    }

    private void log_message(String line) {
        LogMsg lm = new LogMsg();
        lm.line = line + "\n";
        log_message(lm);
    }

    private void log_message(LogMsg _msg) {
        String msg = _msg.line.trim();
        String x2 = "VERIFY OK";
        String x3 = "dhcp-option";
        String x4 = "TUN write";
        String x5 = "Certificate verification failed";
        String x6 = "Generated ";
        String x7 = "Connecting to [";
        String x8 = "Server Authentication";
        if (msg.contains("\n")) {
            String[] split = msg.split("\n");
            for (String str: split) {
                if(str.contains(x2)) {
                    addLogInfo(str);
                }
                if(str.contains(x3)) {
                    addLogInfo(str);
                }
                if(str.contains(x4)){
                    addLogInfo(str);
                }
                if(str.contains(x5)){
                    addLogInfo(str);
                }
                if(str.contains(x6)){
                    addLogInfo(str);
                }
               /* if(str.contains("Transport Error")){
                    if (this.active && util.isNetworkAvailable(OpenVPNService.this)){
                        hLogStatus.clearLog();
                        startService(new Intent(OpenVPNService.this,HarlieService.class).setAction(HarlieService.RESTART_SERVICE));
                    }
                }
                if(str.contains("Proxy Error")){
                    if (this.active && util.isNetworkAvailable(OpenVPNService.this)){
                        hLogStatus.clearLog();
                        startService(new Intent(OpenVPNService.this,HarlieService.class).setAction(HarlieService.RESTART_SERVICE));
                    }
                }*/
                if(str.contains("no TCP server")){
                    addLogInfo(str);
                }
                if(str.contains(x7)) {
                    addLogInfo("Connecting to " + str.split("127.0.0.1")[1]);
                }
                if(str.contains(x8)){
                    hLogStatus.updateStateString(hLogStatus.VPN_AUTHENTICATING, resString(R.string.state_auth));
                    addLogInfo(resString(R.string.state_auth));
                }
            }
        }
    }

    private void addLogInfo(String msg){
        String hst = mConfig.getSecureString(SERVER_KEY);
        String prx = mConfig.getSecureString(PROXY_IP_KEY);
        /*if (msg.equals("0")||msg.equals("")){
            if (util.isNetworkAvailable(OpenVPNService.this)){
                hLogStatus.clearLog();
                startService(new Intent(OpenVPNService.this,HarlieService.class).setAction(HarlieService.RESTART_SERVICE));
            }
        }*/
        if (!msg.contains(hst) || !msg.contains(prx)){
            hLogStatus.logInfo(msg);
        }
    }

    public boolean socket_protect(int socket) {
        boolean status = protect(socket);
        String str = TAG;
        Object[] objArr = new Object[MSG_LOG];
        objArr[GCI_REQ_ESTABLISH] = Integer.valueOf(socket);
        objArr[MSG_EVENT] = Boolean.valueOf(status);
        Log.d(str, String.format("SOCKET PROTECT: fd=%d protected status=%b", objArr));
        return status;
    }

    public boolean pause_on_connection_timeout() {
        boolean ret = hLogStatus.isTunnelActive();
        String str = TAG;
        Object[] objArr = new Object[MSG_EVENT];
        objArr[GCI_REQ_ESTABLISH] = Boolean.valueOf(ret);
        Log.d(str, String.format("pause_on_connection_timeout %b", objArr));
        return ret;
    }

    public OpenVPNClientThread.TunBuilder tun_builder_new() {
        return new TunBuilder();
    }

    public void event(ClientAPI_Event event) {
        EventMsg evm = new EventMsg();
        if (event.getError()) {
            evm.flags |= MSG_EVENT;
        }
        evm.name = event.getName();
        evm.info = event.getInfo();
        EventInfo evi = (EventInfo) this.event_info.get(evm.name);
        if (evi != null) {
            evm.progress = evi.progress;
            evm.priority = evi.priority;
            evm.res_id = evi.res_id;
            evm.icon_res_id = evi.icon_res_id;
            evm.flags |= evi.flags;
            if (evi.res_id == R.string.state_connected && this.mThread != null) {
                evm.conn_info = this.mThread.connection_info();
            }
        } else {
            evm.res_id = R.string.state_unknown;
            /*if (util.isNetworkAvailable(OpenVPNService.this) && HarlieService.isVPNRunning()){
                startService(new Intent(OpenVPNService.this, HarlieService.class).setAction(HarlieService.RECONNECT_SERVICE));
            }*/
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(MSG_EVENT, evm));
    }

    public void log(ClientAPI_LogInfo loginfo) {
        LogMsg lm = new LogMsg();
        lm.line = loginfo.getText();
        this.mHandler.sendMessage(this.mHandler.obtainMessage(MSG_LOG, lm));
    }

    private String cert_format_pem(X509Certificate cert) throws CertificateEncodingException {
        Object[] objArr = new Object[MSG_EVENT];
        objArr[GCI_REQ_ESTABLISH] = Base64.encodeToString(cert.getEncoded(), GCI_REQ_ESTABLISH);
        return String.format("-----BEGIN CERTIFICATE-----%n%s-----END CERTIFICATE-----%n", objArr);
    }

    public void external_pki_cert_request(ClientAPI_ExternalPKICertRequest req) {
        try {
            X509Certificate[] chain = KeyChain.getCertificateChain(this, req.getAlias());
            if (chain == null) {
                req.setError(true);
                req.setInvalidAlias(true);
            } else if (chain.length >= MSG_EVENT) {
                req.setCert(cert_format_pem(chain[GCI_REQ_ESTABLISH]));
                if (chain.length >= MSG_LOG) {
                    StringBuilder builder = new StringBuilder();
                    for (int i = MSG_EVENT; i < chain.length; i += MSG_EVENT) {
                        builder.append(cert_format_pem(chain[i]));
                    }
                    req.setSupportingChain(builder.toString());
                }
            } else {
                req.setError(true);
                req.setInvalidAlias(true);
                req.setErrorText(resString(R.string.epki_missing_cert));
            }
        } catch (Exception e) {
            Log.e(TAG, "EPKI error in external_pki_cert_request", e);
            req.setError(true);
            req.setInvalidAlias(true);
            req.setErrorText(e.toString());
        }
    }

    public void external_pki_sign_request(ClientAPI_ExternalPKISignRequest req) {
        try {
            String errfmt = "EPKI error in external_pki_sign_request: %s";
            byte[] data_bytes = Base64.decode(req.getData(), GCI_REQ_ESTABLISH);
            byte[] sig_bytes = null;
            PrivateKey pk;
            if (this.jellyBeanHack == null) {
                Log.d(TAG, "EPKI: normal mode");
                pk = KeyChain.getPrivateKey(this, req.getAlias());
                if (pk != null) {
                    Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
                    cipher.init(MSG_EVENT, pk);
                    sig_bytes = cipher.doFinal(data_bytes);
                } else {
                    req.setError(true);
                    req.setInvalidAlias(true);
                }
            } else {
                Log.d(TAG, "EPKI: Jelly bean mode");
                if (this.jellyBeanHack.enabled()) {
                    pk = this.jellyBeanHack.getPrivateKey(this, req.getAlias());
                    if (pk != null) {
                        sig_bytes = this.jellyBeanHack.rsaSign(pk, data_bytes);
                    } else {
                        req.setError(true);
                        req.setInvalidAlias(true);
                    }
                } else {
                    String err = "Android OpenSSL not accessible";
                    String str = TAG;
                    Object[] objArr = new Object[MSG_EVENT];
                    objArr[GCI_REQ_ESTABLISH] = err;
                    Log.e(str, String.format(errfmt, objArr));
                    req.setError(true);
                    req.setInvalidAlias(true);
                    req.setErrorText(err);
                    return;
                }
            }
            if (sig_bytes != null) {
                req.setSig(Base64.encodeToString(sig_bytes, MSG_LOG));
            }
        } catch (Exception e) {
            Log.e(TAG, "EPKI error in external_pki_sign_request", e);
            req.setError(true);
            req.setInvalidAlias(true);
            req.setErrorText(e.toString());
        }
    }

    public void done(ClientAPI_Status status) {
        boolean err = status.getError();
        String msg = status.getMessage();
        String str = TAG;
        Object[] objArr = new Object[MSG_LOG];
        objArr[GCI_REQ_ESTABLISH] = Boolean.valueOf(err);
        objArr[MSG_EVENT] = msg;
        Log.d(str, String.format("EXIT: connect() exited, err=%b, msg='%s'", objArr));
        log_stats();
        if (err) {
            if (msg == null || !msg.equals("CORE_THREAD_ABANDONED")) {
                String label = status.getStatus();
                if (label.length() == 0) {
                    label = "CORE_THREAD_ERROR";
                }
                gen_event(MSG_EVENT, label, msg);
            } else {
                gen_event(MSG_EVENT, "CORE_THREAD_ABANDONED", null);
            }
        }
        gen_event(GCI_REQ_ESTABLISH, "CORE_THREAD_INACTIVE", null);
        this.active = false;
    }

    public void set_autostart_profile_name(String profile_name) {
        if (profile_name != null) {
            this.prefs.set_string("autostart_profile_name", profile_name);
        } else {
            this.prefs.delete_key("autostart_profile_name");
        }
    }

    public static String[] stat_names() {
        int size = ClientAPI_OpenVPNClient.stats_n();
        String[] ret = new String[size];
        for (int i = GCI_REQ_ESTABLISH; i < size; i += MSG_EVENT) {
            ret[i] = ClientAPI_OpenVPNClient.stats_name(i);
        }
        return ret;
    }

    public ClientAPI_LLVector stat_values_full() {
        if (this.mThread != null) {
            return this.mThread.stats_bundle();
        }
        return null;
    }

    protected static Date get_app_expire() {
        int expire = ClientAPI_OpenVPNClient.app_expire();
        if (expire > 0) {
            return new Date(((long) expire) * 1000);
        }
        return null;
    }

    protected static String get_openvpn_core_platform() {
        return ClientAPI_OpenVPNClient.platform();
    }

    private void log_stats() {
        if (this.active) {
            String[] sn = stat_names();
            ClientAPI_LLVector sv = stat_values_full();
            if (sv != null) {
                for (int i = GCI_REQ_ESTABLISH; i < sn.length; i += MSG_EVENT) {
                    String name = sn[i];
                    long value = sv.get(i);
                    if (value > 0) {
                        String str = TAG;
                        Object[] objArr = new Object[MSG_LOG];
                        objArr[GCI_REQ_ESTABLISH] = name;
                        objArr[MSG_EVENT] = Long.valueOf(value);
                        Log.i(str, String.format("STAT %s=%s", objArr));
                    }
                }
            }
        }
    }

    public String read_file(String location, String filename) throws IOException {
        if (location.equals("bundled")) {
            return FileUtils.readAsset(this, filename);
        }
        if (location.equals("imported")) {
            return FileUtils.readFileAppPrivate(this, filename);
        }
        throw new InternalError();
    }

    private String resString(int res_id) {
        return getResources().getString(res_id);
    }

    private void populate_event_info_map() {
        this.event_info = new HashMap<>();
        this.event_info.put("RECONNECTING", new EventInfo(R.string.state_reconnecting, R.drawable.ic_cloud_off, 20, MSG_LOG, GCI_REQ_ESTABLISH));
        this.event_info.put("RESOLVE", new EventInfo(R.string.state_resolve, R.drawable.ic_cloud_off, 30, MSG_EVENT, GCI_REQ_ESTABLISH));
        this.event_info.put("WAIT_PROXY", new EventInfo(R.string.wait_proxy, R.drawable.ic_cloud_off, 40, MSG_EVENT, GCI_REQ_ESTABLISH));
        this.event_info.put("WAIT", new EventInfo(R.string.state_wait, R.drawable.ic_cloud_off, 50, MSG_EVENT, GCI_REQ_ESTABLISH));
        this.event_info.put("CONNECTING", new EventInfo(R.string.state_connecting, R.drawable.ic_cloud_off, 60, MSG_EVENT, GCI_REQ_ESTABLISH));
        this.event_info.put("GET_CONFIG", new EventInfo(R.string.state_get_config, R.drawable.ic_cloud_off, 70, MSG_EVENT, GCI_REQ_ESTABLISH));
        this.event_info.put("ASSIGN_IP", new EventInfo(R.string.state_assign_ip, R.drawable.ic_cloud_off, 80, MSG_EVENT, GCI_REQ_ESTABLISH));
        this.event_info.put("ADD_ROUTES", new EventInfo(R.string.state_add_routes, R.drawable.ic_cloud_off, 90, MSG_EVENT, GCI_REQ_ESTABLISH));
        this.event_info.put("CONNECTED", new EventInfo(R.string.state_connected, R.drawable.ic_cloud_on, 100, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("DISCONNECTED", new EventInfo(R.string.state_disconnected, R.drawable.ic_cloud_off, GCI_REQ_ESTABLISH, MSG_LOG, GCI_REQ_ESTABLISH));
        this.event_info.put("AUTH_FAILED", new EventInfo(R.string.state_auth_failed, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("PEM_PASSWORD_FAIL", new EventInfo(R.string.pem_password_fail, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("CERT_VERIFY_FAIL", new EventInfo(R.string.cert_verify_fail, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("TLS_VERSION_MIN", new EventInfo(R.string.tls_version_min, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("DYNAMIC_CHALLENGE", new EventInfo(R.string.dynamic_challenge, R.drawable.ic_error, GCI_REQ_ESTABLISH, MSG_LOG, GCI_REQ_ESTABLISH));
        this.event_info.put("TUN_SETUP_FAILED", new EventInfo(R.string.tun_setup_failed, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("TUN_IFACE_CREATE", new EventInfo(R.string.tun_iface_create, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("TAP_NOT_SUPPORTED", new EventInfo(R.string.tap_not_supported, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("PROFILE_NOT_FOUND", new EventInfo(R.string.profile_not_found, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("CONFIG_FILE_PARSE_ERROR", new EventInfo(R.string.config_file_parse_error, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("NEED_CREDS_ERROR", new EventInfo(R.string.need_creds_error, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("CREDS_ERROR", new EventInfo(R.string.creds_error, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("CONNECTION_TIMEOUT", new EventInfo(R.string.state_connection_timeout, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("INACTIVE_TIMEOUT", new EventInfo(R.string.inactive_timeout, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("INFO", new EventInfo(R.string.info_msg, R.drawable.ic_error, GCI_REQ_ESTABLISH, GCI_REQ_ESTABLISH, GCI_REQ_ESTABLISH));
        this.event_info.put("WARN", new EventInfo(R.string.warn_msg, R.drawable.ic_error, GCI_REQ_ESTABLISH, GCI_REQ_ESTABLISH, GCI_REQ_ESTABLISH));
        this.event_info.put("PROXY_NEED_CREDS", new EventInfo(R.string.proxy_need_creds, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("PROXY_ERROR", new EventInfo(R.string.proxy_error, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("PROXY_CONTEXT_EXPIRED", new EventInfo(R.string.proxy_context_expired, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("EPKI_ERROR", new EventInfo(R.string.epki_error, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("EPKI_INVALID_ALIAS", new EventInfo(R.string.epki_invalid_alias, R.drawable.ic_error, GCI_REQ_ESTABLISH, GCI_REQ_ESTABLISH, GCI_REQ_ESTABLISH));
        this.event_info.put("PAUSE", new EventInfo(R.string.state_pause, R.drawable.ic_cloud_off, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("RESUME", new EventInfo(R.string.state_resume, R.drawable.ic_error, GCI_REQ_ESTABLISH, MSG_LOG, GCI_REQ_ESTABLISH));
        this.event_info.put("CORE_THREAD_ACTIVE", new EventInfo(R.string.state_starting, R.drawable.ic_cloud_off, 10, MSG_EVENT, GCI_REQ_ESTABLISH));
        this.event_info.put("CORE_THREAD_INACTIVE", new EventInfo(R.string.state_stopping, -1, GCI_REQ_ESTABLISH, GCI_REQ_ESTABLISH, GCI_REQ_ESTABLISH));
        this.event_info.put("CORE_THREAD_ERROR", new EventInfo(R.string.core_thread_error, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("CORE_THREAD_ABANDONED", new EventInfo(R.string.core_thread_abandoned, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("CLIENT_HALT", new EventInfo(R.string.client_halt, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, GCI_REQ_ESTABLISH));
        this.event_info.put("CLIENT_RESTART", new EventInfo(R.string.client_restart, R.drawable.ic_cloud_off, GCI_REQ_ESTABLISH, MSG_LOG, GCI_REQ_ESTABLISH));
        this.event_info.put("PROFILE_PARSE_ERROR", new EventInfo(R.string.profile_parse_error, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, 4));
        this.event_info.put("PROFILE_CONFLICT", new EventInfo(R.string.profile_conflict, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, 4));
        this.event_info.put("PROFILE_WRITE_ERROR", new EventInfo(R.string.profile_write_error, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, 4));
        this.event_info.put("PROFILE_FILENAME_ERROR", new EventInfo(R.string.profile_filename_error, R.drawable.ic_error, GCI_REQ_ESTABLISH, EV_PRIO_HIGH, 4));
        this.event_info.put("PROFILE_MERGE_EXCEPTION", new EventInfo(R.string.profile_merge_exception, R.drawable.ic_error, GCI_REQ_ESTABLISH, MSG_LOG, 4));
        this.event_info.put("PROFILE_MERGE_OVPN_EXT_FAIL", new EventInfo(R.string.profile_merge_ovpn_ext_fail, R.drawable.ic_error, GCI_REQ_ESTABLISH, MSG_LOG, 4));
        this.event_info.put("PROFILE_MERGE_OVPN_FILE_FAIL", new EventInfo(R.string.profile_merge_ovpn_file_fail, R.drawable.ic_error, GCI_REQ_ESTABLISH, MSG_LOG, 4));
        this.event_info.put("PROFILE_MERGE_REF_FAIL", new EventInfo(R.string.profile_merge_ref_fail, R.drawable.ic_error, GCI_REQ_ESTABLISH, MSG_LOG, 4));
        this.event_info.put("PROFILE_MERGE_MULTIPLE_REF_FAIL", new EventInfo(R.string.profile_merge_multiple_ref_fail, R.drawable.ic_error, GCI_REQ_ESTABLISH, MSG_LOG, 4));
        this.event_info.put("UI_RESET", new EventInfo(R.string.ui_reset, R.drawable.ic_error, GCI_REQ_ESTABLISH, GCI_REQ_ESTABLISH, 8));
    }
}


