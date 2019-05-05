package com.example.stripedemo.controller;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.example.stripedemo.dialog.ProgressDialogFragment;


/**
 * Class used to show and hide the progress spinner.
 */
public class ProgressDialogController {

    @NonNull
    private final Resources mRes;
    @NonNull
    private final FragmentManager mFragmentManager;
    @Nullable
    private ProgressDialogFragment mProgressFragment;

    public ProgressDialogController(@NonNull FragmentManager fragmentManager,
                                    @NonNull Resources res) {
        mFragmentManager = fragmentManager;
        mRes = res;
    }

    public void show(@StringRes int resId) {

        if (mProgressFragment != null && mProgressFragment.getDialog().isShowing()) {//不能用isVisible
            Log.e("test", "++++++++++++++++++++++++  ");
            mProgressFragment.dismiss();
            mProgressFragment = null;
        }
        mProgressFragment = ProgressDialogFragment.newInstance(mRes.getString(resId));
        mProgressFragment.show(mFragmentManager, "progress");
    }

    public void dismiss() {
        if (mProgressFragment != null) {
            mProgressFragment.dismiss();
        }
    }
}
