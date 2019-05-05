package com.example.stripedemo.controller;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;

import com.example.stripedemo.R;
import com.example.stripedemo.dialog.ErrorDialogFragment;


/**
 * A convenience class to handle displaying error dialogs.
 */
public class ErrorDialogHandler {

    @NonNull private final FragmentManager mFragmentManager;

    public ErrorDialogHandler(@NonNull FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }

    public void show(@NonNull String errorMessage) {
        ErrorDialogFragment.newInstance(R.string.validationErrors, errorMessage)
                .show(mFragmentManager, "error");
    }
}
