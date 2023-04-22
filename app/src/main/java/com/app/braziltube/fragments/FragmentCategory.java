package com.app.braziltube.fragments;

import static com.app.braziltube.Config.REST_API_KEY;
import static com.app.braziltube.utils.Constant.CATEGORY_GRID_2_COLUMN;
import static com.app.braziltube.utils.Constant.CATEGORY_GRID_3_COLUMN;
import static com.app.braziltube.utils.Constant.CATEGORY_LIST_DEFAULT;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.braziltube.R;
import com.app.braziltube.activities.ActivityCategoryDetail;
import com.app.braziltube.activities.MainActivity;
import com.app.braziltube.adapters.AdapterCategory;
import com.app.braziltube.databases.prefs.SharedPref;
import com.app.braziltube.models.Category;
import com.app.braziltube.rests.ApiInterface;
import com.app.braziltube.rests.RestAdapter;
import com.app.braziltube.utils.Constant;
import com.app.braziltube.utils.ItemOffsetDecoration;
import com.app.braziltube.utils.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class FragmentCategory extends Fragment {

    private View root_view;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AdapterCategory adapterCategory;
    public static final String EXTRA_OBJC = "key.EXTRA_OBJC";
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private ShimmerFrameLayout lyt_shimmer;
    SharedPref sharedPref;
    private Activity activity;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (Activity) context;
    }
    
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root_view = inflater.inflate(R.layout.fragment_category, container, false);
        sharedPref = new SharedPref(activity);

        lyt_shimmer = root_view.findViewById(R.id.shimmer_view_container);
        initShimmerLayout();

        swipeRefreshLayout = root_view.findViewById(R.id.swipe_refresh_layout_category);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

        recyclerView = root_view.findViewById(R.id.recyclerViewCategory);

        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(activity, R.dimen.grid_space_category);
        if (sharedPref.getCategoryViewType() == CATEGORY_LIST_DEFAULT) {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, LinearLayoutManager.VERTICAL));
        } else if (sharedPref.getCategoryViewType() == CATEGORY_GRID_2_COLUMN) {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL));
            recyclerView.addItemDecoration(itemDecoration);
        } else if (sharedPref.getCategoryViewType() == CATEGORY_GRID_3_COLUMN) {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(3, LinearLayoutManager.VERTICAL));
            recyclerView.addItemDecoration(itemDecoration);
        }

        //set data and list adapter
        adapterCategory = new AdapterCategory(activity, new ArrayList<>());
        recyclerView.setAdapter(adapterCategory);

        // on item list clicked
        adapterCategory.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(activity, ActivityCategoryDetail.class);
            intent.putExtra(EXTRA_OBJC, obj);
            startActivity(intent);
            ((MainActivity) activity).showInterstitialAd();
            ((MainActivity) activity).destroyBannerAd();
        });

        // on swipe list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (mCompositeDisposable != null && !mCompositeDisposable.isDisposed()) {
                mCompositeDisposable.dispose();
                mCompositeDisposable = new CompositeDisposable();
            }
            adapterCategory.resetListData();
            requestAction();
        });

        requestAction();

        return root_view;
    }

    private void displayApiResult(final List<Category> categories) {
        adapterCategory.setListData(categories);
        swipeProgress(false);
        if (categories.size() == 0) {
            showNoItemView(true);
        }
    }

    private void requestCategoriesApi() {
        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());
        mCompositeDisposable.add(apiInterface.getAllCategories(REST_API_KEY)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((resp, throwable) -> {
                    if (resp != null && resp.status.equals("ok")) {
                        displayApiResult(resp.categories);
                    } else {
                        onFailRequest();
                    }
                }));
    }

    private void onFailRequest() {
        swipeProgress(false);
        if (Tools.isConnect(activity)) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.no_internet_text));
        }
    }

    private void requestAction() {
        showFailedView(false, "");
        swipeProgress(true);
        showNoItemView(false);
        new Handler().postDelayed(this::requestCategoriesApi, Constant.DELAY_TIME);
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

    private void showFailedView(boolean flag, String message) {
        View lyt_failed = root_view.findViewById(R.id.lyt_failed_category);
        ((TextView) root_view.findViewById(R.id.failed_message)).setText(message);
        if (flag) {
            recyclerView.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        root_view.findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction());
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = root_view.findViewById(R.id.lyt_no_item_category);
        ((TextView) root_view.findViewById(R.id.no_item_message)).setText(R.string.no_category_found);
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
        View lyt_shimmer_category_list = root_view.findViewById(R.id.lyt_shimmer_category_list);
        View lyt_shimmer_category_grid2 = root_view.findViewById(R.id.lyt_shimmer_category_grid2);
        View lyt_shimmer_category_grid3 = root_view.findViewById(R.id.lyt_shimmer_category_grid3);
        if (sharedPref.getCategoryViewType() == CATEGORY_LIST_DEFAULT) {
            lyt_shimmer_category_list.setVisibility(View.VISIBLE);
            lyt_shimmer_category_grid2.setVisibility(View.GONE);
            lyt_shimmer_category_grid3.setVisibility(View.GONE);
        } else if (sharedPref.getCategoryViewType() == CATEGORY_GRID_2_COLUMN) {
            lyt_shimmer_category_list.setVisibility(View.GONE);
            lyt_shimmer_category_grid2.setVisibility(View.VISIBLE);
            lyt_shimmer_category_grid3.setVisibility(View.GONE);
        } else if (sharedPref.getCategoryViewType() == CATEGORY_GRID_3_COLUMN) {
            lyt_shimmer_category_list.setVisibility(View.GONE);
            lyt_shimmer_category_grid2.setVisibility(View.GONE);
            lyt_shimmer_category_grid3.setVisibility(View.VISIBLE);
        }
    }

}
