package com.app.braziltube.databases.prefs;

import static com.app.braziltube.Config.CATEGORY_VIEW_TYPE;
import static com.app.braziltube.Config.CHANNEL_VIEW_TYPE;
import static com.app.braziltube.Config.DEFAULT_PLAYER_SCREEN_ORIENTATION;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPref {

    Context context;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";

    public SharedPref(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public Boolean getIsDarkTheme() {
        return sharedPreferences.getBoolean("theme", false);
    }

    public void setIsDarkTheme(Boolean isDarkTheme) {
        editor.putBoolean("theme", isDarkTheme);
        editor.apply();
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime);
        editor.apply();
    }

    public boolean isFirstTimeLaunch() {
        return sharedPreferences.getBoolean(IS_FIRST_TIME_LAUNCH, true);
    }

    public Integer getChannelViewType() {
        return sharedPreferences.getInt("video_list", CHANNEL_VIEW_TYPE);
    }

    public void updateChannelViewType(int position) {
        editor.putInt("video_list", position);
        editor.apply();
    }

    public Integer getCategoryViewType() {
        return sharedPreferences.getInt("category_list", CATEGORY_VIEW_TYPE);
    }

    public void updateCategoryViewType(int position) {
        editor.putInt("category_list", position);
        editor.apply();
    }

    public Integer getPlayerMode() {
        return sharedPreferences.getInt("player_mode", DEFAULT_PLAYER_SCREEN_ORIENTATION);
    }

    public void updatePlayerMode(int position) {
        editor.putInt("player_mode", position);
        editor.apply();
    }

    public void saveConfig(String api_url, String application_id) {
        editor.putString("api_url", api_url);
        editor.putString("application_id", application_id);
        editor.apply();
    }

    public String getBaseUrl() {
        return sharedPreferences.getString("api_url", "http://10.0.2.2/the_stream");
    }

    public String getApplicationId() {
        return sharedPreferences.getString("application_id", "com.app.braziltube2023");
    }

    public void saveCredentials(String youtube_api_key, String more_apps_url, String privacy_policy, String redirect_url) {
        editor.putString("youtube_api_key", youtube_api_key);
        editor.putString("more_apps_url", more_apps_url);
        editor.putString("privacy_policy", privacy_policy);
        editor.putString("redirect_url", redirect_url);
        editor.apply();
    }

    public String getYoutubeAPIKey() {
        return sharedPreferences.getString("youtube_api_key", "0");
    }

    public String getPrivacyPolicy() {
        return sharedPreferences.getString("privacy_policy", "");
    }

    public String getMoreAppsUrl() {
        return sharedPreferences.getString("more_apps_url", "https://play.google.com/store/apps/developer?id=Solodroid");
    }

    public Integer getInAppReviewToken() {
        return sharedPreferences.getInt("in_app_review_token", 0);
    }

    public void updateInAppReviewToken(int value) {
        editor.putInt("in_app_review_token", value);
        editor.apply();
    }

    public String getRedirectUrl() {
        return sharedPreferences.getString("redirect_url", "");
    }

}
