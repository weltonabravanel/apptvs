package com.app.braziltube.fragments;

import static com.app.braziltube.Config.REST_API_KEY;
import static com.app.braziltube.utils.Constant.CHANNEL_GRID_2_COLUMN;
import static com.app.braziltube.utils.Constant.CHANNEL_GRID_3_COLUMN;
import static com.app.braziltube.utils.Constant.CHANNEL_LIST_DEFAULT;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.braziltube.Config;
import com.app.braziltube.R;
import com.app.braziltube.activities.ActivityChannelDetail;
import com.app.braziltube.activities.MainActivity;
import com.app.braziltube.adapters.AdapterChannel;
import com.app.braziltube.databases.prefs.SharedPref;
import com.app.braziltube.models.Channel;
import com.app.braziltube.rests.ApiInterface;
import com.app.braziltube.rests.RestAdapter;
import com.app.braziltube.utils.Constant;
import com.app.braziltube.utils.PopupMenu;
import com.app.braziltube.utils.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class FragmentRecent extends Fragment {

    View root_view;
    LinearLayout parent_view;
    private RecyclerView recyclerView;
    private AdapterChannel adapterChannel;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int post_total = 0;
    private int failed_page = 0;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private ShimmerFrameLayout lyt_shimmer;
    private SharedPref sharedPref;
    private Activity activity;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (Activity) context;
    }
    
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root_view = inflater.inflate(R.layout.fragment_recent, container, false);
        sharedPref = new SharedPref(activity);
        parent_view = root_view.findViewById(R.id.parent_view);
        lyt_shimmer = root_view.findViewById(R.id.shimmer_view_container);
        initShimmerLayout();

        swipeRefreshLayout = root_view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        recyclerView = root_view.findViewById(R.id.recyclerView);

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

        //set data and list adapter
        adapterChannel = new AdapterChannel(activity, recyclerView, new ArrayList<>());
        recyclerView.setAdapter(adapterChannel);

        // on item list clicked
        adapterChannel.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(activity, ActivityChannelDetail.class);
            intent.putExtra(Constant.EXTRA_OBJC, obj);
            startActivity(intent);
            ((MainActivity) activity).showInterstitialAd();
            ((MainActivity) activity).destroyBannerAd();
        });

        // detect when scroll reach bottom
        adapterChannel.setOnLoadMoreListener(this::setLoadMore);

        // on swipe list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (mCompositeDisposable != null && !mCompositeDisposable.isDisposed()) {
                mCompositeDisposable.dispose();
                mCompositeDisposable = new CompositeDisposable();
            }
            adapterChannel.resetListData();
            requestAction(1);
        });

        requestAction(1);

        return root_view;
    }

    public void setLoadMore(int current_page) {
        Log.d("page", "currentPage: " + current_page);
        // Assuming final total items equal to real post items plus the ad
        int totalItemBeforeAds = (adapterChannel.getItemCount() - current_page);
        if (post_total > totalItemBeforeAds && current_page != 0) {
            int next_page = current_page + 1;
            requestAction(next_page);
        } else {
            adapterChannel.setLoaded();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    private void displayApiResult(final List<Channel> channels) {
        adapterChannel.insertDataWithNativeAd(channels);
        swipeProgress(false);
        if (channels.size() == 0) {
            showNoItemView(true);
        }
    }

    private void requestListPostApi(final int page_no) {
        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());
        mCompositeDisposable.add(apiInterface.getRecentChannel(page_no, Config.LOAD_MORE, REST_API_KEY)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((resp, throwable) -> {
                    if (resp != null && resp.status.equals("ok")) {
                        post_total = resp.count_total;
                        displayApiResult(resp.posts);
                        addFavorite();
                    } else {
                        onFailRequest(page_no);
                    }
                }));
    }

    public void addFavorite() {
        adapterChannel.setOnItemOverflowClickListener((v, obj, position) -> {
            PopupMenu popupMenu = new PopupMenu(activity);
            popupMenu.onClickItemOverflow(v, obj, parent_view);
        });
    }

    private void onFailRequest(int page_no) {
        failed_page = page_no;
        adapterChannel.setLoaded();
        swipeProgress(false);
        if (Tools.isConnect(activity)) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.no_internet_text));
        }
    }

    private void requestAction(final int page_no) {
        showFailedView(false, "");
        showNoItemView(false);
        if (page_no == 1) {
            swipeProgress(true);
        } else {
            adapterChannel.setLoading();
        }
        new Handler().postDelayed(() -> requestListPostApi(page_no), Constant.DELAY_TIME);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        swipeProgress(false);
        if (mCompositeDisposable != null && !mCompositeDisposable.isDisposed()) {
            mCompositeDisposable.clear();
        }
        lyt_shimmer.stopShimmer();
    }

    private void showFailedView(boolean show, String message) {
        View lyt_failed = (View) root_view.findViewById(R.id.lyt_failed_home);
        ((TextView) root_view.findViewById(R.id.failed_message)).setText(message);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        root_view.findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction(failed_page));
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = root_view.findViewById(R.id.lyt_no_item_home);
        ((TextView) root_view.findViewById(R.id.no_item_message)).setText(R.string.no_post_found);
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
            swipeRefreshLayout.setRefreshing(show);
            lyt_shimmer.setVisibility(View.GONE);
            lyt_shimmer.stopShimmer();
            return;
        }
        swipeRefreshLayout.post(() -> {
            swipeRefreshLayout.setRefreshing(show);
            lyt_shimmer.setVisibility(View.VISIBLE);
            lyt_shimmer.startShimmer();
        });
    }

    private void initShimmerLayout() {
        View lyt_shimmer_channel_list = root_view.findViewById(R.id.lyt_shimmer_channel_list);
        View lyt_shimmer_channel_grid2 = root_view.findViewById(R.id.lyt_shimmer_channel_grid2);
        View lyt_shimmer_channel_grid3 = root_view.findViewById(R.id.lyt_shimmer_channel_grid3);
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

}
