package com.example.stripedemo.net;

import com.example.stripedemo.rx.RxUtil;

import io.reactivex.Observable;
import okhttp3.ResponseBody;

/**
 * Description :
 * Author :cgy
 * Date :2019/4/30
 */
public class ApiWrapper {
    static ApiWrapper mInstance;

    StripeService mService;

    private ApiWrapper() {
        mService = RetrofitFactory.getInstance().create(StripeService.class);
    }

    public static ApiWrapper getInstance() {
        synchronized (ApiWrapper.class) {
            if (mInstance == null) {
                mInstance = new ApiWrapper();
            }
        }
        return mInstance;
    }

    public Observable<ResponseBody> createEphemeralKey(String cus, String version) {
        return mService.createEphemeralKey(cus, version).compose(RxUtil.<ResponseBody>applySchedulersJobUI());
    }

    public Observable<ResponseBody> uploadCard(String cus, String token) {
        return mService.uploadCard(cus, token).compose(RxUtil.<ResponseBody>applySchedulersJobUI());
    }

    public Observable<ResponseBody> createCharge(String cus, String sourceId, int amount) {
        return mService.createCharge(cus, sourceId, amount).compose(RxUtil.<ResponseBody>applySchedulersJobUI());
    }
}
