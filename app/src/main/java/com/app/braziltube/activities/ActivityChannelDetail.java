package com.app.braziltube.activities;

import static com.app.braziltube.utils.Constant.BANNER_CHANNEL_DETAIL;
import static com.app.braziltube.utils.Constant.INTERSTITIAL_AD_CHANNEL_DETAIL;
import static com.app.braziltube.utils.Constant.NATIVE_AD_CHANNEL_DETAIL;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.braziltube.Config;
import com.app.braziltube.R;
import com.app.braziltube.adapters.AdapterSuggested;
import com.app.braziltube.callbacks.CallbackChannelDetail;
import com.app.braziltube.databases.dao.AppDatabase;
import com.app.braziltube.databases.dao.ChannelEntity;
import com.app.braziltube.databases.dao.DAO;
import com.app.braziltube.databases.prefs.AdsPref;
import com.app.braziltube.databases.prefs.SharedPref;
import com.app.braziltube.models.Channel;
import com.app.braziltube.rests.ApiInterface;
import com.app.braziltube.rests.RestAdapter;
import com.app.braziltube.utils.AdsManager;
import com.app.braziltube.utils.AppBarLayoutBehavior;
import com.app.braziltube.utils.Constant;
import com.app.braziltube.utils.PopupMenu;
import com.app.braziltube.utils.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Picasso;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ActivityChannelDetail extends AppCompatActivity {

    private LinearLayout lyt_main_content;
    private Channel channel;
    ImageView channel_image;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    TextView channel_name, channel_category, title_toolbar;
    WebView channel_description;
    private DAO dao;
    boolean flag_read_later;
    CoordinatorLayout parent_view;
    private ShimmerFrameLayout lyt_shimmer;
    RelativeLayout lyt_suggested;
    private SwipeRefreshLayout swipe_refresh;
    SharedPref sharedPref;
    AdsPref adsPref;
    ImageButton btn_favorite, btn_share;
    AdsManager adsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_detail);
        Tools.setNavigation(this);

        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);

        adsManager = new AdsManager(this);
        adsManager.loadBannerAd(BANNER_CHANNEL_DETAIL);
        adsManager.loadInterstitialAd(INTERSTITIAL_AD_CHANNEL_DETAIL, 1);
        adsManager.loadNativeAd(NATIVE_AD_CHANNEL_DETAIL);

        dao = AppDatabase.getDatabase(this).get();

        AppBarLayout appBarLayout = findViewById(R.id.appbar);
        ((CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams()).setBehavior(new AppBarLayoutBehavior());

        swipe_refresh = findViewById(R.id.swipe_refresh_layout);
        swipe_refresh.setColorSchemeResources(R.color.colorPrimary);
        swipe_refresh.setRefreshing(false);

        lyt_main_content = findViewById(R.id.lyt_main_content);
        lyt_shimmer = findViewById(R.id.shimmer_view_container);
        parent_view = findViewById(R.id.parent_view);

        title_toolbar = findViewById(R.id.title_toolbar);
        btn_favorite = findViewById(R.id.btn_favorite);
        btn_share = findViewById(R.id.btn_share);
        channel_image = findViewById(R.id.channel_image);
        channel_name = findViewById(R.id.channel_name);
        channel_category = findViewById(R.id.channel_category);
        channel_description = findViewById(R.id.channel_description);

        lyt_suggested = findViewById(R.id.lyt_suggested);

        channel = (Channel) getIntent().getSerializableExtra(Constant.EXTRA_OBJC);

        requestAction();

        swipe_refresh.setOnRefreshListener(() -> {
            if (mCompositeDisposable != null && !mCompositeDisposable.isDisposed()) {
                mCompositeDisposable.dispose();
                mCompositeDisposable = new CompositeDisposable();
            }
            lyt_shimmer.setVisibility(View.VISIBLE);
            lyt_shimmer.startShimmer();
            lyt_main_content.setVisibility(View.GONE);
            requestAction();
        });

        initToolbar();
        refreshReadLaterMenu();

    }

    private void requestAction() {
        showFailedView(false, "");
        swipeProgress(true);
        new Handler().postDelayed(this::requestPostData, 200);
    }

    private void requestPostData() {
        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());
        mCompositeDisposable.add(apiInterface.getChannelDetail(channel.channel_id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((resp, throwable) -> {
                    if (resp != null && resp.status.equals("ok")) {
                        displayAllData(resp);
                        swipeProgress(false);
                        lyt_main_content.setVisibility(View.VISIBLE);
                    } else {
                        onFailRequest();
                    }
                }));
    }

    private void onFailRequest() {
        swipeProgress(false);
        lyt_main_content.setVisibility(View.GONE);
        if (Tools.isConnect(ActivityChannelDetail.this)) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.failed_text));
        }
    }

    private void showFailedView(boolean show, String message) {
        View lyt_failed = findViewById(R.id.lyt_failed_home);
        ((TextView) findViewById(R.id.failed_message)).setText(message);
        if (show) {
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            lyt_failed.setVisibility(View.GONE);
        }
        findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction());
    }

    private void swipeProgress(final boolean show) {
        if (!show) {
            swipe_refresh.setRefreshing(show);
            lyt_shimmer.setVisibility(View.GONE);
            lyt_shimmer.stopShimmer();
            lyt_main_content.setVisibility(View.VISIBLE);
            return;
        }
        lyt_main_content.setVisibility(View.GONE);
    }

    private void displayAllData(CallbackChannelDetail resp) {
        displayData(resp.post);
        displaySuggested(resp.suggested);
    }

    public void displayData(final Channel channel) {

        channel_name.setText(channel.channel_name);

        channel_category.setText(channel.category_name);
        if (Config.ENABLE_CHANNEL_LIST_CATEGORY_NAME) {
            channel_category.setVisibility(View.VISIBLE);
        } else {
            channel_category.setVisibility(View.GONE);
        }

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

        channel_image.setOnClickListener(view -> {
            Tools.startPlayer(this, parent_view, channel);
            showInterstitialAd();
        });

        Tools.displayContent(this, channel_description, channel.channel_description);

        btn_share.setOnClickListener(view -> Tools.share(this, channel.channel_name));

        addToFavorite();

        new Handler().postDelayed(() -> lyt_suggested.setVisibility(View.VISIBLE), 1000);

    }

    private void showInterstitialAd() {
        if (adsPref.getInterstitialAdCounter() < adsPref.getInterstitialAdInterval()) {
            adsPref.updateInterstitialAdCounter(adsPref.getInterstitialAdCounter() + 1);
        } else {
            adsPref.updateInterstitialAdCounter(1);
            adsManager.showInterstitialAd();
        }
    }

    private void displaySuggested(List<Channel> list) {

        RecyclerView recyclerView = findViewById(R.id.recycler_view_suggested);
        recyclerView.setLayoutManager(new LinearLayoutManager(ActivityChannelDetail.this));
        AdapterSuggested adapterSuggested = new AdapterSuggested(ActivityChannelDetail.this, recyclerView, list);
        recyclerView.setAdapter(adapterSuggested);
        recyclerView.setNestedScrollingEnabled(false);
        adapterSuggested.setOnItemClickListener((view, obj, position) -> {
            Intent intent = new Intent(getApplicationContext(), ActivityChannelDetail.class);
            intent.putExtra(Constant.EXTRA_OBJC, obj);
            startActivity(intent);
            showInterstitialAd();
            adsManager.destroyBannerAd();
        });

        adapterSuggested.setOnItemOverflowClickListener((v, obj, position) -> {
            PopupMenu popupMenu = new PopupMenu(this);
            popupMenu.onClickItemOverflow(v, obj, parent_view);
        });

        TextView txt_suggested = findViewById(R.id.txt_suggested);
        if (list.size() > 0) {
            txt_suggested.setText(getResources().getString(R.string.txt_suggested));
        } else {
            txt_suggested.setText("");
        }

    }

    private void initToolbar() {
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (sharedPref.getIsDarkTheme()) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
        } else {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle("");
        }

        title_toolbar.setText(channel.category_name);

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
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
        } else {
            return super.onOptionsItemSelected(menuItem);
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        adsManager.resumeBannerAd(BANNER_CHANNEL_DETAIL);
    }

    public void onDestroy() {
        super.onDestroy();
        lyt_shimmer.stopShimmer();
        if (mCompositeDisposable != null && !mCompositeDisposable.isDisposed()) {
            mCompositeDisposable.clear();
        }
        adsManager.destroyBannerAd();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        adsManager.destroyBannerAd();
    }

}
