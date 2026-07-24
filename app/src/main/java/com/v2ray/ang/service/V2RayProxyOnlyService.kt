package com.v2ray.ang.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import java.lang.ref.SoftReference
import kotlin.system.exitProcess

class V2RayProxyOnlyService : Service(), ServiceControl {
    companion object {
        @JvmStatic val MSG_STATE_STAR = "MSG_STATE_START"
        @JvmStatic val MSG_STATE_STOP = "MSG_STATE_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        V2RayServiceManager.serviceControl = SoftReference(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action!=null){
            if (action == V2RayVpnService.MSG_STATE_STAR){
                V2RayServiceManager.startV2rayPoint()
            } else if (action== V2RayVpnService.MSG_STATE_STOP) {
                onDestroy()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        V2RayServiceManager.stopV2rayPoint()
    }

    override fun getService(): Service {
        return this
    }

    override fun startService() {
        // do nothing
    }

    override fun stopService() {
        stopSelf()
    }

    override fun vpnProtect(socket: Int): Boolean {
        return true
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}