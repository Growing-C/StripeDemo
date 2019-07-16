package com.example.stripedemo;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentIntentParams;

public class StripeAuthReturnAcitivity extends AppCompatActivity {
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.e("test", "onNewIntent~~~~~" + intent.getData());
        if (intent.getData() != null && intent.getData().getQuery() != null) {
            final String host = intent.getData().getHost();
            final String clientSecret = intent.getData().getQueryParameter(
                    "payment_intent_client_secret");



            // If you had a dialog open when your user went elsewhere, remember to close it here.
//            mRedirectDialogController.dismissDialog();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stripe_auth_return_acitivity);
    }
}
