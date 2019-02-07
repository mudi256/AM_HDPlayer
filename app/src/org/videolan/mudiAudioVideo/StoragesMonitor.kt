package org.videolan.mudiAudioVideo

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.delay
import org.videolan.medialibrary.Medialibrary
import org.videolan.mudiAudioVideo.gui.DialogActivity
import org.videolan.mudiAudioVideo.util.AppScope
import org.videolan.mudiAudioVideo.util.getFromMl
import org.videolan.mudiAudioVideo.util.scanAllowed



private const val TAG = "VLC/StoragesMonitor"
class StoragesMonitor : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive ${intent.action}")
        val action = intent.action ?: return
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        when (action) {
            Intent.ACTION_MEDIA_MOUNTED -> intent.data?.let { actor.offer(Mount(context, it)) }
            Intent.ACTION_MEDIA_UNMOUNTED -> intent.data?.let { actor.offer(Unmount(context, it)) }
            else -> return
        }
    }

    private val actor = AppScope.actor<MediaEvent>(capacity = Channel.UNLIMITED) {
        for (action in channel) when (action){
            is Mount -> {
                if (TextUtils.isEmpty(action.uuid)) return@actor
                if (action.path.scanAllowed()) {
                    val knownDevices = action.ctx.getFromMl { devices }
                    val ml = Medialibrary.getInstance()
                    val scan = !containsDevice(knownDevices, action.path) && ml.addDevice(action.uuid, action.path, true)
                    val intent = Intent(action.ctx, DialogActivity::class.java).apply {
                        setAction(DialogActivity.KEY_DEVICE)
                        putExtra(DialogActivity.EXTRA_PATH, action.path)
                        putExtra(DialogActivity.EXTRA_UUID, action.uuid)
                        putExtra(DialogActivity.EXTRA_SCAN, scan)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    action.ctx.startActivity(intent)
                }
            }
            is Unmount -> {
                delay(100L)
                Medialibrary.getInstance().removeDevice(action.uuid, action.path)
            }
        }
    }
}

private sealed class MediaEvent(val ctx: Context)
private class Mount(ctx: Context, val uri : Uri, val path : String = uri.path, val uuid : String = uri.lastPathSegment) : MediaEvent(ctx)
private class Unmount(ctx: Context, val uri : Uri, val path : String = uri.path, val uuid : String = uri.lastPathSegment) : MediaEvent(ctx)

fun Context.enableStorageMonitoring() {
    val componentName = ComponentName(applicationContext, StoragesMonitor::class.java)
    applicationContext.packageManager.setComponentEnabledSetting(componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP)
}