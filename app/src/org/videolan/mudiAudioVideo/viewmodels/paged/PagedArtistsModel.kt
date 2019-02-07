package org.videolan.mudiAudioVideo.viewmodels.paged

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.videolan.medialibrary.Medialibrary
import org.videolan.medialibrary.media.Artist
import org.videolan.mudiAudioVideo.util.EmptyMLCallbacks


class PagedArtistsModel(context: Context, private var showAll: Boolean = false): MLPagedModel<Artist>(context), Medialibrary.ArtistsCb by EmptyMLCallbacks {

    override fun onArtistsAdded() {
        refresh()
    }

    fun showAll(show: Boolean) {
        showAll = show
    }

    override fun getAll() : Array<Artist> = medialibrary.getArtists(showAll, sort, desc)

    override fun getPage(loadSize: Int, startposition: Int): Array<Artist> {
        return if (filterQuery == null) medialibrary.getPagedArtists(showAll, sort, desc, loadSize, startposition)
        else medialibrary.searchArtist(filterQuery, sort, desc, loadSize, startposition)
    }

    override fun getTotalCount() = if (filterQuery == null) medialibrary.getArtistsCount(showAll)
    else medialibrary.getArtistsCount(filterQuery)

    override fun onMedialibraryReady() {
        super.onMedialibraryReady()
        medialibrary.addArtistsCb(this)
    }

    override fun onCleared() {
        medialibrary.removeArtistsCb(this)
        super.onCleared()
    }

    class Factory(private val context: Context, private val showAll: Boolean): ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return PagedArtistsModel(context, showAll) as T
        }
    }
}