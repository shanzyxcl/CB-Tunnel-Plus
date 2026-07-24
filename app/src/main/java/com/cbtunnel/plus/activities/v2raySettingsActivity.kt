package com.cbtunnel.plus.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.v2ray.ang.AppConfig
import com.v2ray.ang.viewmodel.SettingsViewModel
import com.cbtunnel.plus.R
import com.cbtunnel.plus.harliesApplication
import com.cbtunnel.plus.logger.hLogStatus

class v2raySettingsActivity : OpenVPNClientBase() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    private var mActivity: AppCompatActivity? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity = this@v2raySettingsActivity
        setContentView(R.layout.v2ray_settings)
        window.statusBarColor = config!!.colorAccent
        val mToolbar:Toolbar = findViewById(R.id.toolbar_main)
        mToolbar.title = resources.getString(R.string.app_name)
        mToolbar.subtitle = "V2Ray Settings"
        mToolbar.setBackgroundColor(config!!.colorAccent)
        mToolbar.setTitleTextColor(if (config!!.appThemeUtil) Color.BLACK else Color.WHITE)
        mToolbar.setSubtitleTextColor(if (config!!.appThemeUtil) Color.BLACK else Color.WHITE)
        mToolbar.setNavigationIcon(if (config!!.appThemeUtil) R.drawable.arrow_d else R.drawable.arrow_l)
        setSupportActionBar(mToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        settingsViewModel.startListenPreferenceChange()
        mToolbar.setNavigationOnClickListener { finish() }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        //private val perAppProxy by lazy { findPreference<CheckBoxPreference>(AppConfig.PREF_PER_APP_PROXY) }
        private val localDns by lazy { findPreference<CheckBoxPreference>(AppConfig.PREF_LOCAL_DNS_ENABLED) }
        private val fakeDns by lazy { findPreference<CheckBoxPreference>(AppConfig.PREF_FAKE_DNS_ENABLED) }
        private val localDnsPort by lazy { findPreference<EditTextPreference>(AppConfig.PREF_LOCAL_DNS_PORT) }
        private val vpnDns by lazy { findPreference<EditTextPreference>(AppConfig.PREF_VPN_DNS) }
        //        val autoRestart by lazy { findPreference(PREF_AUTO_RESTART) as CheckBoxPreference }
        private val remoteDns by lazy { findPreference<EditTextPreference>(AppConfig.PREF_REMOTE_DNS) }
        private val domesticDns by lazy { findPreference<EditTextPreference>(AppConfig.PREF_DOMESTIC_DNS) }
        private val socksPort by lazy { findPreference<EditTextPreference>(AppConfig.PREF_SOCKS_PORT) }
        private val httpPort by lazy { findPreference<EditTextPreference>(AppConfig.PREF_HTTP_PORT) }
        //private val routingCustom by lazy { findPreference<Preference>(AppConfig.PREF_ROUTING_CUSTOM) }
        //        val licenses: Preference by lazy { findPreference(PREF_LICENSES) }
//        val feedback: Preference by lazy { findPreference(PREF_FEEDBACK) }
//        val tgGroup: Preference by lazy { findPreference(PREF_TG_GROUP) }

        private val mode by lazy { findPreference<ListPreference>(AppConfig.PREF_MODE) }

        override fun onCreatePreferences(bundle: Bundle?, s: String?) {
            addPreferencesFromResource(R.xml.v2rey_preferences)
            //val mConfig = ConfigUtil.getInstance(if (activity==null) harliesApplication.getApp() else activity)
            findPreference<PreferenceScreen>("pref_preference_screen")?.isEnabled = !hLogStatus.isTunnelActive()
            /*routingCustom?.setOnPreferenceClickListener {
                startActivity(Intent(activity, v2rayRoutingSettingsActivity::class.java))
                false
            }*/

            /*perAppProxy?.setOnPreferenceClickListener {
                startActivity(Intent(activity, HarliesAppManager::class.java))
                perAppProxy?.isChecked = true
                false
            }*/

            remoteDns?.setOnPreferenceChangeListener { _, any ->
                // remoteDns.summary = any as String
                val nval = any as String
                remoteDns?.summary = if (nval == "") AppConfig.DNS_AGENT else nval
                true
            }
            domesticDns?.setOnPreferenceChangeListener { _, any ->
                // domesticDns.summary = any as String
                val nval = any as String
                domesticDns?.summary = if (nval == "") AppConfig.DNS_DIRECT else nval
                true
            }

            localDns?.setOnPreferenceChangeListener{ _, any ->
                updateLocalDns(any as Boolean)
                true
            }
            localDnsPort?.setOnPreferenceChangeListener { _, any ->
                val nval = any as String
                localDnsPort?.summary = if (TextUtils.isEmpty(nval)) AppConfig.PORT_LOCAL_DNS else nval
                true
            }
            vpnDns?.setOnPreferenceChangeListener { _, any ->
                vpnDns?.summary = any as String
                true
            }
            socksPort?.setOnPreferenceChangeListener { _, any ->
                val nval = any as String
                socksPort?.summary = if (TextUtils.isEmpty(nval)) AppConfig.PORT_SOCKS else nval
                true
            }
            httpPort?.setOnPreferenceChangeListener { _, any ->
                val nval = any as String
                httpPort?.summary = if (TextUtils.isEmpty(nval)) AppConfig.PORT_HTTP else nval
                true
            }
            mode?.setOnPreferenceChangeListener { _, newValue ->
                updateMode(newValue.toString())
                true
            }
            mode?.dialogLayoutResource = R.layout.preference_with_help_link
            //loglevel.summary = "LogLevel"
        }

        override fun onStart() {
            super.onStart()
            val defaultSharedPreferences = harliesApplication.getDefaultSharedPreferences()
            updateMode(defaultSharedPreferences.getString(AppConfig.PREF_MODE, "VPN"))
            var remoteDnsString = defaultSharedPreferences.getString(AppConfig.PREF_REMOTE_DNS, "")
            domesticDns?.summary = defaultSharedPreferences.getString(AppConfig.PREF_DOMESTIC_DNS, "")

            localDnsPort?.summary = defaultSharedPreferences.getString(AppConfig.PREF_LOCAL_DNS_PORT, AppConfig.PORT_LOCAL_DNS)
            socksPort?.summary = defaultSharedPreferences.getString(AppConfig.PREF_SOCKS_PORT, AppConfig.PORT_SOCKS)
            httpPort?.summary = defaultSharedPreferences.getString(AppConfig.PREF_HTTP_PORT, AppConfig.PORT_HTTP)

            if (TextUtils.isEmpty(remoteDnsString)) {
                remoteDnsString = AppConfig.DNS_AGENT
            }
            if (TextUtils.isEmpty(domesticDns?.summary)) {
                domesticDns?.summary = AppConfig.DNS_DIRECT
            }
            remoteDns?.summary = remoteDnsString
            vpnDns?.summary = defaultSharedPreferences.getString(AppConfig.PREF_VPN_DNS, remoteDnsString)

            if (TextUtils.isEmpty(localDnsPort?.summary)) {
                localDnsPort?.summary = AppConfig.PORT_LOCAL_DNS
            }
            if (TextUtils.isEmpty(socksPort?.summary)) {
                socksPort?.summary = AppConfig.PORT_SOCKS
            }
            if (TextUtils.isEmpty(httpPort?.summary)) {
                httpPort?.summary = AppConfig.PORT_HTTP
            }
        }

        private fun updateMode(mode: String?) {
            val defaultSharedPreferences = harliesApplication.getDefaultSharedPreferences()
            val vpn = mode == "VPN"
           /*perAppProxy?.isEnabled = vpn
            perAppProxy?.isChecked = HarliesApplication.getDefaultSharedPreferences().getBoolean(AppConfig.PREF_PER_APP_PROXY, false)
            */
            localDns?.isEnabled = vpn
            fakeDns?.isEnabled = vpn
            localDnsPort?.isEnabled = vpn
            vpnDns?.isEnabled = vpn
            if (vpn) {
                updateLocalDns(defaultSharedPreferences.getBoolean(AppConfig.PREF_LOCAL_DNS_ENABLED, false))
            }
        }

        private fun updateLocalDns(enabled: Boolean) {
            fakeDns?.isEnabled = enabled
            localDnsPort?.isEnabled = enabled
            vpnDns?.isEnabled = !enabled
        }
    }

    fun onModeHelpClicked() {
        startActivity(Intent(this@v2raySettingsActivity, webActivity::class.java).putExtra("mConfigPanelRenew", config!!.contactUrl))
    }
}
