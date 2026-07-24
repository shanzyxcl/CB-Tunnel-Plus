package com.v2ray.ang.util

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.text.Editable
import android.util.Base64
import android.util.Log
import android.util.Patterns
import android.webkit.URLUtil
import androidx.annotation.Keep
import com.tencent.mmkv.MMKV
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.ANG_PACKAGE
import com.v2ray.ang.service.V2RayProxyOnlyService
import com.v2ray.ang.service.V2RayVpnService
import java.io.IOException
import java.net.*
import java.util.*

object Utils {
    private val settingsStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_SETTING, MMKV.MULTI_PROCESS_MODE) }

    @Keep
    @JvmStatic
    fun startV2RayService(context: Context) {
        val intent = if ((settingsStorage.decodeString(AppConfig.PREF_MODE) ?: "VPN") == "VPN") {
            Intent(context.applicationContext, V2RayVpnService::class.java).setAction(V2RayVpnService.MSG_STATE_STAR)
        } else {
            Intent(context.applicationContext, V2RayProxyOnlyService::class.java).setAction(V2RayProxyOnlyService.MSG_STATE_STAR)
        }
        context.startService(intent)
    }

    @Keep
    @JvmStatic
    fun stopV2RayVService(context: Context) {
        val intent = if ((settingsStorage.decodeString(AppConfig.PREF_MODE) ?: "VPN") == "VPN") {
            Intent(context.applicationContext, V2RayVpnService::class.java).setAction(V2RayVpnService.MSG_STATE_STOP)
        } else {
            Intent(context.applicationContext, V2RayProxyOnlyService::class.java).setAction(V2RayProxyOnlyService.MSG_STATE_STOP)
        }
        context.startService(intent)
    }

    /**
     * convert string to editalbe for kotlin
     *
     * @param text
     * @return
     */
    fun getEditable(text: String): Editable {
        return Editable.Factory.getInstance().newEditable(text)
    }

    /**
     * find value in array position
     */
    fun arrayFind(array: Array<out String>, value: String): Int {
        for (i in array.indices) {
            if (array[i] == value) {
                return i
            }
        }
        return -1
    }

    /**
     * parseInt
     */
    fun parseInt(str: String): Int {
        return parseInt(str, 0)
    }

    fun parseInt(str: String?, default: Int): Int {
        str ?: return default
        return try {
            Integer.parseInt(str)
        } catch (e: Exception) {
            //e.printStackTrace()
            default
        }
    }

    /**
     * get text from clipboard
     */
    fun getClipboard(context: Context): String {
        return try {
            val cmb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cmb.primaryClip?.getItemAt(0)?.text.toString()
        } catch (e: Exception) {
            //e.printStackTrace()
            ""
        }
    }

    /**
     * set text to clipboard
     */
    fun setClipboard(context: Context, content: String) {
        try {
            val cmb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText(null, content)
            cmb.setPrimaryClip(clipData)
        } catch (e: Exception) {
            //e.printStackTrace()
        }
    }

    /**
     * base64 decode
     */
    fun decode(text: String): String {
        tryDecodeBase64(text)?.let { return it }
        if (text.endsWith('=')) {
            // try again for some loosely formatted base64
            tryDecodeBase64(text.trimEnd('='))?.let { return it }
        }
        return ""
    }

    @SuppressLint("LongLogTag")
    fun tryDecodeBase64(text: String): String? {
        try {
            return Base64.decode(text, Base64.NO_WRAP).toString(charset("UTF-8"))
        } catch (e: Exception) {
            Log.i(ANG_PACKAGE, "Parse base64 standard failed $e")
        }
        try {
            return Base64.decode(text, Base64.NO_WRAP.or(Base64.URL_SAFE)).toString(charset("UTF-8"))
        } catch (e: Exception) {
            Log.i(ANG_PACKAGE, "Parse base64 url safe failed $e")
        }
        return null
    }

    /**
     * base64 encode
     */
    fun encode(text: String): String {
        return try {
            Base64.encodeToString(text.toByteArray(charset("UTF-8")), Base64.NO_WRAP)
        } catch (e: Exception) {
            //e.printStackTrace()
            ""
        }
    }

    /**
     * get remote dns servers from preference
     */
    fun getRemoteDnsServers(): List<String> {
        val remoteDns = settingsStorage.decodeString(AppConfig.PREF_REMOTE_DNS) ?: AppConfig.DNS_AGENT
        val ret = remoteDns.split(",").filter { isPureIpAddress(it) || isCoreDNSAddress(it) }
        if (ret.isEmpty()) {
            return listOf(AppConfig.DNS_AGENT)
        }
        return ret
    }

    fun getVpnDnsServers(): List<String> {
        val vpnDns = settingsStorage.decodeString(AppConfig.PREF_VPN_DNS)
            ?: settingsStorage.decodeString(AppConfig.PREF_REMOTE_DNS)
            ?: AppConfig.DNS_AGENT
        return vpnDns.split(",").filter { isPureIpAddress(it) }
        // allow empty, in that case dns will use system default
    }

    /**
     * get remote dns servers from preference
     */
    fun getDomesticDnsServers(): List<String> {
        val domesticDns = settingsStorage.decodeString(AppConfig.PREF_DOMESTIC_DNS) ?: AppConfig.DNS_DIRECT
        val ret = domesticDns.split(",").filter { isPureIpAddress(it) || isCoreDNSAddress(it) }
        if (ret.isEmpty()) {
            return listOf(AppConfig.DNS_DIRECT)
        }
        return ret
    }

    /**
     * is ip address
     */
    fun isIpAddress(value: String): Boolean {
        try {
            var addr = value
            if (addr.isEmpty() || addr.isBlank()) {
                return false
            }
            //CIDR
            if (addr.indexOf("/") > 0) {
                val arr = addr.split("/")
                if (arr.count() == 2 && Integer.parseInt(arr[1]) > 0) {
                    addr = arr[0]
                }
            }

            // "::ffff:192.168.173.22"
            // "[::ffff:192.168.173.22]:80"
            if (addr.startsWith("::ffff:") && '.' in addr) {
                addr = addr.drop(7)
            } else if (addr.startsWith("[::ffff:") && '.' in addr) {
                addr = addr.drop(8).replace("]", "")
            }

            // addr = addr.toLowerCase()
            val octets = addr.split('.').toTypedArray()
            if (octets.size == 4) {
                if (octets[3].indexOf(":") > 0) {
                    addr = addr.substring(0, addr.indexOf(":"))
                }
                return isIpv4Address(addr)
            }

            // Ipv6addr [2001:abc::123]:8080
            return isIpv6Address(addr)
        } catch (e: Exception) {
            //e.printStackTrace()
            return false
        }
    }

    fun isPureIpAddress(value: String): Boolean {
        return (isIpv4Address(value) || isIpv6Address(value))
    }

    fun isIpv4Address(value: String): Boolean {
        val regV4 = Regex("^([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])$")
        return regV4.matches(value)
    }

    fun isIpv6Address(value: String): Boolean {
        var addr = value
        if (addr.indexOf("[") == 0 && addr.lastIndexOf("]") > 0) {
            addr = addr.drop(1)
            addr = addr.dropLast(addr.count() - addr.lastIndexOf("]"))
        }
        val regV6 = Regex("^((?:[0-9A-Fa-f]{1,4}))?((?::[0-9A-Fa-f]{1,4}))*::((?:[0-9A-Fa-f]{1,4}))?((?::[0-9A-Fa-f]{1,4}))*|((?:[0-9A-Fa-f]{1,4}))((?::[0-9A-Fa-f]{1,4})){7}$")
        return regV6.matches(addr)
    }

    private fun isCoreDNSAddress(s: String): Boolean {
        return s.startsWith("https") || s.startsWith("tcp") || s.startsWith("quic")
    }

    /**
     * uuid
     */
    fun getUuid(): String {
        return try {
            UUID.randomUUID().toString().replace("-", "")
        } catch (e: Exception) {
            //e.printStackTrace()
            ""
        }
    }

    fun urlDecode(url: String): String {
        return try {
            URLDecoder.decode(url, "utf-8")
        } catch (e: Exception) {
            //e.printStackTrace()
            url
        }
    }

    fun urlEncode(url: String): String {
        return try {
            URLEncoder.encode(url, "UTF-8")
        } catch (e: Exception) {
            //e.printStackTrace()
            url
        }
    }


    /**
     * readTextFromAssets
     */
    fun readTextFromAssets(context: Context, fileName: String): String {
        val content = context.assets.open(fileName).bufferedReader().use {
            it.readText()
        }
        return content
    }

    @Keep
    @JvmStatic
    fun userAssetPath(context: Context?): String {
        if (context == null)
            return ""
        val extDir = context.getExternalFilesDir(AppConfig.DIR_ASSETS)
            ?: return context.getDir(AppConfig.DIR_ASSETS, 0).absolutePath
        return extDir.absolutePath
    }

    fun getIpv6Address(address: String): String {
        return if (isIpv6Address(address)) {
            String.format("[%s]", address)
        } else {
            address
        }
    }

    fun fixIllegalUrl(str: String): String {
        return str
            .replace(" ","%20")
            .replace("|","%7C")
    }

    fun removeWhiteSpace(str: String?): String? {
        return str?.replace(" ", "")
    }



}

