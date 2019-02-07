package org.videolan.mudiAudioVideo.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import org.videolan.libvlc.Media
import org.videolan.medialibrary.Medialibrary
import org.videolan.medialibrary.media.MediaWrapper
import org.videolan.tools.SingletonHolder
import org.videolan.mudiAudioVideo.VLCApplication
import org.videolan.mudiAudioVideo.startMedialibrary
import java.io.File
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import kotlin.coroutines.resume

object Settings : SingletonHolder<SharedPreferences, Context>({ PreferenceManager.getDefaultSharedPreferences(it) })

fun String.validateLocation(): Boolean {
    var location = this
    /* Check if the MRL contains a scheme */
    if (!location.matches("\\w+://.+".toRegex())) location = "file://$location"
    if (location.toLowerCase(Locale.ENGLISH).startsWith("file://")) {
        /* Ensure the file exists */
        val f: File
        try {
            f = File(URI(location))
        } catch (e: URISyntaxException) {
            return false
        } catch (e: IllegalArgumentException) {
            return false
        }
        if (!f.isFile) return false
    }
    return true
}

inline fun <reified T : ViewModel> Fragment.getModelWithActivity() = ViewModelProviders.of(requireActivity()).get(T::class.java)
inline fun <reified T : ViewModel> Fragment.getModel() = ViewModelProviders.of(this).get(T::class.java)
inline fun <reified T : ViewModel> FragmentActivity.getModel() = ViewModelProviders.of(this).get(T::class.java)

suspend fun retry (
        times: Int = 3,
        delayTime: Long = 500L,
        block: suspend () -> Boolean): Boolean
{
    repeat(times - 1) {
        if (block()) return true
        delay(delayTime)
    }
    return block() // last attempt
}

fun Media?.canExpand() = this != null && (type == Media.Type.Directory || type == Media.Type.Playlist)
fun MediaWrapper?.isMedia() = this != null && (type == MediaWrapper.TYPE_AUDIO || type == MediaWrapper.TYPE_VIDEO)
fun MediaWrapper?.isBrowserMedia() = this != null && (isMedia() || type == MediaWrapper.TYPE_DIR || type == MediaWrapper.TYPE_PLAYLIST)

fun Context.getAppSystemService(name: String) = applicationContext.getSystemService(name)!!

fun Long.random() = (Random().nextFloat() * this).toLong()

suspend inline fun <reified T> Context.getFromMl(crossinline block: Medialibrary.() -> T) = withContext(Dispatchers.IO) {
    val ml = Medialibrary.getInstance()
    if (ml.isStarted) block.invoke(ml)
    else suspendCancellableCoroutine { continuation ->
        val listener = object : Medialibrary.OnMedialibraryReadyListener {
            override fun onMedialibraryReady() {
                continuation.resume(block.invoke(ml))
                let { launch { ml.removeOnMedialibraryReadyListener(it) } }
            }
            override fun onMedialibraryIdle() {}
        }
        continuation.invokeOnCancellation { ml.removeOnMedialibraryReadyListener(listener) }
        ml.addOnMedialibraryReadyListener(listener)
        startMedialibrary(false, false, false)
    }
}


fun List<MediaWrapper>.getWithMLMeta() : List<MediaWrapper> {
    if (this is MutableList<MediaWrapper>) return updateWithMLMeta()
    val list = mutableListOf<MediaWrapper>()
    val ml = VLCApplication.getMLInstance()
    for (media in this) {
        if (media.id == 0L) {
            val mw = ml.findMedia(media)
            if (mw.id != 0L) if (mw.type == MediaWrapper.TYPE_ALL) mw.type = media.type
            list.add(mw)
        }
    }
    return list
}


fun MutableList<MediaWrapper>.updateWithMLMeta() : MutableList<MediaWrapper> {
    val iter = listIterator()
    val ml = VLCApplication.getMLInstance()
    try {
        while (iter.hasNext()) {
            val media = iter.next()
            if (media.id == 0L) {
                val mw = ml.findMedia(media)
                if (mw!!.id != 0L) {
                    if (mw.type == MediaWrapper.TYPE_ALL) mw.type = media.getType()
                    iter.set(mw)
                }
            }
        }
    } catch (ignored: Exception) {}
    return this
}

suspend fun String.scanAllowed() = withContext(Dispatchers.IO) {
    val file = File(Uri.parse(this@scanAllowed).path)
    if (!file.exists() || !file.canRead()) return@withContext false
    val children = file.list() ?: return@withContext true
    for (child in children) if (child == ".nomedia") return@withContext false
    true
}

fun <X, Y> CoroutineScope.map(
        source: LiveData<X>,
        f : suspend (value: X?) -> Y
): LiveData<Y> {
    return MediatorLiveData<Y>().apply {
        addSource(source) {
            launch { value = f(it) }
        }
    }
}
