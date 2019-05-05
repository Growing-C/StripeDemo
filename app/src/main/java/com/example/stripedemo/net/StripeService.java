
package com.example.stripedemo.net;

import java.util.Map;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.FieldMap;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * A Retrofit service used to communicate with a server.
 */
public interface StripeService {

    @GET("createEphemeralKey")
    Observable<ResponseBody> createEphemeralKey(@Query("customerId") String cus, @Query("version") String version);

    @GET("addCard")
    Observable<ResponseBody> uploadCard(@Query("customerId") String cus, @Query("token") String token);

    @GET("charge")
    Observable<ResponseBody> createCharge(@Query("customerId") String cus, @Query("source") String sourceId,
                                          @Query("amount") int amount);


    @POST("create_intent")
    Observable<ResponseBody> createPaymentIntent(@FieldMap Map<String, Object> params);
}
