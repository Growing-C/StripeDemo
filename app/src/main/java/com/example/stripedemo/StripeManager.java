package com.example.stripedemo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.example.stripedemo.net.ApiWrapper;
import com.example.stripedemo.net.RetrofitFactory;
import com.example.stripedemo.net.StripeService;
import com.example.stripedemo.rx.BaseRxAction;
import com.example.stripedemo.rx.RxUtil;
import com.stripe.android.CustomerSession;
import com.stripe.android.EphemeralKeyProvider;
import com.stripe.android.EphemeralKeyUpdateListener;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.Stripe;
import com.stripe.android.StripeError;
import com.stripe.android.TokenCallback;
import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.model.Card;
import com.stripe.android.model.Customer;
import com.stripe.android.model.CustomerSource;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentIntentParams;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import okhttp3.ResponseBody;

/**
 * Description :
 * Author :cgy
 * Date :2019/4/29
 */
public class StripeManager {
    private final String STRIP_PUB_KEY = "pk_test_sxgqTrwz5alXy47e9YBkJMm5000wQJsBtw";
    private final String STRIP_API_KEY = "sk_test_0oamHqJqUkai2LqEGL8T1yo700RB7LLmi1";
    private final String APP_RETURN_URL = "stripe_auth_return://stripe_demo";
    //                schema://host:port/path/query

    private static StripeManager sInstance;
    Stripe mStripe;

    Context mContext;
    Customer mCustomer;

    private StripeManager(Context context) {
        PaymentConfiguration.init(STRIP_PUB_KEY);
        this.mStripe = new Stripe(context, STRIP_PUB_KEY);
        this.mContext = context;
    }

    public static StripeManager getInstance(Context context) {
        synchronized (StripeManager.class) {
            if (sInstance == null) {
                sInstance = new StripeManager(context);
            }
            return sInstance;
        }
    }

    public static void main(String[] args) {
        final ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(10);
        queue.add("111");
        queue.add("222");
        queue.add("333");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                System.out.println(System.currentTimeMillis() + "add thread start");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                queue.add("999");
            }
        };
        new Thread(runnable).start();
        try {
            System.out.println("current thread:" + Thread.currentThread().getName());
            while (true) {
                System.out.println(System.currentTimeMillis() + " take start");
                String s = queue.take();
                System.out.println(System.currentTimeMillis() + "--take:" + s);

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void initCustomerSession(final BaseRxAction mAction, @NonNull final ProgressListener mProgressListener) {
        CustomerSession.initCustomerSession(new EphemeralKeyProvider() {
            @Override
            public void createEphemeralKey(@NonNull String apiVersion, @NonNull final EphemeralKeyUpdateListener keyUpdateListener) {
                Log.i("test", "createEphemeralKey apiVersion:" + apiVersion);//2017-06-05
                Map<String, String> apiParamMap = new HashMap<>();
                apiParamMap.put("api_version", apiVersion);

                mAction.addSubscription(ApiWrapper.getInstance().createEphemeralKey("", apiVersion)
                        , new DisposableObserver<ResponseBody>() {
                            @Override
                            public void onNext(ResponseBody response) {
                                try {
                                    String rawKey = response.string();
                                    keyUpdateListener.onKeyUpdate(rawKey);
                                    if (rawKey.startsWith("Error: ")) {
                                        mProgressListener.onError(rawKey);
                                    } else
                                        mProgressListener.onStringResponse(rawKey);
                                } catch (IOException ignored) {
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                e.printStackTrace();
                                Log.e("test", e.toString());
                                mProgressListener.onError(e.getMessage());
                            }

                            @Override
                            public void onComplete() {

                            }
                        });


            }
        });
    }

    public void retrieveCustomer() {
        CustomerSession.getInstance().retrieveCurrentCustomer(new CustomerSession.CustomerRetrievalListener() {
            @Override
            public void onCustomerRetrieved(@NonNull Customer customer) {
                Log.i("test", "retrieveCurrentCustomer  onCustomerRetrieved :" + customer.toJson());
                List<CustomerSource> sources = customer.getSources();
                mCustomer = customer;
                if (sources.size() > 0) {
                    Log.i("test", "sources  size :" + sources.size());

                    Source card = sources.get(0).asSource();

//                    Log.i("test", "brand:" + card.getBrand());
//                    Log.i("test", "getLast4:" + card.getLast4());
//                    Log.i("test", "getCurrency:" + card.getCurrency());
                    for (int i = 0; i < sources.size(); i++) {
                        Log.i("test", i + "--getSourceType:" + sources.get(i).getSourceType());
                        Log.i("test", i + "--card?:" + (sources.get(i).asCard() == null));
                        Log.i("test", i + "--source?:" + (sources.get(i).asSource() == null));
                    }
//                    mCardBrand = card.getBrand();
//                    mLast4 = card.getLast4();
                }
            }

            @Override
            public void onError(int errorCode, @Nullable String errorMessage, @Nullable StripeError stripeError) {
                Log.e("test", "retrieveCurrentCustomer  onError :" + errorMessage);
            }
        });
    }

    public void updateCustomer() {
        CustomerSession.getInstance().updateCurrentCustomer(new CustomerSession.CustomerRetrievalListener() {
            @Override
            public void onCustomerRetrieved(@NonNull Customer customer) {
                List<CustomerSource> sources = customer.getSources();
                mCustomer = customer;
                if (sources != null && sources.size() > 0) {
                    Log.i("test", "updateCustomer sources  size :" + sources.size());

                }
            }

            @Override
            public void onError(int errorCode, @Nullable String errorMessage, @Nullable StripeError stripeError) {

            }
        });
    }

    /**
     * 获取默认选中的卡
     *
     * @return
     */
    public CustomerSource getDefaultSource() {
        if (mCustomer == null || mCustomer.getSources() == null || mCustomer.getSources().size() == 0)
            return null;

        if (TextUtils.isEmpty(mCustomer.getDefaultSource()))
            return mCustomer.getSources().get(0);

        return mCustomer.getSourceById(mCustomer.getDefaultSource());
    }

    /**
     * 切换用户或者退出 清空session
     */
    public void endCustomerSession() {
        CustomerSession.endCustomerSession();
    }

    public Customer getCustomer() {
        return mCustomer;
    }

    public List<CustomerSource> getSources() {
        return mCustomer.getSources();
    }

    //<editor-fold desc="卡支付相关 ">

    public void createTokenByCard(Card card, TokenCallback tokenCallback) {
        mStripe.createToken(
                card,
                tokenCallback
        );
    }

    public void uploadCardByToken(BaseRxAction mAction, String tokenId, DisposableObserver observer) {
        mAction.addSubscription(ApiWrapper.getInstance().uploadCard(mCustomer.getId(), tokenId)
                , observer);
    }

    /**
     * 使用paymentIntent支付流程
     * <p>
     * 1.Create a PaymentIntent and make its client secret accessible to your application
     * (使用source可以跳过第二步)2.Collect card information and create a PaymentMethodCreateParams object
     * 3.Confirm the PaymentIntent
     * 4.Redirect and authenticate the payment if necessary
     *
     * @param clientSecret
     */
    public void paymentIntentFromSource(final Source source, final String clientSecret) {
        Observable.create(new ObservableOnSubscribe<PaymentIntent>() {
            @Override
            public void subscribe(ObservableEmitter<PaymentIntent> emitter) throws Exception {
                PaymentIntentParams paymentIntentParams =
                        PaymentIntentParams.createConfirmPaymentIntentWithSourceIdParams(
                                source.getId(), clientSecret,
                                APP_RETURN_URL);

                try {
//                    3.Confirm the PaymentIntent
                    PaymentIntent paymentIntent = mStripe.confirmPaymentIntentSynchronous(
                            paymentIntentParams, STRIP_PUB_KEY);


                    emitter.onNext(paymentIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                    emitter.onError(e);
                }
            }
        }).flatMap(new Function<PaymentIntent, ObservableSource<Uri>>() {
            @Override
            public ObservableSource<Uri> apply(PaymentIntent paymentIntent) throws Exception {
                PaymentIntent.Status status = PaymentIntent.Status
                        .fromCode(paymentIntent.getStatus());
                Uri redirectUrl;
                if (PaymentIntent.Status.RequiresAction == status) {
                    redirectUrl = paymentIntent.getRedirectUrl();
                    if (redirectUrl != null) {
                        Log.e("test", "paymentIntentFromSource RequiresAction:" + redirectUrl);
//                        startActivity(new Intent(Intent.ACTION_VIEW, redirectUrl));
                    }
                } else {
                    Log.e("test", "paymentIntentFromSource success:");
                    redirectUrl = new Uri.Builder().build();
                    // Show success message
                }

                return Observable.just(redirectUrl);
            }
        }).compose(RxUtil.<Uri>applySchedulersJobUI())
                .subscribe(new DisposableObserver<Uri>() {
                    @Override
                    public void onNext(Uri uri) {
                        Log.e("test", "paymentIntentFromSource onNext uri:" + uri.toString());
//                        startActivity(new Intent(Intent.ACTION_VIEW, redirectUrl));
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    /**
     * 使用paymentIntent支付流程
     * <p>
     * 1.Create a PaymentIntent and make its client secret accessible to your application
     * 2.Collect card information and create a PaymentMethodCreateParams object
     * 3.Confirm the PaymentIntent
     * 4.Redirect and authenticate the payment if necessary
     *
     * @param card
     * @param clientSecret
     */
    public void paymentIntent(Card card, String clientSecret) {
        PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParams.create(card.toPaymentMethodParamsCard(), null);
        PaymentIntentParams paymentIntentParams =
                PaymentIntentParams.createConfirmPaymentIntentWithPaymentMethodCreateParams(
                        paymentMethodCreateParams, clientSecret,
                        APP_RETURN_URL);

        try {
            PaymentIntent paymentIntent = mStripe.confirmPaymentIntentSynchronous(
                    paymentIntentParams, STRIP_PUB_KEY);
        } catch (AuthenticationException e) {
            e.printStackTrace();
        } catch (InvalidRequestException e) {
            e.printStackTrace();
        } catch (APIConnectionException e) {
            e.printStackTrace();
        } catch (APIException e) {
            e.printStackTrace();
        }
    }

    public void retrievePaymentIntent(String clientSecret) {
        final PaymentIntentParams retrievePaymentIntentParams =
                PaymentIntentParams.createRetrievePaymentIntentParams(clientSecret);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                // retrieve the PaymentIntent on a background thread
                try {
                    final PaymentIntent paymentIntent =
                            mStripe.retrievePaymentIntentSynchronous(
                                    retrievePaymentIntentParams,
                                    STRIP_PUB_KEY);

                } catch (AuthenticationException e) {
                    e.printStackTrace();
                } catch (InvalidRequestException e) {
                    e.printStackTrace();
                } catch (APIConnectionException e) {
                    e.printStackTrace();
                } catch (APIException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    //</editor-fold>

    public interface ProgressListener {
        void onStringResponse(String string);

        void onError(String errorMsg);
    }
}
