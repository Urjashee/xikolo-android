package de.xikolo.controller.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import de.xikolo.GlobalApplication;
import de.xikolo.R;

public class ProgressDialog extends DialogFragment {

    public static final String TAG = ProgressDialog.class.getSimpleName();

    public static final String KEY_TITLE = "multiple_title";
    public static final String KEY_MESSAGE = "multiple_message";

    private String title;

    private String message;

    public ProgressDialog() {
    }

    public static ProgressDialog getInstance() {
        return getInstance(GlobalApplication.getInstance().getString(R.string.dialog_progress_title),
                GlobalApplication.getInstance().getString(R.string.dialog_progress_message));
    }

    public static ProgressDialog getInstance(String title, String message) {
        ProgressDialog fragment = new ProgressDialog();
        Bundle args = new Bundle();
        args.putString(KEY_TITLE, title);
        args.putString(KEY_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null) {
            title = getArguments().getString(KEY_TITLE);
            message = getArguments().getString(KEY_MESSAGE);
        }

        android.app.ProgressDialog dialog = new android.app.ProgressDialog(getActivity());
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        setCancelable(false);

        return dialog;
    }

}