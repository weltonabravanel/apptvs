package com.app.braziltube.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.app.braziltube.BuildConfig;
import com.app.braziltube.Config;
import com.app.braziltube.R;
import com.app.braziltube.activities.ActivityNotificationDetail;
import com.app.braziltube.activities.ActivityStreamPlayer;
import com.app.braziltube.activities.ActivityWebView;
import com.app.braziltube.activities.ActivityYoutubePlayer;
import com.app.braziltube.activities.MainActivity;
import com.app.braziltube.databases.prefs.AdsPref;
import com.app.braziltube.databases.prefs.SharedPref;
import com.app.braziltube.models.Ads;
import com.app.braziltube.models.Channel;
import com.google.android.material.snackbar.Snackbar;

import java.nio.charset.StandardCharsets;

public class Tools {

    public static void getTheme(Context context) {
        SharedPref sharedPref = new SharedPref(context);
        if (sharedPref.getIsDarkTheme()) {
            context.setTheme(R.style.AppDarkTheme);
        } else {
            context.setTheme(R.style.AppTheme);
        }
    }

    public static void setNavigation(Activity activity) {
        SharedPref sharedPref = new SharedPref(activity);
        if (sharedPref.getIsDarkTheme()) {
            Tools.darkNavigation(activity);
        } else {
            Tools.lightNavigation(activity);
        }
        setLayoutDirection(activity);
    }

    public static void setLayoutDirection(Activity activity) {
        if (Config.ENABLE_RTL_MODE) {
            activity.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }
    }

    public static void darkNavigation(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.getWindow().setNavigationBarColor(ContextCompat.getColor(activity, R.color.colorStatusBarDark));
            activity.getWindow().setStatusBarColor(ContextCompat.getColor(activity, R.color.colorStatusBarDark));
            activity.getWindow().getDecorView().setSystemUiVisibility(0);
        }
    }

    public static void lightNavigation(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.getWindow().setNavigationBarColor(ContextCompat.getColor(activity, R.color.colorWhite));
            activity.getWindow().setStatusBarColor(ContextCompat.getColor(activity, R.color.colorPrimaryDark));
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    public static void setupToolbar(AppCompatActivity activity, Toolbar toolbar, String title, boolean backButton) {
        SharedPref sharedPref = new SharedPref(activity);
        activity.setSupportActionBar(toolbar);
        if (sharedPref.getIsDarkTheme()) {
            toolbar.setBackgroundColor(activity.getResources().getColor(R.color.colorToolbarDark));
        } else {
            toolbar.setBackgroundColor(activity.getResources().getColor(R.color.colorPrimary));
        }
        final ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(backButton);
            activity.getSupportActionBar().setHomeButtonEnabled(backButton);
            activity.getSupportActionBar().setTitle(title);
        }
    }

    public static void notificationOpenHandler(Context context, Intent getIntent) {
        long unique_id = getIntent.getLongExtra("unique_id", 0);
        long post_id = getIntent.getLongExtra("post_id", 0);
        String title = getIntent.getStringExtra("title");
        String link = getIntent.getStringExtra("link");
        if (post_id == 0) {
            if (link != null && !link.equals("")) {
                Intent intent = new Intent(context, ActivityWebView.class);
                intent.putExtra("title", title);
                intent.putExtra("url", link);
                context.startActivity(intent);
            }
        } else if (post_id > 0) {
            Intent intent = new Intent(context, ActivityNotificationDetail.class);
            intent.putExtra("id", String.valueOf(post_id));
            context.startActivity(intent);
        } else {
            Intent intent = new Intent(context, MainActivity.class);
            context.startActivity(intent);
        }
        Log.d("push_notification", "unique id : " + unique_id);
        Log.d("push_notification", "link : " + link);
        Log.d("push_notification", "post id : " + post_id);
    }

    public static boolean isNetworkAvailable(Activity activity) {
        ConnectivityManager connectivity = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network[] networks = connectivity.getAllNetworks();
        NetworkInfo networkInfo;
        for (Network mNetwork : networks) {
            networkInfo = connectivity.getNetworkInfo(mNetwork);
            if (networkInfo.getState().equals(NetworkInfo.State.CONNECTED)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isConnect(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null) {
                return activeNetworkInfo.isConnected() || activeNetworkInfo.isConnectedOrConnecting();
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static void getCategoryPosition(Activity activity, Intent intent) {
        if (intent.hasExtra("category_position")) {
            String select = intent.getStringExtra("category_position");
            if (select != null) {
                if (select.equals("category_position")) {
                    if (activity instanceof MainActivity) {
                        ((MainActivity) activity).selectCategory();
                    }
                }
            }
        }
    }

    public static void share(Activity activity, String title) {
        String share_title = Html.fromHtml(title).toString();
        String share_content = Html.fromHtml(activity.getString(R.string.share_content)).toString();
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, share_title + "\n\n" + share_content + "\n\n" + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
        sendIntent.setType("text/plain");
        activity.startActivity(sendIntent);
    }

    public static void displayContent(Activity activity, WebView webView, String htmlData) {
        SharedPref sharedPref = new SharedPref(activity);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setFocusableInTouchMode(false);
        webView.setFocusable(false);
        webView.getSettings().setDefaultTextEncodingName("UTF-8");

        WebSettings webSettings = webView.getSettings();
        Resources res = activity.getResources();
        int fontSize = res.getInteger(R.integer.font_size);
        webSettings.setDefaultFontSize(fontSize);

        String mimeType = "text/html; charset=UTF-8";
        String encoding = "utf-8";

        String bg_paragraph;
        if (sharedPref.getIsDarkTheme()) {
            bg_paragraph = "<style type=\"text/css\">body{color: #eeeeee;} a{color:#ffffff; font-weight:bold;}";
        } else {
            bg_paragraph = "<style type=\"text/css\">body{color: #000000;} a{color:#1e88e5; font-weight:bold;}";
        }

        String font_style_default = "<style type=\"text/css\">@font-face {font-family: MyFont;src: url(\"file:///android_asset/font/custom_font.ttf\")}body {font-family: MyFont; font-size: medium; overflow-wrap: break-word; word-wrap: break-word; -ms-word-break: break-all; word-break: break-all; word-break: break-word; -ms-hyphens: auto; -moz-hyphens: auto; -webkit-hyphens: auto; hyphens: auto;}</style>";

        String text = "<html><head>"
                + font_style_default
                + "<style>img{max-width:100%;height:auto;} figure{max-width:100%;height:auto;} iframe{width:100%;}</style> "
                + bg_paragraph
                + "</style></head>"
                + "<body>"
                + htmlData
                + "</body></html>";

        String text_rtl = "<html dir='rtl'><head>"
                + font_style_default
                + "<style>img{max-width:100%;height:auto;} figure{max-width:100%;height:auto;} iframe{width:100%;}</style> "
                + bg_paragraph
                + "</style></head>"
                + "<body>"
                + htmlData
                + "</body></html>";

        if (Config.ENABLE_RTL_MODE) {
            webView.loadDataWithBaseURL(null, text_rtl, mimeType, encoding, null);
        } else {
            webView.loadDataWithBaseURL(null, text, mimeType, encoding, null);
        }
    }

    public static void startPlayer(Activity activity, View view, Channel channel) {
        if (isNetworkAvailable(activity)) {
            if (channel.channel_type != null && channel.channel_type.equals("YOUTUBE")) {
                Intent intent = new Intent(activity, ActivityYoutubePlayer.class);
                intent.putExtra("id", channel.video_id);
                activity.startActivity(intent);
            } else {
                Intent intent = new Intent(activity, ActivityStreamPlayer.class);
                intent.putExtra("url", channel.channel_url);
                intent.putExtra("user_agent", channel.user_agent);
                activity.startActivity(intent);
            }
        } else {
            Snackbar.make(view, activity.getResources().getString(R.string.network_required), Snackbar.LENGTH_SHORT).show();
        }
    }

    public static void saveAds(AdsPref adsPref, Ads ads) {
        if (ads.test_mode == 1) {
            adsPref.saveAds(
                    ads.ad_status.replace("on", "1"),
                    ads.ad_type,
                    ads.backup_ads,
                    "pub-3940256099942544",
                    "ca-app-pub-3940256099942544~3347511713",
                    "ca-app-pub-3940256099942544/6300978111",
                    "ca-app-pub-3940256099942544/1033173712",
                    "ca-app-pub-3940256099942544/2247696110",
                    "ca-app-pub-3940256099942544/3419835294",
                    "/6499/example/banner",
                    "/6499/example/interstitial",
                    "/6499/example/native",
                    "/6499/example/app-open",
                    "IMG_16_9_APP_INSTALL#1102290040176998_1102321626840506",
                    "IMG_16_9_APP_INSTALL#1102290040176998_1103218190084183",
                    "IMG_16_9_APP_INSTALL#1102290040176998_1142394442833224",
                    "0",
                    "4089993",
                    "banner",
                    "video",
                    "0",
                    "0",
                    "0",
                    "0",
                    "0",
                    "85460dcd",
                    "DefaultBanner",
                    "DefaultInterstitial",
                    ads.interstitial_ad_interval,
                    ads.native_ad_interval,
                    ads.native_ad_index,
                    ads.date_time
            );

            adsPref.setTestMode(true);
            Log.d("AdNetwork", "Ad Test Mode ON");
        } else {
            adsPref.saveAds(
                    ads.ad_status.replace("on", "1"),
                    ads.ad_type,
                    ads.backup_ads,
                    ads.admob_publisher_id,
                    ads.admob_app_id,
                    ads.admob_banner_unit_id,
                    ads.admob_interstitial_unit_id,
                    ads.admob_native_unit_id,
                    ads.admob_app_open_ad_unit_id,
                    ads.ad_manager_banner_unit_id,
                    ads.ad_manager_interstitial_unit_id,
                    ads.ad_manager_native_unit_id,
                    ads.ad_manager_app_open_ad_unit_id,
                    ads.fan_banner_unit_id,
                    ads.fan_interstitial_unit_id,
                    ads.fan_native_unit_id,
                    ads.startapp_app_id,
                    ads.unity_game_id,
                    ads.unity_banner_placement_id,
                    ads.unity_interstitial_placement_id,
                    ads.applovin_banner_ad_unit_id,
                    ads.applovin_interstitial_ad_unit_id,
                    ads.applovin_native_ad_manual_unit_id,
                    ads.applovin_banner_zone_id,
                    ads.applovin_interstitial_zone_id,
                    ads.ironsource_app_key,
                    ads.ironsource_banner_placement_name,
                    ads.ironsource_interstitial_placement_name,
                    ads.interstitial_ad_interval,
                    ads.native_ad_interval,
                    ads.native_ad_index,
                    ads.date_time
            );

            adsPref.setTestMode(false);
            Log.d("AdNetwork", "Ad Test Mode OFF");
        }
    }

    public static String decode(String code) {
        return decodeBase64(decodeBase64(decodeBase64(code)));
    }

    public static String decodeBase64(String code) {
        byte[] valueDecoded = Base64.decode(code.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
        return new String(valueDecoded);
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return packageManager.getApplicationInfo(packageName, 0).enabled;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

}