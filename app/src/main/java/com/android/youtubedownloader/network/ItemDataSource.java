package com.android.youtubedownloader.network;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.paging.PageKeyedDataSource;

import com.android.youtubedownloader.activities.PlaylistActivity;
import com.android.youtubedownloader.models.Item;
import com.android.youtubedownloader.models.YoutubeApiResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.android.youtubedownloader.activities.PlaylistActivity.progressDialog;

public class ItemDataSource extends PageKeyedDataSource<String, Item> {

    private static final String PART = "snippet";
    private static final String TYPE = "video";
    public static final int MAX_SIZE = 25;

    @Override
    public void loadInitial(@NonNull LoadInitialParams<String> params, @NonNull final LoadInitialCallback<String, Item> callback) {

        RetrofitClient.getInstance()
                .getYoutubeService().getVideos(PART, MAX_SIZE, PlaylistActivity.keyWord, "1a204ffc1amsh9cd74c7b68633a6p18aa4djsnedf040e2a64b","youtube-v31.p.rapidapi.com")
                .enqueue(new Callback<YoutubeApiResponse>() {
                    @Override
                    public void onResponse(Call<YoutubeApiResponse> call, Response<YoutubeApiResponse> response) {
                        YoutubeApiResponse youtubeApiResponse = response.body();

                        if (youtubeApiResponse != null) {
                            progressDialog.dismiss();
                            // Fetch data and pass the result  null for the previous page
                            callback.onResult(youtubeApiResponse.getItems(), null, youtubeApiResponse.getNextPageToken());
                        }

                        if (youtubeApiResponse == null) {
                            Log.d("Quota", "Quota of Youtube Data Api is finished");
                            progressDialog.dismiss();
                            return;
                        }

                        if (!response.isSuccessful()) {
                            Log.d("Result", "Code: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<YoutubeApiResponse> call, Throwable t) {
                        Log.d("onFailure", "Failed");
                        progressDialog.dismiss();
                    }
                });
    }

    @Override
    public void loadBefore(@NonNull final LoadParams<String> params, @NonNull final LoadCallback<String, Item> callback) {
        RetrofitClient.getInstance()
                .getYoutubeService().getVideos(PART, MAX_SIZE, PlaylistActivity.keyWord, "1a204ffc1amsh9cd74c7b68633a6p18aa4djsnedf040e2a64b","youtube-v31.p.rapidapi.com")
                .enqueue(new Callback<YoutubeApiResponse>() {
                    @Override
                    public void onResponse(Call<YoutubeApiResponse> call, Response<YoutubeApiResponse> response) {
                        YoutubeApiResponse youtubeApiResponse = response.body();

                        String key = "";
                        if (youtubeApiResponse != null) {
                            key = youtubeApiResponse.getPrevPageToken();
                            Log.d("loadBefore", key + "");

                            // Passing the loaded database and the previous page key
                            callback.onResult(youtubeApiResponse.getItems(), key);
                        }
                    }

                    @Override
                    public void onFailure(Call<YoutubeApiResponse> call, Throwable t) {
                        Log.d("onFailure", "Failed");
                    }
                });

    }

    @Override
    public void loadAfter(@NonNull final LoadParams<String> params, @NonNull final LoadCallback<String, Item> callback) {
        RetrofitClient.getInstance()
                .getYoutubeService().getVideos(PART, MAX_SIZE, PlaylistActivity.keyWord, "1a204ffc1amsh9cd74c7b68633a6p18aa4djsnedf040e2a64b","youtube-v31.p.rapidapi.com")
                .enqueue(new Callback<YoutubeApiResponse>() {
                    @Override
                    public void onResponse(Call<YoutubeApiResponse> call, Response<YoutubeApiResponse> response) {
                        YoutubeApiResponse youtubeApiResponse = response.body();

                        if (youtubeApiResponse != null) {
                            // If the response has next page, load it
                            String key = youtubeApiResponse.getNextPageToken();

                            Log.d("loadAfter", key + "");

                            // Passing the loaded database and next page value
                            callback.onResult(youtubeApiResponse.getItems(), key);
                        }
                    }

                    @Override
                    public void onFailure(Call<YoutubeApiResponse> call, Throwable t) {
                        Log.d("onFailure", "Failed");
                    }
                });
    }

}
