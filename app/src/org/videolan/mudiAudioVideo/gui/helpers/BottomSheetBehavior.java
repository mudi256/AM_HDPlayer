package org.videolan.mudiAudioVideo.gui.helpers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.videolan.mudiAudioVideo.gui.AudioPlayerContainerActivity;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;


public class BottomSheetBehavior<V extends View> extends com.google.android.material.bottomsheet.BottomSheetBehavior<V> {
    public static final String TAG = "VLC/BottomSheetBehavior";
    private boolean lock = false;

    public BottomSheetBehavior() {
        setHideable(true);
    }

    public BottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void lock(boolean lock) {
        this.lock = lock;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (lock) return false;
        try {
            return super.onInterceptTouchEvent(parent, child, event);
        } catch (NullPointerException ignored) {
            // BottomSheetBehavior receives input events too soon and mNestedScrollingChildRef is not set yet.
            return false;
        }
    }

    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        if (lock || getState() == BottomSheetBehavior.STATE_EXPANDED) return;
        if (dyConsumed > 0 && child.getVisibility() == View.VISIBLE) {
            final AudioPlayerContainerActivity activity = (AudioPlayerContainerActivity) child.getContext();
            activity.hideAudioPlayer();
        } else if (dyConsumed < 0 && getState() == BottomSheetBehavior.STATE_HIDDEN) {
            final AudioPlayerContainerActivity activity = (AudioPlayerContainerActivity) child.getContext();
            activity.showAudioPlayer();
        }
    }

    @Override
    public void onStopNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View target, int type) {
        if (lock) return;
        try {
            super.onStopNestedScroll(coordinatorLayout, child, target, type);
        } catch (NullPointerException ignored) {}
    }

    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target) {
        if (lock) return;
        try {
            super.onStopNestedScroll(coordinatorLayout, child, target);
        } catch (NullPointerException ignored) {
            //Same crash, weakref not already set.
        }
    }

    @Override
    public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        if (lock) return;
        try {
            super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type);
        } catch (NullPointerException ignored) {
            //Same crash, weakref not already set.
        }
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, V child, View target, int dx, int dy, int[] consumed) {
        if (lock) return;
        try {
            super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed);
        } catch (NullPointerException ignored) {
            //Same crash, weakref not already set.
        }
    }

    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, V child, View target, float velocityX, float velocityY) {
        if (lock) return false;
        try {
            return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);
        } catch (NullPointerException ignored) {
            //Same crash, weakref not already set.
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (lock) return false;
        try {
            return super.onTouchEvent(parent, child, event);
        } catch (NullPointerException ignored) {
            return false;
        }
    }
}
