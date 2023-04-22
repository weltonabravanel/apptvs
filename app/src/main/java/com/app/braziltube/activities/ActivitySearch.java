package com.app.braziltube.activities;

import static com.app.braziltube.Config.ENABLE_RTL_MODE;
import static com.app.braziltube.Config.REST_API_KEY;
import static com.app.braziltube.utils.Constant.BANNER_CHANNEL_DETAIL;
import static com.app.braziltube.utils.Constant.BANNER_SEARCH;
import static com.app.braziltube.utils.Constant.CHANNEL_GRID_2_COLUMN;
import static com.app.braziltube.utils.Constant.CHANNEL_GRID_3_COLUMN;
import static com.app.braziltube.utils.Constant.CHANNEL_LIST_DEFAULT;
import static com.app.braziltube.utils.Constant.INTERSTITIAL_AD_CHANNEL_LIST;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.app.braziltube.R;
import com.app.braziltube.adapters.AdapterChannel;
import com.app.braziltube.databases.prefs.AdsPref;
import com.app.braziltube.databases.prefs.SharedPref;
import com.app.braziltube.rests.ApiInterface;
import com.app.braziltube.rests.RestAdapter;
import com.app.braziltube.utils.AdsManager;
import com.app.braziltube.utils.Constant;
import com.app.braziltube.utils.PopupMenu;
import com.app.braziltube.utils.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ActivitySearch extends AppCompatActivity {

    private EditText et_search;
    private RecyclerView recyclerView;
    private AdapterChannel adapterChannel;
    private ImageButton bt_clear;
    RelativeLayout parent_view;
    Snackbar snackbar;
    CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private ShimmerFrameLayout lyt_shimmer;
    SharedPref sharedPref;
    AdsPref adsPref;
    AdsManager adsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_search);
        Tools.setNavigation(this);

        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);

        adsManager = new AdsManager(this);
        adsManager.loadBannerAd(BANNER_SEARCH);
        adsManager.loadInterstitialAd(INTERSTITIAL_AD_CHANNEL_LIST, adsPref.getInterstitialAdInterval());

        parent_view = findViewById(R.id.parent_view);
        et_search = findViewById(R.id.et_search);
        bt_clear = findViewById(R.id.bt_clear);
        bt_clear.setVisibility(View.GONE);
        lyt_shimmer = findViewById(R.id.shimmer_view_container);
        initShimmerLayout();
        swipeProgress(false);
        recyclerView = findViewById(R.id.recyclerView);

        if (sharedPref.getChannelViewType() == CHANNEL_LIST_DEFAULT) {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));
        } else if (sharedPref.getChannelViewType() == CHANNEL_GRID_2_COLUMN) {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        } else if (sharedPref.getChannelViewType() == CHANNEL_GRID_3_COLUMN) {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL));
        }

        int padding = getResources().getDimensionPixelOffset(R.dimen.recycler_view_padding);
        if (sharedPref.getChannelViewType() == CHANNEL_LIST_DEFAULT) {
            recyclerView.setPadding(0, padding, 0, padding);
        } else {
            recyclerView.setPadding(padding, padding, padding, padding);
        }

        et_search.addTextChangedListener(textWatcher);

        //set data and list adapter
        adapterChannel = new AdapterChannel(this, recyclerView, new ArrayList<>());
        recyclerView.setAdapter(adapterChannel);

        bt_clear.setOnClickListener(view -> et_search.setText(""));

        et_search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                searchAction();
                return true;
            }
            return false;
        });

        adapterChannel.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getApplicationContext(), ActivityChannelDetail.class);
            intent.putExtra(Constant.EXTRA_OBJC, obj);
            startActivity(intent);
            adsManager.showInterstitialAd();
            adsManager.destroyBannerAd();
        });

        setupToolbar();

    }

    public void setupToolbar() {
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle("");
        }
    }

    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence c, int i, int i1, int i2) {
            if (c.toString().trim().length() == 0) {
                bt_clear.setVisibility(View.GONE);
            } else {
                bt_clear.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence c, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };

    private void requestSearchApi(final String query) {
        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());
        mCompositeDisposable.add(apiInterface.getSearchChannel(query, Constant.MAX_SEARCH_RESULT, REST_API_KEY)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((resp, throwable) -> {
                    if (resp != null && resp.status.equals("ok")) {
                        adapterChannel.insertData(resp.posts);
                        addFavorite();
                        if (resp.posts.size() == 0) showNotFoundView(true);
                    } else {
                        onFailRequest();
                    }
                    swipeProgress(false);
                }));
    }

    private void requestSearchApiRTL(final String query) {
        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());
        mCompositeDisposable.add(apiInterface.getSearchChannelRTL(query, Constant.MAX_SEARCH_RESULT, REST_API_KEY)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((resp, throwable) -> {
                    if (resp != null && resp.status.equals("ok")) {
                        adapterChannel.insertData(resp.posts);
                        addFavorite();
                        if (resp.posts.size() == 0) showNotFoundView(true);
                    } else {
                        onFailRequest();
                    }
                    swipeProgress(false);
                }));
    }

    private void onFailRequest() {
        if (Tools.isConnect(this)) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.no_internet_text));
        }
    }

    private void searchAction() {
        showFailedView(false, "");
        showNotFoundView(false);
        final String query = et_search.getText().toString().trim();
        if (!query.equals("")) {
            adapterChannel.resetListData();
            swipeProgress(true);
            new Handler().postDelayed(() -> {
                if (ENABLE_RTL_MODE) {
                    requestSearchApiRTL(query);
                } else {
                    requestSearchApi(query);
                }
            }, Constant.DELAY_TIME);
        } else {
            snackbar = Snackbar.make(parent_view, getResources().getString(R.string.msg_search_input), Snackbar.LENGTH_SHORT);
            snackbar.show();
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

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showFailedView(boolean show, String message) {
        View lyt_failed = findViewById(R.id.lyt_failed);
        ((TextView) findViewById(R.id.failed_message)).setText(message);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        findViewById(R.id.failed_retry).setOnClickListener(view -> searchAction());
    }

    private void showNotFoundView(boolean show) {
        View lyt_no_item = findViewById(R.id.lyt_no_item);
        ((TextView) findViewById(R.id.no_item_message)).setText(R.string.no_search_found);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_no_item.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_no_item.setVisibility(View.GONE);
        }
    }

    private void swipeProgress(final boolean show) {
        if (!show) {
            lyt_shimmer.setVisibility(View.GONE);
            lyt_shimmer.stopShimmer();
        } else {
            lyt_shimmer.setVisibility(View.VISIBLE);
            lyt_shimmer.startShimmer();
        }
    }

    private void initShimmerLayout() {
        View lyt_shimmer_channel_list = findViewById(R.id.lyt_shimmer_channel_list);
        View lyt_shimmer_channel_grid2 = findViewById(R.id.lyt_shimmer_channel_grid2);
        View lyt_shimmer_channel_grid3 = findViewById(R.id.lyt_shimmer_channel_grid3);
        if (sharedPref.getChannelViewType() == CHANNEL_LIST_DEFAULT) {
            lyt_shimmer_channel_list.setVisibility(View.VISIBLE);
            lyt_shimmer_channel_grid2.setVisibility(View.GONE);
            lyt_shimmer_channel_grid3.setVisibility(View.GONE);
        } else if (sharedPref.getChannelViewType() == CHANNEL_GRID_2_COLUMN) {
            lyt_shimmer_channel_list.setVisibility(View.GONE);
            lyt_shimmer_channel_grid2.setVisibility(View.VISIBLE);
            lyt_shimmer_channel_grid3.setVisibility(View.GONE);
        } else if (sharedPref.getChannelViewType() == CHANNEL_GRID_3_COLUMN) {
            lyt_shimmer_channel_list.setVisibility(View.GONE);
            lyt_shimmer_channel_grid2.setVisibility(View.GONE);
            lyt_shimmer_channel_grid3.setVisibility(View.VISIBLE);
        }
    }

    public void addFavorite() {
        adapterChannel.setOnItemOverflowClickListener((v, obj, position) -> {
            PopupMenu popupMenu = new PopupMenu(this);
            popupMenu.onClickItemOverflow(v, obj, parent_view);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        adsManager.resumeBannerAd(BANNER_CHANNEL_DETAIL);
    }

    public void onDestroy() {
        super.onDestroy();
        adsManager.destroyBannerAd();
    }

    @Override
    public void onBackPressed() {
        if (et_search.length() > 0) {
            et_search.setText("");
        } else {
            super.onBackPressed();
            adsManager.destroyBannerAd();
        }
    }

}
