package org.videolan.mudiAudioVideo.util

import android.content.Context
import android.text.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.medialibrary.Medialibrary
import org.videolan.medialibrary.media.*
import org.videolan.mudiAudioVideo.PlaybackService
import org.videolan.mudiAudioVideo.R
import org.videolan.mudiAudioVideo.viewmodels.SortableModel

private const val LENGTH_WEEK = 7*24*60*60
private const val LENGTH_MONTH = 30*LENGTH_WEEK
private const val LENGTH_YEAR = 52*LENGTH_WEEK
private const val LENGTH_2_YEAR = 2* LENGTH_YEAR

object ModelsHelper {

    suspend fun generateSections(sort: Int, items: List<MediaLibraryItem>) = withContext(Dispatchers.IO) {
        val array = splitList(sort, items)
        val datalist = mutableListOf<MediaLibraryItem>()
        for ((key, list) in array) {
            datalist.add(DummyItem(key))
            datalist.addAll(list)
        }
        datalist
    }

    internal suspend fun splitList(sort: Int, items: Collection<MediaLibraryItem>) = withContext(Dispatchers.IO) {
        val array = mutableMapOf<String, MutableList<MediaLibraryItem>>()
        when (sort) {
            Medialibrary.SORT_DEFAULT,
            Medialibrary.SORT_FILENAME,
            Medialibrary.SORT_ALPHA -> {
                var currentLetter: String? = null
                for (item in items) {
                    if (item.itemType == MediaLibraryItem.TYPE_DUMMY) continue
                    val title = item.title
                    val letter = if (title.isEmpty() || !Character.isLetter(title[0]) || ModelsHelper.isSpecialItem(item)) "#" else title.substring(0, 1).toUpperCase()
                    if (currentLetter === null || !TextUtils.equals(currentLetter, letter)) {
                        currentLetter = letter
                        if (array[letter].isNullOrEmpty()) array[letter] = mutableListOf()
                    }
                    array[letter]?.add(item)
                }
            }
            Medialibrary.SORT_DURATION -> {
                var currentLengthCategory: String? = null
                for (item in items) {
                    if (item.itemType == MediaLibraryItem.TYPE_DUMMY) continue
                    val length = ModelsHelper.getLength(item)
                    val lengthCategory = ModelsHelper.lengthToCategory(length)
                    if (currentLengthCategory == null || !TextUtils.equals(currentLengthCategory, lengthCategory)) {
                        currentLengthCategory = lengthCategory
                        if (array[currentLengthCategory].isNullOrEmpty()) array[currentLengthCategory] = mutableListOf()
                    }
                    array[currentLengthCategory]?.add(item)
                }
            }
            Medialibrary.SORT_RELEASEDATE -> {
                var currentYear: String? = null
                for (item in items) {
                    if (item.itemType == MediaLibraryItem.TYPE_DUMMY) continue
                    val year = ModelsHelper.getYear(item)
                    if (currentYear === null || !TextUtils.equals(currentYear, year)) {
                        currentYear = year
                        if (array[currentYear].isNullOrEmpty()) array[currentYear] = mutableListOf()
                    }
                    array[currentYear]?.add(item)
                }
            }
            Medialibrary.SORT_ARTIST -> {
                var currentArtist: String? = null
                for (item in items) {
                    if (item.itemType == MediaLibraryItem.TYPE_DUMMY) continue
                    val artist = (item as MediaWrapper).artist ?: ""
                    if (currentArtist === null || !TextUtils.equals(currentArtist, artist)) {
                        currentArtist = artist
                        if (array[currentArtist].isNullOrEmpty()) array[currentArtist] = mutableListOf()
                    }
                    array[currentArtist]?.add(item)
                }
            }
            Medialibrary.SORT_ALBUM -> {
                var currentAlbum: String? = null
                for (item in items) {
                    if (item.itemType == MediaLibraryItem.TYPE_DUMMY) continue
                    val album = (item as MediaWrapper).album ?: ""
                    if (currentAlbum === null || !TextUtils.equals(currentAlbum, album)) {
                        currentAlbum = album
                        if (array[currentAlbum].isNullOrEmpty()) array[currentAlbum] = mutableListOf()
                    }
                    array[currentAlbum]?.add(item)
                }
            }
        }
        if (sort == Medialibrary.SORT_DEFAULT || sort == Medialibrary.SORT_FILENAME || sort == Medialibrary.SORT_ALPHA)
            array.toSortedMap()
        else array
    }

    fun getHeader(context: Context, sort: Int, item: MediaLibraryItem, aboveItem: MediaLibraryItem?) = when (sort) {
        Medialibrary.SORT_DEFAULT,
        Medialibrary.SORT_FILENAME,
        Medialibrary.SORT_ALPHA -> {
            val letter = if (item.title.isEmpty() || !Character.isLetter(item.title[0]) || ModelsHelper.isSpecialItem(item)) "#" else item.title.substring(0, 1).toUpperCase()
            if (aboveItem == null) letter
            else {
                val previous = if (aboveItem.title.isEmpty() || !Character.isLetter(aboveItem.title[0]) || ModelsHelper.isSpecialItem(aboveItem)) "#" else aboveItem.title.substring(0, 1).toUpperCase()
                if (letter != previous) letter else null
            }
        }
        Medialibrary.SORT_DURATION -> {
            val length = getLength(item)
            val lengthCategory = lengthToCategory(length)
            if (aboveItem == null) lengthCategory
            else {
                val previous = lengthToCategory(getLength(aboveItem))
                if (lengthCategory != previous) lengthCategory else null
            }
        }
        Medialibrary.SORT_RELEASEDATE -> {
            val year = getYear(item)
            if (aboveItem == null) year
            else {
                val previous = getYear(aboveItem)
                if (year != previous) year else null
            }
        }
        Medialibrary.SORT_LASTMODIFICATIONDATE -> {
            val timestamp = (item as MediaWrapper).lastModified
            val category = getTimeCategory(timestamp)
            if (aboveItem == null) getTimeCategoryString(context, category)
            else {
                val prevCat = getTimeCategory((aboveItem as MediaWrapper).lastModified)
                if (prevCat != category) getTimeCategoryString(context, category) else null
            }
        }
        Medialibrary.SORT_ARTIST -> {
            val artist = (item as MediaWrapper).artist ?: ""
            if (aboveItem == null) artist
            else {
                val previous = (aboveItem as MediaWrapper).artist ?: ""
                if (artist != previous) artist else null
            }
        }
        Medialibrary.SORT_ALBUM -> {
            val album = (item as MediaWrapper).album ?: ""
            if (aboveItem == null) album
            else {
                val previous = (aboveItem as MediaWrapper).album ?: ""
                if (album != previous) album else null
            }
        }
        else -> null
    }

    private fun getTimeCategory(timestamp: Long): Int {
        val delta = (System.currentTimeMillis()/1000L) - timestamp
        return when {
            delta < LENGTH_WEEK -> 0
            delta < LENGTH_MONTH -> 1
            delta < LENGTH_YEAR -> 2
            delta < LENGTH_2_YEAR -> 3
            else -> 4
        }
    }

    private fun getTimeCategoryString(context: Context, cat: Int) = when (cat) {
        0 -> context.getString(R.string.time_category_new)
        1 -> context.getString(R.string.time_category_current_month)
        2 -> context.getString(R.string.time_category_current_year)
        3 -> context.getString(R.string.time_category_last_year)
        else -> context.getString(R.string.time_category_older)
    }

    private fun isSpecialItem(item: MediaLibraryItem) = item.itemType == MediaLibraryItem.TYPE_ARTIST
            && (item.id == 1L || item.id == 2L) || item.itemType == MediaLibraryItem.TYPE_ALBUM
            && item.title == Album.SpecialRes.UNKNOWN_ALBUM

    private fun getLength(media: MediaLibraryItem): Int {
        return when {
            media.itemType == MediaLibraryItem.TYPE_ALBUM -> (media as Album).duration
            media.itemType == MediaLibraryItem.TYPE_MEDIA -> (media as MediaWrapper).length.toInt()
            else -> 0
        }
    }

    private fun lengthToCategory(length: Int): String {
        val value: Int
        if (length == 0) return "-"
        if (length < 60000) return "< 1 min"
        if (length < 600000) {
            value = Math.floor((length / 60000).toDouble()).toInt()
            return value.toString() + " - " + (value + 1).toString() + " min"
        }
        return if (length < 3600000) {
            value = (10 * Math.floor((length / 600000).toDouble())).toInt()
            value.toString() + " - " + (value + 10).toString() + " min"
        } else {
            value = Math.floor((length / 3600000).toDouble()).toInt()
            value.toString() + " - " + (value + 1).toString() + " h"
        }
    }

    private fun getYear(media: MediaLibraryItem): String {
        return when (media.itemType) {
            MediaLibraryItem.TYPE_ALBUM -> if ((media as Album).releaseYear == 0) "-" else media.releaseYear.toString()
            MediaLibraryItem.TYPE_MEDIA -> if ((media as MediaWrapper).date == null) "-" else media.date
            else -> "-"
        }
    }

    fun getTracksCount(media: MediaLibraryItem): Int {
        return when (media.itemType) {
            MediaLibraryItem.TYPE_ALBUM -> (media as Album).tracksCount
            MediaLibraryItem.TYPE_PLAYLIST -> (media as Playlist).tracksCount
            else -> 0
        }
    }
}

object EmptyMLCallbacks : Medialibrary.MediaCb, Medialibrary.ArtistsCb, Medialibrary.AlbumsCb, Medialibrary.GenresCb, Medialibrary.PlaylistsCb {
    override fun onMediaAdded() {}
    override fun onMediaModified() {}
    override fun onMediaDeleted() {}
    override fun onArtistsDeleted() {}
    override fun onAlbumsDeleted() {}
    override fun onArtistsAdded() {}
    override fun onArtistsModified() {}
    override fun onAlbumsAdded() {}
    override fun onAlbumsModified() {}
    override fun onGenresAdded() {}
    override fun onGenresModified() {}
    override fun onGenresDeleted() {}
    override fun onPlaylistsAdded() {}
    override fun onPlaylistsModified() {}
    override fun onPlaylistsDeleted() {}
}

object EmptyPBSCallback : PlaybackService.Callback {
    override fun update() {}
    override fun onMediaEvent(event: Media.Event) {}
    override fun onMediaPlayerEvent(event: MediaPlayer.Event) {}
}

interface RefreshModel {
    fun refresh() : Boolean
}

fun SortableModel.canSortBy(sort: Int) = when(sort) {
    Medialibrary.SORT_DEFAULT -> true
    Medialibrary.SORT_ALPHA -> canSortByName()
    Medialibrary.SORT_FILENAME -> canSortByFileNameName()
    Medialibrary.SORT_DURATION -> canSortByDuration()
    Medialibrary.SORT_INSERTIONDATE -> canSortByInsertionDate()
    Medialibrary.SORT_LASTMODIFICATIONDATE -> canSortByLastModified()
    Medialibrary.SORT_RELEASEDATE -> canSortByReleaseDate()
    Medialibrary.SORT_FILESIZE -> canSortByFileSize()
    Medialibrary.SORT_ARTIST -> canSortByArtist()
    Medialibrary.SORT_ALBUM -> canSortByAlbum()
    Medialibrary.SORT_PLAYCOUNT -> canSortByPlayCount()
    else -> false
}