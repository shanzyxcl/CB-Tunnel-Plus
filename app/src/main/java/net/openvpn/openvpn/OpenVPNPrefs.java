package net.openvpn.openvpn;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import java.util.Objects;
import com.cbtunnel.plus.R;
import com.cbtunnel.plus.activities.OpenVPNClientBase;
import com.cbtunnel.plus.harliesApplication;
import com.cbtunnel.plus.logger.hLogStatus;

public class OpenVPNPrefs extends OpenVPNClientBase {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Objects.requireNonNull(getConfig()).getColorAccent());
        setContentView(R.layout.ovpn_settings);
        Toolbar mToolbar = findViewById(R.id.toolbar_main);
        mToolbar.setTitle(resString(R.string.app_name));
        mToolbar.setSubtitle(resString(R.string.openvpn_settings));
        mToolbar.setBackgroundColor(getConfig().getColorAccent());
        mToolbar.setTitleTextColor(getConfig().getAppThemeUtil()? Color.BLACK:Color.WHITE);
        mToolbar.setSubtitleTextColor(getConfig().getAppThemeUtil()? Color.BLACK:Color.WHITE);
        mToolbar.setNavigationIcon(getConfig().getAppThemeUtil()? R.drawable.arrow_d:R.drawable.arrow_l);
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationOnClickListener(v -> finish());
        PreferenceFragmentCompat preference = new SettingsFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_settings, preference).commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private final String[] prefKeys = {"pref_vpn_proxy_address","vpn_proto","ipv6","conn_timeout","compression_mode","tls_version_min_override"};
        private final String[] prefSummary = {"127.0.0.1:8989","Select the VPN Protocol","IPv6 tunnel preference.","How long should VPN try to connect before giving up?","Tunnel compression options","Select the minimum SSL/TLS version for communication with the OpenVPN Server."};
        private String getPrefSummary(String key,String sam){
            if (key.equals(prefKeys[2])){
                String[] iv6 = getActivity().getResources().getStringArray(R.array.ipv6_array);
                switch (sam){
                    case "yes":
                        return iv6[0];
                    case "no":
                        return iv6[1];
                    case "default":
                        return iv6[2];
                }
            }
            if (key.equals(prefKeys[3])){
                String[] con = getActivity().getResources().getStringArray(R.array.conn_timeout_array);
                switch (sam){
                    case "15":
                        return con[0];
                    case "30":
                        return con[1];
                    case "60":
                        return con[2];
                    case "120":
                        return con[3];
                    case "0":
                        return con[4];
                }
            }
            if (key.equals(prefKeys[4])){
                String[] com = getActivity().getResources().getStringArray(R.array.compression_array);
                switch (sam){
                    case "no":
                        return com[0];
                    case "yes":
                        return com[1];
                    case "asym":
                        return com[2];
                }
            }
            return sam;
        }

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            addPreferencesFromResource(R.xml.ovpn_preferences);
            SharedPreferences dPref = harliesApplication.getDefaultSharedPreferences();
            ((PreferenceScreen) Objects.requireNonNull(findPreference("pref_preference_screen"))).setEnabled(!hLogStatus.isTunnelActive());
            for (int i=0;i < prefKeys.length;i++) {
                findPreference(prefKeys[i]).setSummary(getPrefSummary(prefKeys[i],dPref.getString(prefKeys[i],prefSummary[i])));
                findPreference(prefKeys[i]).setOnPreferenceChangeListener((preference, newValue) -> {
                    String key = preference.getKey();
                    String sam = String.valueOf(newValue);
                    preference.setSummary(getPrefSummary(key,sam));
                    return true;
                });
            }
        }
    }

}
