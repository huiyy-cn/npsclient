package com.duanlab.npsclient

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class ShellService : LifecycleService() {
    private val _processThreads = MutableStateFlow(mutableMapOf<NpsConfig, ShellThread>())
    private val _logText = MutableStateFlow("")
    val logText: StateFlow<String> = _logText

    fun clearLog() {
        _logText.value = ""
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): ShellService = this@ShellService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val npsConfigs: ArrayList<NpsConfig>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.extras?.getParcelableArrayList(
                    IntentExtraKey.NpsConfig,
                    NpsConfig::class.java
                )
            } else {
                @Suppress("DEPRECATION") intent?.extras?.getParcelableArrayList(IntentExtraKey.NpsConfig)
            }
        if (npsConfigs == null) {
            Log.e("ShellService", "npsConfig is null")
            Toast.makeText(this, "npsConfig is null", Toast.LENGTH_SHORT).show()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ShellServiceAction.START -> {
                for (config in npsConfigs) {
                    startNps(config)
                }
                Toast.makeText(this, getString(R.string.service_start_toast), Toast.LENGTH_SHORT)
                    .show()
                startForeground(1, showNotification())
            }
            ShellServiceAction.STOP -> {
                for (config in npsConfigs) {
                    stopNps(config)
                }
                startForeground(1, showNotification())
                if (_processThreads.value.isEmpty()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION") stopForeground(true)
                    }
                    stopSelf()
                    Toast.makeText(this, getString(R.string.service_stop_toast), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startNps(config: NpsConfig) {
        Log.d("ShellService", "start config: $config")
        val ainfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_SHARED_LIBRARY_FILES)
        val args = config.cmdstr
            .split(" ")
            .map { it.trim() }
            .filter { it.startsWith("-") }
            .toMutableList()
        if (args.none { it.startsWith("-debug=") }) {
            args.add("-debug=false")
        }
        if (args.none { it.startsWith("-log=") }) {
            args.add("-log=stdout")
        }
        val nativeLibPath = ainfo.nativeLibraryDir
        val binary = "$nativeLibPath/${BuildConfig.NpcFileName}"
        val commandList = listOf(binary) + args
        Log.d("ShellService", "Command: ${commandList.joinToString(" ")}")
        try {
            val thread = runCommand(commandList, filesDir)
            _processThreads.update { it.toMutableMap().apply { put(config, thread) } }
        } catch (e: Exception) {
            Log.e("ShellService", e.stackTraceToString())
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun stopNps(config: NpsConfig) {
        val thread = _processThreads.value[config]
        thread?.stopProcess()
        _processThreads.update { it.toMutableMap().apply { remove(config) } }
    }

    override fun onDestroy() {
        super.onDestroy()
        _processThreads.value.forEach { it.value.stopProcess() }
        _processThreads.update { it.clear(); it }
    }

    private fun runCommand(command: List<String>, dir: File): ShellThread {
        val sharedPref = applicationContext.getSharedPreferences(PreferencesKey.PREF_NAME, Context.MODE_PRIVATE)
        val thread = ShellThread(command, dir) { line ->
            if (sharedPref.getBoolean(PreferencesKey.LOG_OUTPUT_ENABLED, true)) {
                _logText.update { current ->
                    val currentLines = if (current.isEmpty()) listOf() else current.split("\n").filter { it.isNotEmpty() }
                    val newLines = currentLines + line
                    val limitedLines = if (newLines.size > 1000) newLines.takeLast(1000) else newLines
                    limitedLines.joinToString("\n") + "\n"
                }
            }
        }
        thread.start()
        return thread
    }

    private fun showNotification(): Notification {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
            }
        val notification = NotificationCompat.Builder(this, "shell_bg")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_channel_name))
            .setContentIntent(pendingIntent)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notification.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                .build()
        } else {
            notification.build()
        }
    }
}