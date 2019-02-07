package org.videolan.mudiAudioVideo.gui.dialogs;

import android.content.DialogInterface;
import androidx.fragment.app.DialogFragment;

public class DismissDialogFragment extends DialogFragment {
    protected DialogInterface.OnDismissListener onDismissListener;

    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (onDismissListener != null) {
            onDismissListener.onDismiss(dialog);
        }
    }
}
