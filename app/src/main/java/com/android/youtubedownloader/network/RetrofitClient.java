package com.android.youtubedownloader.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class  RetrofitClient {

   // private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/";
    private static final String BASE_URL = "https://youtube-search.p.rapidapi.com/";
    private static RetrofitClient mInstance;
    private Retrofit retrofit;
    OkHttpClient baseOkHttpClient = new OkHttpClient.Builder()
            .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            .build();
    private RetrofitClient() {
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(baseOkHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static synchronized RetrofitClient getInstance() {
        if (mInstance == null) {
            mInstance = new RetrofitClient();
        }
        return mInstance;
    }

    public YoutubeApi getYoutubeService() {
        return retrofit.create(YoutubeApi.class);
    }

}
