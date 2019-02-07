/*
 * *************************************************************************
 *  SlidingPaneActivity.java
 * **************************************************************************
 *  Copyright © 2015-2018 VLC authors and VideoLAN
 *  Author: Geoffrey Métais
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *  ***************************************************************************
 */

package org.videolan.mudiAudioVideo.gui;

import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;

import org.videolan.libvlc.util.AndroidUtil;
import org.videolan.medialibrary.Medialibrary;
import org.videolan.mudiAudioVideo.BuildConfig;
import org.videolan.mudiAudioVideo.ExternalMonitor;
import org.videolan.mudiAudioVideo.MediaParsingService;
import org.videolan.mudiAudioVideo.MediaParsingServiceKt;
import org.videolan.mudiAudioVideo.PlaybackService;
import org.videolan.mudiAudioVideo.R;
import org.videolan.mudiAudioVideo.ScanProgress;
import org.videolan.mudiAudioVideo.gui.audio.AudioPlayer;
import org.videolan.mudiAudioVideo.gui.browser.StorageBrowserFragment;
import org.videolan.mudiAudioVideo.gui.helpers.BottomSheetBehavior;
import org.videolan.mudiAudioVideo.gui.helpers.UiTools;
import org.videolan.mudiAudioVideo.interfaces.IRefreshable;
import org.videolan.mudiAudioVideo.media.PlaylistManager;
import org.videolan.mudiAudioVideo.util.AndroidDevices;
import org.videolan.mudiAudioVideo.util.Constants;
import org.videolan.mudiAudioVideo.util.WeakHandler;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.ViewStubCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;

public class AudioPlayerContainerActivity extends BaseActivity {

    public static final String TAG = "VLC/AudioPlayerContainerActivity";

    protected AppBarLayout mAppBarLayout;
    protected Toolbar mToolbar;
    private TabLayout mTabLayout;
    protected AudioPlayer mAudioPlayer;
    private FrameLayout mAudioPlayerContainer;
    protected PlaybackService mService;
    public BottomSheetBehavior mBottomSheetBehavior;
    protected View mFragmentContainer;
    protected int mOriginalBottomPadding;
    private View mScanProgressLayout;
    private TextView mScanProgressText;
    private ProgressBar mScanProgressBar;

    protected boolean mPreventRescan = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Init Medialibrary if KO
        if (savedInstanceState != null) {
            if (AndroidUtil.isNougatOrLater)
                UiTools.setLocale(this);

            MediaParsingServiceKt.startMedialibrary(this, false, false, true);
        }
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        registerLiveData();
    }

    protected void initAudioPlayerContainerActivity() {
        mFragmentContainer = findViewById(R.id.fragment_placeholder);
        if (mFragmentContainer != null) mOriginalBottomPadding = mFragmentContainer.getPaddingBottom();
        mToolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);
        mAppBarLayout = findViewById(R.id.appbar);
        mTabLayout = findViewById(R.id.sliding_tabs);
        mAppBarLayout.setExpanded(true);
        mAudioPlayerContainer = findViewById(R.id.audio_player_container);
    }

    public void setTabLayoutVisibility(boolean show) {
        mTabLayout.setVisibility( show ? View.VISIBLE : View.GONE);
    }

    private float elevation = 0f;
    public void toggleAppBarElevation(final boolean elevate) {
        if (!AndroidUtil.isLolliPopOrLater) return;
        if (elevation == 0f) elevation = getResources().getDimensionPixelSize(R.dimen.default_appbar_elevation);
        mAppBarLayout.post(new Runnable() {
            @Override
            public void run() {
                ViewCompat.setElevation(mAppBarLayout, elevate ? elevation : 0f);
            }
        });
    }

    private void initAudioPlayer() {
        findViewById(R.id.audio_player_stub).setVisibility(View.VISIBLE);
        mAudioPlayer = (AudioPlayer) getSupportFragmentManager().findFragmentById(R.id.audio_player);
        mBottomSheetBehavior = (BottomSheetBehavior) BottomSheetBehavior.from(mAudioPlayerContainer);
        mBottomSheetBehavior.setPeekHeight(getResources().getDimensionPixelSize(R.dimen.player_peek_height));
        mBottomSheetBehavior.setBottomSheetCallback(mAudioPlayerBottomSheetCallback);
        showTipViewIfNeeded(R.id.audio_player_tips, Constants.PREF_AUDIOPLAYER_TIPS_SHOWN);
    }

    public void expandAppBar() {
        mAppBarLayout.setExpanded(true);
    }

    @Override
    protected void onStart() {
        ExternalMonitor.INSTANCE.subscribeStorageCb(this);
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mPreventRescan = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        ExternalMonitor.INSTANCE.unsubscribeStorageCb(this);
    }

    @Override
    public void onBackPressed() {
        if (slideDownAudioPlayer()) return;
        super.onBackPressed();
    }

    protected Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.fragment_placeholder);
    }

    public Menu getMenu() {
        return mToolbar.getMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
                // Current fragment loaded
                final Fragment current = getCurrentFragment();
                if (current instanceof StorageBrowserFragment && ((StorageBrowserFragment) current).goBack())
                    return true;
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void updateLib() {
        if (mPreventRescan) {
            mPreventRescan = false;
            return;
        }
        final FragmentManager fm = getSupportFragmentManager();
        final Fragment current = fm.findFragmentById(R.id.fragment_placeholder);
        if (current instanceof IRefreshable) ((IRefreshable) current).refresh();
    }

    /**
     * Show a tip view.
     * @param stubId the stub of the tip view
     * @param settingKey the setting key to check if the view must be displayed or not.
     */
    public void showTipViewIfNeeded(final int stubId, final String settingKey) {
        if (BuildConfig.DEBUG) return;
        View vsc = findViewById(stubId);
        if (vsc != null && !mSettings.getBoolean(settingKey, false) && !AndroidDevices.showTvUi(this)) {
            View v = ((ViewStubCompat)vsc).inflate();
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeTipViewIfDisplayed();
                }
            });
            TextView okGotIt = v.findViewById(R.id.okgotit_button);
            okGotIt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeTipViewIfDisplayed();
                    SharedPreferences.Editor editor = mSettings.edit();
                    editor.putBoolean(settingKey, true);
                    editor.apply();
                }
            });
        }
    }

    /**
     * Remove the current tip view if there is one displayed.
     */
    public void removeTipViewIfDisplayed() {
        View tips = findViewById(R.id.audio_tips);
        if (tips != null) ((ViewGroup) tips.getParent()).removeView(tips);
    }
    /**
     * Show the audio player.
     */
    public void showAudioPlayer() {
        if (isFinishing()) return;
        mActivityHandler.sendEmptyMessageDelayed(ACTION_SHOW_PLAYER, 100L);
    }

    private void showAudioPlayerImpl() {
        if (!isAudioPlayerReady()) initAudioPlayer();
        if (mAudioPlayerContainer.getVisibility() != View.VISIBLE) {
            mAudioPlayerContainer.setVisibility(View.VISIBLE);
        }
        if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
        mBottomSheetBehavior.setHideable(false);
        mBottomSheetBehavior.lock(false);
    }

    /**
     * Slide down the audio player.
     * @return true on success else false.
     */
    public boolean slideDownAudioPlayer() {
        if (isAudioPlayerReady() && mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            return true;
        }
        return false;
    }

    /**
     * Slide up and down the audio player depending on its current state.
     */
    public void slideUpOrDownAudioPlayer() {
        if (!isAudioPlayerReady() || mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) return;
        mBottomSheetBehavior.setState(mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED?
                BottomSheetBehavior.STATE_COLLAPSED : BottomSheetBehavior.STATE_EXPANDED);
    }

    /**
     * Hide the audio player.
     */
    public void hideAudioPlayer() {
        if (isFinishing()) return;
        mActivityHandler.sendEmptyMessage(ACTION_HIDE_PLAYER);
    }

    private void hideAudioPlayerImpl() {
        if (!isAudioPlayerReady()) return;
        mBottomSheetBehavior.setHideable(true);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void updateProgressVisibility(boolean show) {
        final int visibility = show ? View.VISIBLE : View.GONE;
        if (mScanProgressLayout != null && mScanProgressLayout.getVisibility() == visibility) return;
        if (show) mActivityHandler.sendEmptyMessageDelayed(ACTION_DISPLAY_PROGRESSBAR, 1000);
        else {
            mActivityHandler.removeMessages(ACTION_DISPLAY_PROGRESSBAR);
            UiTools.setViewVisibility(mScanProgressLayout, visibility);
        }
    }

    private void showProgressBar() {
        final View vsc = findViewById(R.id.scan_viewstub);
        if (vsc != null) {
            vsc.setVisibility(View.VISIBLE);
            mScanProgressLayout = findViewById(R.id.scan_progress_layout);
            mScanProgressText = findViewById(R.id.scan_progress_text);
            mScanProgressBar = findViewById(R.id.scan_progress_bar);
        } else if (mScanProgressLayout != null)
            mScanProgressLayout.setVisibility(View.VISIBLE);
        final ScanProgress sp = MediaParsingService.Companion.getProgress().getValue();
        if (sp != null) {
            if (mScanProgressText != null) mScanProgressText.setText(sp.getDiscovery());
            if (mScanProgressBar != null) mScanProgressBar.setProgress(sp.getParsing());
        }
    }

    protected void updateContainerPadding(boolean show) {
        if (mFragmentContainer == null) return;
        int factor = show ? 1 : 0;
        final int peekHeight = show && mBottomSheetBehavior != null ? mBottomSheetBehavior.getPeekHeight() : 0;
        mFragmentContainer.setPadding(mFragmentContainer.getPaddingLeft(),
                mFragmentContainer.getPaddingTop(), mFragmentContainer.getPaddingRight(),
                mOriginalBottomPadding+factor*peekHeight);
    }

    private void applyMarginToProgressBar(int marginValue) {
        if (mScanProgressLayout != null && mScanProgressLayout.getVisibility() == View.VISIBLE) {
            final CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) mScanProgressLayout.getLayoutParams();
            lp.bottomMargin = marginValue;
            mScanProgressLayout.setLayoutParams(lp);
        }
    }

    final Handler mActivityHandler = new ProgressHandler(this);
    final AudioPlayerBottomSheetCallback mAudioPlayerBottomSheetCallback = new AudioPlayerBottomSheetCallback();

    private static final int ACTION_DISPLAY_PROGRESSBAR = 1339;
    private static final int ACTION_SHOW_PLAYER = 1340;
    private static final int ACTION_HIDE_PLAYER = 1341;

    public boolean isAudioPlayerReady() {
        return mAudioPlayer != null;
    }

    public boolean isAudioPlayerExpanded() {
        return isAudioPlayerReady() && mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED;
    }

    private class AudioPlayerBottomSheetCallback extends BottomSheetBehavior.BottomSheetCallback {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            AudioPlayerContainerActivity.this.onPlayerStateChanged(bottomSheet, newState);
            mAudioPlayer.onStateChanged(newState);
            switch (newState) {
                case BottomSheetBehavior.STATE_COLLAPSED:
                    removeTipViewIfDisplayed();
                    break;
                case BottomSheetBehavior.STATE_HIDDEN:
                    removeTipViewIfDisplayed();
                    break;
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
    }

    protected void onPlayerStateChanged(View bottomSheet, int newState) {}

    private void registerLiveData() {
        PlaylistManager.Companion.getShowAudioPlayer().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean showPlayer) {
                if (showPlayer) showAudioPlayer();
                else {
                    hideAudioPlayer();
                    if (mBottomSheetBehavior != null) mBottomSheetBehavior.lock(true);
                }
            }
        });
        MediaParsingService.Companion.getProgress().observe(this, new Observer<ScanProgress>() {
            @Override
            public void onChanged(@Nullable ScanProgress scanProgress) {
                if (scanProgress == null || !Medialibrary.getInstance().isWorking()) {
                    updateProgressVisibility(false);
                    return;
                }
                updateProgressVisibility(true);
                if (mScanProgressText != null) mScanProgressText.setText(scanProgress.getDiscovery());
                if (mScanProgressBar != null) mScanProgressBar.setProgress(scanProgress.getParsing());
            }
        });
        Medialibrary.getState().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean started) {
                if (started != null) updateProgressVisibility(started);
            }
        });
        MediaParsingService.Companion.getNewStorages().observe(this, new Observer<List<String>>() {
            @Override
            public void onChanged(@Nullable List<String> devices) {
                if (devices == null) return;
                for (String device : devices) UiTools.newStorageDetected(AudioPlayerContainerActivity.this, device);
                MediaParsingService.Companion.getNewStorages().setValue(null);
            }
        });
    }

    private static class ProgressHandler extends WeakHandler<AudioPlayerContainerActivity> {

        ProgressHandler(AudioPlayerContainerActivity owner) {
            super(owner);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final AudioPlayerContainerActivity owner = getOwner();
            if (owner == null) return;
            switch (msg.what){
                case ACTION_DISPLAY_PROGRESSBAR:
                    removeMessages(ACTION_DISPLAY_PROGRESSBAR);
                    owner.showProgressBar();
                    break;
                case ACTION_SHOW_PLAYER:
                    owner.showAudioPlayerImpl();
                    owner.updateContainerPadding(true);
                    owner.applyMarginToProgressBar(owner.mBottomSheetBehavior.getPeekHeight());
                    break;
                case ACTION_HIDE_PLAYER:
                    removeMessages(ACTION_SHOW_PLAYER);
                    owner.hideAudioPlayerImpl();
                    owner.updateContainerPadding(false);
                    owner.applyMarginToProgressBar(0);
                    break;
            }
        }
    }
}
