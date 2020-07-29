package com.app.androidkt.speechapi;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;


interface QueryApi {

    @Headers("Content-Type: application/json")
    @POST("webhooks/rest/webhook")
    Call<List<QueryResponse>> queryResponse(@Body Query query);

}
