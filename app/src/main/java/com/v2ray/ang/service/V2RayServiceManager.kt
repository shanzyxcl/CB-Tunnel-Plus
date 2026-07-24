package com.v2ray.ang.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import com.v2ray.ang.util.MmkvManager
import com.v2ray.ang.util.Utils
import com.v2ray.ang.util.V2rayConfigUtil
import com.cbtunnel.plus.R
import com.tencent.mmkv.MMKV
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.MessageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.SoftReference
import com.cbtunnel.plus.config.ConfigUtil
import libv2ray.Libv2ray
import go.Seq
import kotlinx.coroutines.DelicateCoroutinesApi
import libv2ray.V2RayPoint
import libv2ray.V2RayVPNServiceSupportsSet

object V2RayServiceManager {

    private val mMsgReceive = ReceiveMessageHandler()
    val v2rayPoint: V2RayPoint = Libv2ray.newV2RayPoint(V2RayCallback(),Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
    private val mainStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_MAIN, MMKV.MULTI_PROCESS_MODE) }
    var currentConfig: String? = null

    @Keep
    @JvmStatic
    var serviceControl: SoftReference<ServiceControl>? = null
        set(value) {
            field = value
            Seq.setContext(value?.get()?.getService()?.applicationContext)
            Libv2ray.initV2Env(Utils.userAssetPath(value?.get()?.getService()))
        }


    private class V2RayCallback : V2RayVPNServiceSupportsSet {
        override fun shutdown(): Long {
            val serviceControl = serviceControl?.get() ?: return -1
            // called by go
            return try {
                serviceControl.stopService()
                0
            } catch (e: Exception) {
                -1
            }
        }

        override fun prepare(): Long {
            return 0
        }

        override fun protect(l: Long): Boolean {
            val serviceControl = serviceControl?.get() ?: return true
            return serviceControl.vpnProtect(l.toInt())
        }

        override fun onEmitStatus(l: Long, s: String?): Long {
            //Logger.d(s)
            return 0
        }

        override fun setup(s: String): Long {
            val serviceControl = serviceControl?.get() ?: return -1
            return try {
                serviceControl.startService()
                0
            } catch (e: Exception) {
                -1
            }
        }
    }


    @SuppressLint("NewApi")
    fun startV2rayPoint() {
        val service = serviceControl?.get()?.getService() ?: return
        val guid = mainStorage.decodeString(MmkvManager.KEY_SELECTED_SERVER) ?: return
        val config = MmkvManager.decodeServerConfig(guid) ?: return
        val serverName:String = ConfigUtil.getInstance(service).serverName
        if (!v2rayPoint.isRunning) {
            val result = V2rayConfigUtil.getV2rayConfig(service, guid)
            if (!result.status) {
                MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_START_FAILURE, "Config Error: ${result.status}")
                return
            }
            try {
                val mFilter = IntentFilter(AppConfig.BROADCAST_ACTION_SERVICE)
                mFilter.addAction(Intent.ACTION_SCREEN_ON)
                mFilter.addAction(Intent.ACTION_SCREEN_OFF)
                mFilter.addAction(Intent.ACTION_USER_PRESENT)
                ContextCompat.registerReceiver(service,mMsgReceive, mFilter, ContextCompat.RECEIVER_EXPORTED)
            } catch (e: Exception) {
                MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_START_FAILURE, e.toString())
            }
            v2rayPoint.configureFileContent = result.content
            v2rayPoint.domainName = config.getV2rayPointDomainAndPort()
            currentConfig = serverName
            try {
                v2rayPoint.runLoop(false)
            } catch (e: Exception) {
                MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_START_FAILURE, e.toString())
            }
            if (v2rayPoint.isRunning) {
                MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_START_SUCCESS, "")
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun measureV2rayDelay() {
        GlobalScope.launch(Dispatchers.IO) {
            val service = serviceControl?.get()?.getService() ?: return@launch
            var time = -1L
            var errstr = ""
            delay(1500L)
            if (v2rayPoint.isRunning) {
                try {
                    time = v2rayPoint.measureDelay()
                } catch (e: Exception) {
                    errstr = e.message?.substringAfter("\":") ?: "empty message"
                }
            }
            val result = if (time == -1L) {
                "<font color = #FF9600>"+service.resources.getString(R.string.connection_test_error, errstr)
            } else {
                if (time >= 400) {
                    service.resources.getString(R.string.connection_test_available)+ " (<font color = #ff0000>" + time + "ms</font>)"
                } else {
                    service.resources.getString(R.string.connection_test_available)+ " (<font color = #68B86B>" + time + "ms</font>)"
                }
            }
            MessageUtil.sendMsg2UI(service, AppConfig.MSG_MEASURE_DELAY_SUCCESS, result)
        }
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun stopV2rayPoint() {
        val service = serviceControl?.get()?.getService() ?: return
        if (v2rayPoint.isRunning) {
            GlobalScope.launch(Dispatchers.Default) {
                try {
                    v2rayPoint.stopLoop()
                } catch (e: Exception) {
                    //   Log.d(BuildConfig.LIBRARY_PACKAGE_NAME, e.toString())
                }
            }
        }
        MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_NOT_RUNNING, "")
        MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_STOP_SUCCESS, "")
        try {
            service.unregisterReceiver(mMsgReceive)
        } catch (e: Exception) {
            //Log.d(ANG_PACKAGE, e.toString())
        }
    }

    private class ReceiveMessageHandler : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val serviceControl = serviceControl?.get() ?: return
            when (intent?.getIntExtra("key", 0)) {
                AppConfig.MSG_REGISTER_CLIENT -> {
                    if (v2rayPoint.isRunning) {
                        MessageUtil.sendMsg2UI(serviceControl.getService(), AppConfig.MSG_STATE_RUNNING, "")
                    } else {
                        MessageUtil.sendMsg2UI(serviceControl.getService(), AppConfig.MSG_STATE_NOT_RUNNING, "")
                    }
                }
                AppConfig.MSG_UNREGISTER_CLIENT -> {
                    // nothing to do
                }
                AppConfig.MSG_STATE_START -> {
                    // nothing to do
                }
                AppConfig.MSG_STATE_STOP -> {
                    serviceControl.stopService()
                }
                AppConfig.MSG_MEASURE_DELAY -> {
                    measureV2rayDelay()
                }
            }
        }
    }


}