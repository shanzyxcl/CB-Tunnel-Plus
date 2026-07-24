package com.v2ray.ang.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.SystemClock
import android.text.TextUtils
import android.util.Log
import androidx.annotation.Keep
import com.v2ray.ang.AppConfig
import com.v2ray.ang.extension.responseLength
import kotlinx.coroutines.DelicateCoroutinesApi
import com.cbtunnel.plus.R
import com.cbtunnel.plus.config.ConfigUtil
import com.cbtunnel.plus.config.SettingsConstants
import com.cbtunnel.plus.harliesApplication
import com.cbtunnel.plus.logger.hLogStatus
import com.cbtunnel.plus.service.HarlieService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import libv2ray.Libv2ray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.net.UnknownHostException
import kotlin.coroutines.coroutineContext

object SpeedtestUtil {

    private val tcpTestingSockets = ArrayList<Socket?>()

    suspend fun tcping(url: String, port: Int): Long {
        var time = -1L
        for (k in 0 until 2) {
            val one = socketConnectTime(url, port)
            if (!coroutineContext.isActive) {
                break
            }
            if (one != -1L && (time == -1L || one < time)) {
                time = one
            }
        }
        return time
    }

    @SuppressLint("LongLogTag")
    fun realPing(config: String): Long {
        return try {
            Libv2ray.measureOutboundDelay(config)
        } catch (e: Exception) {
            Log.d(AppConfig.ANG_PACKAGE, "realPing: $e")
            -1L
        }
    }

    @Keep
    @JvmStatic
    fun getPing(url: String,count: String): Long {
        try {
            val command = "/system/bin/ping -c $count $url"
            val process = Runtime.getRuntime().exec(command)
            val allText = process.inputStream.bufferedReader().use { it.readText() }
            if (!TextUtils.isEmpty(allText)) {
                val tempInfo = allText.substring(allText.indexOf("min/avg/max/mdev") + 19)
                val temps = tempInfo.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (temps.count() > 0 && temps[0].length < 10) {
                    return temps[0].toFloat().toInt().toLong()
                }
            }
        } catch (e: Exception) {
            //e.printStackTrace()
        }
        return -1
    }

    fun ping(url: String): String {
        try {
            val command = "/system/bin/ping -c 3 $url"
            val process = Runtime.getRuntime().exec(command)
            val allText = process.inputStream.bufferedReader().use { it.readText() }
            if (!TextUtils.isEmpty(allText)) {
                val tempInfo = allText.substring(allText.indexOf("min/avg/max/mdev") + 19)
                val temps = tempInfo.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (temps.count() > 0 && temps[0].length < 10) {
                    return temps[0].toFloat().toInt().toString() + "ms"
                }
            }
        } catch (e: Exception) {
            //e.printStackTrace()
        }
        return "-1ms"
    }

    @SuppressLint("LongLogTag")
    fun socketConnectTime(url: String, port: Int): Long {
        try {
            val socket = Socket()
            synchronized(this) {
                tcpTestingSockets.add(socket)
            }
            val start = System.currentTimeMillis()
            socket.connect(InetSocketAddress(url, port),3000)
            val time = System.currentTimeMillis() - start
            synchronized(this) {
                tcpTestingSockets.remove(socket)
            }
            socket.close()
            return time
        } catch (e: UnknownHostException) {
            //e.printStackTrace()
        } catch (e: IOException) {
            Log.d(AppConfig.ANG_PACKAGE, "socketConnectTime IOException: $e")
        } catch (e: Exception) {
            //e.printStackTrace()
        }
        return -1
    }

    fun closeAllTcpSockets() {
        synchronized(this) {
            tcpTestingSockets.forEach {
                it?.close()
            }
            tcpTestingSockets.clear()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Keep
    @JvmStatic
    fun testConnection(context: Context,isConnected: Boolean) {
        val serverType:String = ConfigUtil.getInstance(context).serverType
        val mPref: SharedPreferences = harliesApplication.getPrivateSharedPreferences()
        if (serverType != SettingsConstants.SERVER_TYPE_V2RAY && isConnected){
            GlobalScope.launch(Dispatchers.IO) {
                var result: String
                var mResult = false
                val sType: String
                var conn: HttpURLConnection? = null
                delay(1000L)
                try {
                    hLogStatus.logInfo("Checking connection.")
                    val url = URL("https", "www.google.com", "/generate_204")
                    conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 30000
                    conn.readTimeout = 30000
                    conn.setRequestProperty("Connection", "close")
                    conn.instanceFollowRedirects = false
                    conn.useCaches = false
                    val start = SystemClock.elapsedRealtime()
                    val code = conn.responseCode
                    val elapsed = SystemClock.elapsedRealtime() - start

                    if (code == 204 || code == 200 && conn.responseLength == 0L) {
                        mResult = true
                        result = if (elapsed >= 400) {
                            context.resources.getString(R.string.connection_test_available)+ " (<font color = #ff0000>" + elapsed + "ms</font>)"
                        } else {
                            context.resources.getString(R.string.connection_test_available)+ " (<font color = #68B86B>" + elapsed + "ms</font>)"
                        }
                    } else {
                        mResult = false
                        throw IOException(context.getString(R.string.connection_test_error_status_code, code))
                    }
                } catch (e: IOException) {
                    if(hLogStatus.isTunnelActive() && isConnected)context.startService(Intent(context, HarlieService::class.java).setAction("CONNECTION_TEST_FAILD"))
                    result = "<font color = #FF9600>"+context.getString(R.string.connection_test_error, e.message).replace("www.google.com","*********")
                } catch (e: Exception) {
                    if(hLogStatus.isTunnelActive() && isConnected)context.startService(Intent(context, HarlieService::class.java).setAction("CONNECTION_TEST_FAILD"))
                    result = "<font color = #FF9600>"+context.getString(R.string.connection_test_error, e.message).replace("www.google.com","*********")
                } finally {
                    conn?.disconnect()
                }
                sType = "<b>$serverType $result</b>"
                hLogStatus.logInfo(sType)
                if (mPref.getBoolean("isAutoPinger", false) && mResult){
                    context.startService(Intent(context, HarlieService::class.java).setAction("START_PINGER"))
                }
            }
        }
    }

   /* fun testConnection(context: Context, port: Int): String {
        // return V2RayVpnService.measureV2rayDelay()
        var result: String
        var conn: HttpURLConnection? = null

        try {
            val url = URL("https", "www.google.com", "/generate_204")
            conn = url.openConnection(Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", port))) as HttpURLConnection
            conn.connectTimeout = 30000
            conn.readTimeout = 30000
            conn.setRequestProperty("Connection", "close")
            conn.instanceFollowRedirects = false
            conn.useCaches = false
            val start = SystemClock.elapsedRealtime()
            val code = conn.responseCode
            val elapsed = SystemClock.elapsedRealtime() - start
            if (code == 204 || code == 200 && conn.responseLength == 0L) {
                if (elapsed >= 400) {
                    result = context.resources.getString(R.string.connection_test_available)+ " (<font color = #ff0000>" + elapsed + "ms</font>)"
                } else {
                    result = context.resources.getString(R.string.connection_test_available)+ " (<font color = #BA000F>" + elapsed + "ms</font>)"
                }
            } else {
                throw IOException(context.getString(R.string.connection_test_error_status_code, code))
            }
        } catch (e: IOException) {
            // network exception
            Log.d(AppConfig.ANG_PACKAGE, "testConnection IOException: " + Log.getStackTraceString(e))
            result = context.getString(R.string.connection_test_error, e.message)
        } catch (e: Exception) {
            // library exception, eg sumsung
            Log.d(AppConfig.ANG_PACKAGE, "testConnection Exception: " + Log.getStackTraceString(e))
            result = context.getString(R.string.connection_test_error, e.message)
        } finally {
            conn?.disconnect()
        }
        return result
    }

    fun getLibVersion(): String {
        return Libv2ray.checkVersionX()
    }*/

}
