package com.example.stripedemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.example.stripedemo.controller.ErrorDialogHandler;
import com.example.stripedemo.controller.ProgressDialogController;
import com.example.stripedemo.net.ApiWrapper;
import com.example.stripedemo.rx.BaseRxAction;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;
import com.stripe.android.model.Token;
import com.stripe.android.view.CardMultilineWidget;
import com.stripe.android.view.PaymentMethodsActivity;
import com.stripe.android.view.PaymentMethodsActivityStarter;

import java.io.IOException;

import io.reactivex.observers.DisposableObserver;
import okhttp3.ResponseBody;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_SELECT_SOURCE = 55;
    CardMultilineWidget mInputCardV;
    TextView mSelectedCardV;
    StripeManager mStripManager;
    private ErrorDialogHandler mErrorDialogHandler;
    private ProgressDialogController mProgressDialogController;

    protected BaseRxAction mAction;

    Source mSelectedSource;

    CreditCardView mCardV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mAction = new BaseRxAction();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mCardV = findViewById(R.id.card_v);
        mSelectedCardV = findViewById(R.id.selected_card);
        mInputCardV = findViewById(R.id.input_card);
        mStripManager = new StripeManager(this);

        mErrorDialogHandler = new ErrorDialogHandler(getSupportFragmentManager());
        mProgressDialogController = new ProgressDialogController(
                getSupportFragmentManager(),
                getResources()
        );

        mStripManager.initCustomerSession(mAction, new StripeManager.ProgressListener() {
            @Override
            public void onStringResponse(String string) {
                Log.i("test", "initCustomerSession onStringResponse :" + string);

            }

            @Override
            public void onError(String errorMsg) {
                Log.i("test", "initCustomerSession onError:" + errorMsg);
            }
        });

        mStripManager.retrieveCustomer();
        mInputCardV.setCardNumber("4242424242424242");

        mCardV.setCard(new Card("4242424242424242", 11, 20, "423"));
        mCardV.setSelected(true);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAction.unSubscribe();
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.upload_card:
                uploadCard();
                break;
            case R.id.select_source:
                new PaymentMethodsActivityStarter(this).startForResult(REQUEST_CODE_SELECT_SOURCE);
                break;
            case R.id.pay:
                pay();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_SOURCE && resultCode == RESULT_OK) {
            String selectedSource = data.getStringExtra(PaymentMethodsActivity.EXTRA_SELECTED_PAYMENT);
            Source source = Source.fromString(selectedSource);
            Log.e("test", "onActivityResult source type:" + source.getType());

            Log.e("test", "onActivityResult source  redirect:" + Source.REDIRECT + "--" + source.getFlow());
            // Note: it isn't possible for a null or non-card source to be returned.
            if (source != null && Source.CARD.equals(source.getType())) {
                SourceCardData cardData = (SourceCardData) source.getSourceTypeModel();
                String cardString = cardData.getBrand() + getString(R.string.ending_in) + cardData.getLast4();
                Log.i("test", "onActivityResult cardString:" + cardString);
//                mSelectedSourceTextView.setText(buildCardString(cardData));
                mSelectedCardV.setText(cardString);
            }
            mSelectedSource = source;
        }
    }

    /**
     * 支付，单位为分  500 即为5元
     */
    private void pay() {
        if (mSelectedSource == null) {
            mErrorDialogHandler.show("No Pay source selected");
            return;
        }
        if (Source.REDIRECT.equals(mSelectedSource.getFlow())) {
            mErrorDialogHandler.show("source need redirect");
            return;
        }

        mProgressDialogController.show(R.string.progressMessage);
        mAction.addSubscription(ApiWrapper.getInstance().createCharge(mStripManager.getCustomer().getId(),
                mSelectedSource.getId(), 500), new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody o) {
                try {
                    String responseStr = o.string();
                    Log.i("test", responseStr);
                    if (responseStr.startsWith("Error: ")) {
                        mErrorDialogHandler.show(responseStr);
                    } else {
                        Toast.makeText(MainActivity.this, "支付成功！", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e("test", e.toString());
                mErrorDialogHandler.show(e.toString());
                mProgressDialogController.dismiss();
            }

            @Override
            public void onComplete() {
                Log.e("test", "onComplete");
                mProgressDialogController.dismiss();
            }
        });

    }

    /**
     * 上传卡片信息
     */
    public void uploadCard() {
//        可用的测试卡号 4242424242424242 4000056655665556 5555555555554444
        Card cardToSave = mInputCardV.getCard();
        if (cardToSave == null) {
            mErrorDialogHandler.show("Invalid Card Data");
            return;
        }
        Log.d("test", "number:" + cardToSave.getNumber());
        Log.d("test", "expDate y:" + cardToSave.getExpYear() + " m:" + cardToSave.getExpMonth());
        Log.d("test", "cvc:" + cardToSave.getCVC());
        Log.d("test", "brand:" + cardToSave.getBrand());
        cardToSave.setName("Customer Test");
//        cardToSave.setAddressZip("223456");


        mProgressDialogController.show(R.string.progressMessage);
        mStripManager.createTokenByCard(cardToSave, new TokenCallback() {
            @Override
            public void onSuccess(@NonNull Token token) {
                // Send token to your server
                Log.d("test", "createTokenByCard onSuccess:" + token.toString());
//                testOnlyPay(token.getId());
//                if (1 > 0) {
//                    return;
//                }
                mStripManager.uploadCardByToken(mAction, token.getId(), new DisposableObserver<ResponseBody>() {
                    @Override
                    public void onNext(ResponseBody o) {
                        try {
                            String responseStr = o.string();
                            Log.i("test", responseStr);
                            if (responseStr.startsWith("Error: ")) {
                                mErrorDialogHandler.show(responseStr);
                            } else {
                                Toast.makeText(MainActivity.this, "添加成功！", Toast.LENGTH_SHORT).show();
                                mStripManager.updateCustomer();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("test", "uploadCardByToken onError:" + e.toString());
                        mProgressDialogController.dismiss();
                    }

                    @Override
                    public void onComplete() {
                        Log.e("test", "uploadCardByToken");
                        mProgressDialogController.dismiss();
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception error) {
                Log.e("test", error.getLocalizedMessage());
                // Show localized error message
                mErrorDialogHandler.show(error.getLocalizedMessage());
                mProgressDialogController.dismiss();
            }
        });

    }

    /**
     * 通过card获取到token之后直接支付
     *
     * @param token
     */
    private void testOnlyPay(String token) {
        mProgressDialogController.show(R.string.create_payment_intent);
        mAction.addSubscription(ApiWrapper.getInstance().createCharge("nothing",
                token, 500), new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody o) {
                try {
                    String responseStr = o.string();
                    Log.i("test", responseStr);
                    if (responseStr.startsWith("Error: ")) {
                        mErrorDialogHandler.show(responseStr);
                    } else {
                        Toast.makeText(MainActivity.this, "支付成功！", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e("test", e.toString());
                mErrorDialogHandler.show(e.toString());
                mProgressDialogController.dismiss();
            }

            @Override
            public void onComplete() {
                Log.e("test", "createCharge onComplete");
                mProgressDialogController.dismiss();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
