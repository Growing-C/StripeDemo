package com.example.stripedemo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.stripedemo.net.ApiWrapper;
import com.example.stripedemo.net.RetrofitFactory;
import com.example.stripedemo.net.StripeService;
import com.example.stripedemo.rx.BaseRxAction;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

/**
 * Description :
 * Author :cgy
 * Date :2019/4/29
 */
public class StripeManager {
    private final String STRIP_PUB_KEY = "pk_test_sxgqTrwz5alXy47e9YBkJMm5000wQJsBtw";
    private final String STRIP_API_KEY = "sk_test_0oamHqJqUkai2LqEGL8T1yo700RB7LLmi1";
    Stripe mStripe;

    Context mContext;
    Customer mCustomer;

    public StripeManager(Context context) {
        PaymentConfiguration.init(STRIP_PUB_KEY);
        this.mStripe = new Stripe(context, STRIP_PUB_KEY);
        this.mContext = context;
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
                    Card card = sources.get(0).asCard();
                    Log.i("test", "brand:" + card.getBrand());
                    Log.i("test", "getLast4:" + card.getLast4());
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

    //TODO:暂时不明用处
    public void paymentIntent(PaymentMethodCreateParams.Card card, String clientSecret) {
        PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParams.create(card, null);
        PaymentIntentParams paymentIntentParams =
                PaymentIntentParams.createConfirmPaymentIntentWithPaymentMethodCreateParams(
                        paymentMethodCreateParams, clientSecret,
                        "yourapp://post-authentication-return-url");

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


    //</editor-fold>

    public interface ProgressListener {
        void onStringResponse(String string);

        void onError(String errorMsg);
    }
}
