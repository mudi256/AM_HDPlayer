/**
 * **************************************************************************
 * PickTimeFragment.java
 * ****************************************************************************
 * Copyright © 2015 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * ***************************************************************************
 */
package org.videolan.mudiAudioVideo.gui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.videolan.libvlc.MediaPlayer;
import org.videolan.medialibrary.Tools;
import org.videolan.mudiAudioVideo.PlaybackService;
import org.videolan.mudiAudioVideo.R;
import org.videolan.mudiAudioVideo.gui.helpers.UiTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

public class SelectChapterDialog extends DismissDialogFragment implements Observer<PlaybackService> {

    public final static String TAG = "VLC/SelectChapterDialog";

    private ListView mChapterList;

    protected PlaybackService mService;

    public static SelectChapterDialog newInstance() {
        return new SelectChapterDialog();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_select_chapter, container);
        mChapterList = view.findViewById(R.id.chapter_list);

        getDialog().setCancelable(true);
        getDialog().setCanceledOnTouchOutside(true);
        Window window = getDialog().getWindow();
        window.setBackgroundDrawableResource(UiTools.getResourceFromAttribute(getActivity(), R.attr.rounded_bg));
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        PlaybackService.Companion.getService().observe(this, this);
    }

    private void initChapterList() {
        final MediaPlayer.Chapter[] chapters = mService.getChapters(-1);
        int chaptersCount = chapters != null ? chapters.length : 0;
        if (chaptersCount <= 1) return;

        final List<Map<String, String>> chapterList = new ArrayList<Map<String, String>>();

        for (int i = 0; i < chaptersCount; i++) {
            String name;
            if (chapters[i].name == null || chapters[i].name.equals(""))
                name = getResources().getString(R.string.chapter) + " " + i;
            else
                name = chapters[i].name;
            chapterList.add(putData(name, Tools.millisToString(chapters[i].timeOffset)));
        }

        String[] from = { "name", "time" };
        int[] to = { R.id.chapter_name, R.id.chapter_time };
        SimpleAdapter adapter = new SimpleAdapter(getActivity(), chapterList,
                R.layout.dialog_select_chapter_item, from, to);

        mChapterList.setAdapter(adapter);
        mChapterList.setSelection(mService.getChapterIdx());
        mChapterList.setItemChecked(mService.getChapterIdx(), true);
        mChapterList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mService.setChapterIdx(position);
                dismiss();
            }
        });
    }

    private Map<String, String> putData(String name, String time) {
        Map<String, String> item = new HashMap<String, String>();
        item.put("name", name);
        item.put("time", time);
        return item;
    }

    @Override
    public void onChanged(PlaybackService service) {
        if (service != null) {
            mService = service;
            initChapterList();
        } else mService = null;
    }
}
