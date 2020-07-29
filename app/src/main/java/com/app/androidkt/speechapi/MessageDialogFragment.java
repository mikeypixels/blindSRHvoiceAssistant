package com.app.androidkt.speechapi;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import java.util.Objects;


/**
 * A simple dialog with a message.
 *
 * <p>The calling {@link android.app.Activity} needs to implement {@link
 * MessageDialogFragment.Listener}.</p>
 */
public class MessageDialogFragment extends AppCompatDialogFragment {

    public interface Listener {
        /**
         * Called when the dialog is dismissed.
         */
        void onMessageDialogDismissed();
    }

    private static final String ARG_MESSAGE = "message";

    /**
     * Creates a new instance of {@link MessageDialogFragment}.
     *
     * @param message The message to be shown on the dialog.
     * @return A newly created dialog fragment.
     */
    static MessageDialogFragment newInstance(String message) {
        final MessageDialogFragment fragment = new MessageDialogFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        assert getArguments() != null;
        return new AlertDialog.Builder(Objects.requireNonNull(getContext()))
                .setMessage(getArguments().getString(ARG_MESSAGE))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((Listener) Objects.requireNonNull(getActivity())).onMessageDialogDismissed();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        ((Listener) Objects.requireNonNull(getActivity())).onMessageDialogDismissed();
                    }
                })
                .create();
    }

}
