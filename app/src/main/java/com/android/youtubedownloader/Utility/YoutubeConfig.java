package com.android.youtubedownloader.Utility;

import com.android.youtubedownloader.activities.AppConstant;

public class YoutubeConfig {

    public YoutubeConfig() {
    }

    private static final String API_KEY = AppConstant.API_KEY;

    public static String getApiKey() {
        return API_KEY;
    }
}
