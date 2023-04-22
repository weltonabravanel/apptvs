package com.app.braziltube.activities;

import static com.app.braziltube.utils.Constant.LOCALHOST_ADDRESS;
import static com.solodroid.ads.sdk.util.Constant.ADMOB;
import static com.solodroid.ads.sdk.util.Constant.AD_STATUS_ON;
import static com.solodroid.ads.sdk.util.Constant.GOOGLE_AD_MANAGER;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.app.braziltube.BuildConfig;
import com.app.braziltube.Config;
import com.app.braziltube.R;
import com.app.braziltube.databases.prefs.AdsPref;
import com.app.braziltube.databases.prefs.SharedPref;
import com.app.braziltube.models.Ads;
import com.app.braziltube.models.App;
import com.app.braziltube.models.Settings;
import com.app.braziltube.rests.ApiInterface;
import com.app.braziltube.rests.RestAdapter;
import com.app.braziltube.utils.AdsManager;
import com.app.braziltube.utils.Tools;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ActivitySplash extends AppCompatActivity {

    public static final String TAG = "ActivitySplash";
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    ImageView img_splash;
    SharedPref sharedPref;
    AdsManager adsManager;
    AdsPref adsPref;
    App app;
    Settings settings;
    Ads ads;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_splash);
        Tools.setNavigation(this);
        adsManager = new AdsManager(this);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);

        img_splash = findViewById(R.id.img_splash);
        if (sharedPref.getIsDarkTheme()) {
            img_splash.setImageResource(R.drawable.bg_splash_dark);
        } else {
            img_splash.setImageResource(R.drawable.bg_splash_default);
        }

        if (adsPref.getAdStatus().equals(AD_STATUS_ON)) {
            Application application = getApplication();
            if (adsPref.getAdType().equals(ADMOB)) {
                if (!adsPref.getAdMobAppOpenAdId().equals("0")) {
                    ((MyApplication) application).showAdIfAvailable(ActivitySplash.this, this::requestConfig);
                } else {
                    requestConfig();
                }
            } else if (adsPref.getAdType().equals(GOOGLE_AD_MANAGER)) {
                if (!adsPref.getAdManagerAppOpenAdId().equals("0")) {
                    ((MyApplication) application).showAdIfAvailable(ActivitySplash.this, this::requestConfig);
                } else {
                    requestConfig();
                }
            } else {
                requestConfig();
            }
        } else {
            requestConfig();
        }

    }

    private void requestConfig() {
        if (Config.SERVER_KEY.contains("XXXXX")) {
            new AlertDialog.Builder(this)
                    .setTitle("App not configured")
                    .setMessage("Please put your Server Key and Rest API Key from settings menu in your admin panel to AppConfig, you can see the documentation for more detailed instructions.")
                    .setPositiveButton(getString(R.string.dialog_ok), (dialogInterface, i) -> startMainActivity())
                    .setCancelable(false)
                    .show();
        } else {
            String data = Tools.decode(Config.SERVER_KEY);
            String[] results = data.split("_applicationId_");
            String baseUrl = results[0].replace("localhost", LOCALHOST_ADDRESS);
            String applicationId = results[1];
            sharedPref.saveConfig(baseUrl, applicationId);

            if (applicationId.equals(BuildConfig.APPLICATION_ID)) {
                if (Tools.isConnect(this)) {
                    requestAPI(baseUrl);
                } else {
                    startMainActivity();
                }
            } else {
                String message = "applicationId does not match, applicationId in your app is : " + BuildConfig.APPLICATION_ID +
                        "\n\n But your Server Key is registered with applicationId : " + applicationId + "\n\n" +
                        "Please update your Server Key with the appropriate registration applicationId that is used in your Android project.";
                new AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage(Html.fromHtml(message))
                        .setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }
        }
    }

    private void requestAPI(String apiUrl) {
        ApiInterface apiInterface = RestAdapter.createAPI(apiUrl);
        mCompositeDisposable.add(apiInterface.getConfig(BuildConfig.APPLICATION_ID, Config.REST_API_KEY)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((resp, throwable) -> {
                    if (resp != null && resp.status.equals("ok")) {
                        app = resp.app;
                        settings = resp.settings;
                        ads = resp.ads;

                        Tools.saveAds(adsPref, ads);

                        sharedPref.saveCredentials(
                                settings.youtube_api_key,
                                settings.more_apps_url,
                                settings.privacy_policy,
                                app.redirect_url
                        );

                        if (app.status.equals("0")) {
                            Intent intent = new Intent(getApplicationContext(), ActivityRedirect.class);
                            startActivity(intent);
                            finish();
                            Log.d(TAG, "App status is suspended");
                        } else {
                            startMainActivity();
                            Log.d("Response", "Ads Data is saved");
                        }
                        Log.d("Response", "Ads Data is saved");
                    } else {
                        startMainActivity();
                    }
                }));
    }

    private void startMainActivity() {
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }, Config.DELAY_SPLASH);
    }

}