package com.duanlab.npsclient

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AutoStartBroadReceiver : BroadcastReceiver() {
    private val ACTION_BOOT = "android.intent.action.BOOT_COMPLETED"
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_BOOT) {
            val sp = context.getSharedPreferences(PreferencesKey.PREF_NAME, Context.MODE_PRIVATE)
            val autoStart = sp.getBoolean(PreferencesKey.AUTO_START, false)
            val shellRunning = sp.getBoolean(PreferencesKey.SHELL_RUNNING, false)
            val cmdStr = sp.getString(PreferencesKey.CMD_STR, "") ?: ""
            if (autoStart && shellRunning && cmdStr.isNotEmpty()) {
                val config = NpsConfig(cmdStr)
                val mainIntent = Intent(context, ShellService::class.java).apply {
                    action = ShellServiceAction.START
                    putParcelableArrayListExtra(IntentExtraKey.NpsConfig, arrayListOf(config))
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(mainIntent)
                } else {
                    context.startService(mainIntent)
                }
            }
        }
    }
}