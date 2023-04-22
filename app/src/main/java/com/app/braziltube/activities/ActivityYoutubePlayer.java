package com.app.braziltube.activities;

import static com.app.braziltube.Config.PRESS_BACK_TWICE_TO_CLOSE_PLAYER;
import static com.app.braziltube.utils.Constant.PLAYER_MODE_LANDSCAPE;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.app.braziltube.R;
import com.app.braziltube.databases.prefs.SharedPref;
import com.app.braziltube.utils.Tools;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerView;

public class ActivityYoutubePlayer extends YouTubeBaseActivity implements YouTubePlayer.OnInitializedListener {

    private static final int RECOVERY_REQUEST = 1;
    private YouTubePlayerView youTubeView;
    private MyPlayerStateChangeListener playerStateChangeListener;
    private String id = "";
    SharedPref sharedPref;
    private long exitTime = 0;
    RelativeLayout parent_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_youtube);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.getWindow().setNavigationBarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBlack));
            this.getWindow().setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBlack));
            this.getWindow().getDecorView().setSystemUiVisibility(0);
        }

        sharedPref = new SharedPref(this);
        if (sharedPref.getPlayerMode() == PLAYER_MODE_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        Bundle bundle = getIntent().getExtras();
        id = (String) bundle.get("id");

        parent_view = findViewById(R.id.parent_view);
        youTubeView = findViewById(R.id.youtube_view);

        if (Tools.isAppInstalled(this, "com.google.android.youtube")) {
            youTubeView.initialize(sharedPref.getYoutubeAPIKey(), this);
            playerStateChangeListener = new MyPlayerStateChangeListener();
        } else {
            errorDialog();
        }

    }

    public void errorDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.whops))
                .setCancelable(false)
                .setMessage("You must install and enable the YouTube app to play this video.")
                .setPositiveButton("Install", (dialogInterface, i) -> {
                    finish();
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.google.android.youtube")));
                })
                .setNegativeButton("Later", (dialogInterface, i) -> finish())
                .show();
    }

    @Override
    public void onInitializationSuccess(Provider provider, YouTubePlayer player, boolean wasRestored) {
        player.setPlayerStateChangeListener(playerStateChangeListener);
        if (!wasRestored) {
            player.loadVideo(id);
        }
    }

    @Override
    public void onInitializationFailure(Provider provider, YouTubeInitializationResult errorReason) {
        if (errorReason.isUserRecoverableError()) {
            errorReason.getErrorDialog(this, RECOVERY_REQUEST).show();
        } else {
            Toast.makeText(this, getResources().getString(R.string.error_player), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECOVERY_REQUEST) {
            // Retry initialization if user performed a recovery action
            getYouTubePlayerProvider().initialize(sharedPref.getYoutubeAPIKey(), this);
        }
    }

    protected Provider getYouTubePlayerProvider() {
        return youTubeView;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private static final class MyPlayerStateChangeListener implements YouTubePlayer.PlayerStateChangeListener {

        @Override
        public void onLoading() {
            // Called when the player is loading a video
            // At this point, it's not ready to accept commands affecting playback such as play() or pause()
        }

        @Override
        public void onLoaded(String s) {
            // Called when a video is done loading.
            // Playback methods such as play(), pause() or seekToMillis(int) may be called after this callback.
        }

        @Override
        public void onAdStarted() {
            // Called when playback of an advertisement starts.
        }

        @Override
        public void onVideoStarted() {
            // Called when playback of the video starts.
        }

        @Override
        public void onVideoEnded() {
            // Called when the video reaches its end.
            //showInterstitialAd();
        }

        @Override
        public void onError(YouTubePlayer.ErrorReason errorReason) {
            // Called when an error occurs.
        }
    }

    @Override
    public void onBackPressed() {
        closePlayer();
    }

    public void closePlayer() {
        if (PRESS_BACK_TWICE_TO_CLOSE_PLAYER) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Snackbar.make(parent_view, getString(R.string.press_again_to_close_player), Snackbar.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

}