package com.app.braziltube.activities;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.app.braziltube.BuildConfig;
import com.app.braziltube.Config;
import com.app.braziltube.R;
import com.app.braziltube.databases.dao.AppDatabase;
import com.app.braziltube.databases.dao.ChannelEntity;
import com.app.braziltube.databases.dao.DAO;
import com.app.braziltube.databases.prefs.AdsPref;
import com.app.braziltube.databases.prefs.SharedPref;
import com.app.braziltube.models.Channel;
import com.app.braziltube.utils.Constant;
import com.app.braziltube.utils.Tools;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Picasso;

public class ActivityChannelDetailOffline extends AppCompatActivity {

    ImageView channel_image;
    TextView channel_name, channel_category, title_toolbar;
    WebView channel_description;
    boolean flag_read_later;
    ImageButton btn_favorite;
    SharedPref sharedPref;
    AdsPref adsPref;
    private Channel channel;
    private DAO dao;
    CoordinatorLayout parent_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_detail);
        Tools.setNavigation(this);

        channel = (Channel) getIntent().getSerializableExtra(Constant.EXTRA_OBJC);

        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        dao = AppDatabase.getDatabase(this).get();

        parent_view = findViewById(R.id.parent_view);
        title_toolbar = findViewById(R.id.title_toolbar);
        btn_favorite = findViewById(R.id.btn_favorite);
        channel_image = findViewById(R.id.channel_image);
        channel_name = findViewById(R.id.channel_name);
        channel_category = findViewById(R.id.channel_category);
        channel_description = findViewById(R.id.channel_description);

        setupToolbar();
        displayData();
        addToFavorite();

        findViewById(R.id.btn_share).setOnClickListener(view -> {
            String share_title = android.text.Html.fromHtml(getResources().getString(R.string.share_title) + " " + channel.channel_name).toString();
            String share_text = android.text.Html.fromHtml(getResources().getString(R.string.share_content)).toString();
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, share_title + "\n\n" + share_text + "\n\n" + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        });
    }

    private void setupToolbar() {
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle("");
        }
    }

    public void displayData() {

        title_toolbar.setText(channel.category_name);
        channel_name.setText(channel.channel_name);
        channel_category.setText(channel.category_name);

        if (channel.channel_type != null && channel.channel_type.equals("YOUTUBE")) {
            if (!channel.channel_image.equals("")) {
                Picasso.get()
                        .load(sharedPref.getBaseUrl() + "/upload/" + channel.channel_image.replace(" ", "%20"))
                        .placeholder(R.drawable.ic_thumbnail)
                        .into(channel_image);
            } else {
                Picasso.get()
                        .load(Constant.YOUTUBE_IMG_FRONT + channel.video_id + Constant.YOUTUBE_IMG_BACK)
                        .placeholder(R.drawable.ic_thumbnail)
                        .into(channel_image);
            }
        } else {
            Picasso.get()
                    .load(sharedPref.getBaseUrl() + "/upload/" + channel.channel_image.replace(" ", "%20"))
                    .placeholder(R.drawable.ic_thumbnail)
                    .into(channel_image);
        }

        channel_image.setOnClickListener(view -> Tools.startPlayer(this, parent_view, channel));

        channel_description.setBackgroundColor(Color.TRANSPARENT);
        channel_description.setFocusableInTouchMode(false);
        channel_description.setFocusable(false);
        channel_description.getSettings().setDefaultTextEncodingName("UTF-8");

        WebSettings webSettings = channel_description.getSettings();
        Resources res = getResources();
        int fontSize = res.getInteger(R.integer.font_size);
        webSettings.setDefaultFontSize(fontSize);

        String mimeType = "text/html; charset=UTF-8";
        String encoding = "utf-8";
        String htmlText = channel.channel_description;

        String bg_paragraph;
        if (sharedPref.getIsDarkTheme()) {
            bg_paragraph = "<style type=\"text/css\">body{color: #eeeeee;}";
        } else {
            bg_paragraph = "<style type=\"text/css\">body{color: #000000;}";
        }

        String font_style_default = "<style type=\"text/css\">@font-face {font-family: MyFont;src: url(\"file:///android_asset/font/custom_font.ttf\")}body {font-family: MyFont; font-size: medium; text-align: left;}</style>";

        String text = "<html><head>"
                + font_style_default
                + "<style>img{max-width:100%;height:auto;} figure{max-width:100%;height:auto;} iframe{width:100%;}</style> "
                + bg_paragraph
                + "</style></head>"
                + "<body>"
                + htmlText
                + "</body></html>";

        String text_rtl = "<html dir='rtl'><head>"
                + font_style_default
                + "<style>img{max-width:100%;height:auto;} figure{max-width:100%;height:auto;} iframe{width:100%;}</style> "
                + bg_paragraph
                + "</style></head>"
                + "<body>"
                + htmlText
                + "</body></html>";

        if (Config.ENABLE_RTL_MODE) {
            channel_description.loadDataWithBaseURL(null, text_rtl, mimeType, encoding, null);
        } else {
            channel_description.loadDataWithBaseURL(null, text, mimeType, encoding, null);
        }

    }

    public void addToFavorite() {
        btn_favorite.setOnClickListener(view -> {
            String str;
            if (flag_read_later) {
                dao.deleteChannel(channel.channel_id);
                str = getString(R.string.favorite_removed);
            } else {
                dao.insertChannel(ChannelEntity.entity(channel));
                str = getString(R.string.favorite_added);
            }
            Snackbar.make(parent_view, str, Snackbar.LENGTH_SHORT).show();
            refreshReadLaterMenu();
        });
    }

    private void refreshReadLaterMenu() {
        flag_read_later = dao.getChannel(channel.channel_id) != null;
        if (flag_read_later) {
            btn_favorite.setImageResource(R.drawable.ic_favorite_white);
        } else {
            btn_favorite.setImageResource(R.drawable.ic_favorite_outline_white);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}
