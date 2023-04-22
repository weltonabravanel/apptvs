package com.app.braziltube.adapters;

import static com.app.braziltube.Config.LEGACY_GDPR;
import static com.app.braziltube.Config.LOAD_MORE;
import static com.app.braziltube.utils.Constant.CHANNEL_GRID_2_COLUMN;
import static com.app.braziltube.utils.Constant.CHANNEL_GRID_3_COLUMN;
import static com.app.braziltube.utils.Constant.CHANNEL_LIST_DEFAULT;
import static com.app.braziltube.utils.Constant.NATIVE_AD_CHANNEL_LIST;
import static com.solodroid.ads.sdk.util.Constant.ADMOB;
import static com.solodroid.ads.sdk.util.Constant.APPLOVIN;
import static com.solodroid.ads.sdk.util.Constant.APPLOVIN_MAX;
import static com.solodroid.ads.sdk.util.Constant.FAN;
import static com.solodroid.ads.sdk.util.Constant.GOOGLE_AD_MANAGER;
import static com.solodroid.ads.sdk.util.Constant.STARTAPP;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.app.braziltube.Config;
import com.app.braziltube.R;
import com.app.braziltube.databases.prefs.AdsPref;
import com.app.braziltube.databases.prefs.SharedPref;
import com.app.braziltube.models.Channel;
import com.app.braziltube.utils.Constant;
import com.solodroid.ads.sdk.format.NativeAdViewHolder;
import com.squareup.picasso.Picasso;

import java.util.List;

public class AdapterChannel extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int VIEW_PROG = 0;
    private final int VIEW_ITEM = 1;
    private final int VIEW_AD = 2;
    private final List<Channel> items;
    private boolean loading;
    private OnLoadMoreListener onLoadMoreListener;
    Context context;
    private OnItemClickListener mOnItemClickListener;
    private OnItemOverflowClickListener mOnItemOverflowClickListener;
    boolean scrolling = false;
    SharedPref sharedPref;
    AdsPref adsPref;

    public interface OnItemClickListener {
        void onItemClick(View view, Channel obj, int position);
    }

    public interface OnItemOverflowClickListener {
        void onItemOverflowClick(View view, Channel obj, int position);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }

    public void setOnItemOverflowClickListener(final OnItemOverflowClickListener mItemOverflowClickListener) {
        this.mOnItemOverflowClickListener = mItemOverflowClickListener;
    }

    public AdapterChannel(Context context, RecyclerView view, List<Channel> items) {
        this.items = items;
        this.context = context;
        this.sharedPref = new SharedPref(context);
        this.adsPref = new AdsPref(context);
        lastItemViewDetector(view);
        view.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    scrolling = true;
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    scrolling = false;
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    public static class OriginalViewHolder extends RecyclerView.ViewHolder {

        public TextView channel_name;
        public TextView channel_category;
        public ImageView channel_image;
        public LinearLayout lyt_parent;
        public ImageView overflow;

        private OriginalViewHolder(View v) {
            super(v);
            channel_name = v.findViewById(R.id.channel_name);
            channel_category = v.findViewById(R.id.channel_category);
            channel_image = v.findViewById(R.id.channel_image);
            lyt_parent = v.findViewById(R.id.lyt_parent);
            overflow = v.findViewById(R.id.overflow);
        }

    }

    public static class ProgressViewHolder extends RecyclerView.ViewHolder {
        public ProgressBar progressBar;

        ProgressViewHolder(View v) {
            super(v);
            progressBar = v.findViewById(R.id.loadMore);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh;
        if (viewType == VIEW_ITEM) {
            if (sharedPref.getChannelViewType() == CHANNEL_GRID_2_COLUMN || sharedPref.getChannelViewType() == CHANNEL_GRID_3_COLUMN) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.lsv_item_post_grid, parent, false);
                vh = new OriginalViewHolder(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.lsv_item_post, parent, false);
                vh = new OriginalViewHolder(v);
            }
        } else if (viewType == VIEW_AD) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_native_ad_medium, parent, false);
            vh = new NativeAdViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_loading, parent, false);
            vh = new ProgressViewHolder(v);
        }
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof OriginalViewHolder) {
            final Channel p = (Channel) items.get(position);
            final OriginalViewHolder vItem = (OriginalViewHolder) holder;

            vItem.channel_name.setText(p.channel_name);

            vItem.channel_category.setText(p.category_name);
            if (Config.ENABLE_CHANNEL_LIST_CATEGORY_NAME) {
                vItem.channel_category.setVisibility(View.VISIBLE);
            } else {
                vItem.channel_category.setVisibility(View.GONE);
            }

            if (p.channel_type != null && p.channel_type.equals("YOUTUBE")) {
                if (!p.channel_image.equals("")) {
                    Picasso.get()
                            .load(sharedPref.getBaseUrl() + "/upload/" + p.channel_image.replace(" ", "%20"))
                            .placeholder(R.drawable.ic_thumbnail)
                            .resizeDimen(R.dimen.list_image_width, R.dimen.list_image_height)
                            .centerCrop()
                            .into(vItem.channel_image);
                } else {
                    Picasso.get()
                            .load(Constant.YOUTUBE_IMG_FRONT + p.video_id + Constant.YOUTUBE_IMG_BACK)
                            .placeholder(R.drawable.ic_thumbnail)
                            .resizeDimen(R.dimen.list_image_width, R.dimen.list_image_height)
                            .centerCrop()
                            .into(vItem.channel_image);
                }
            } else {
                Picasso.get()
                        .load(sharedPref.getBaseUrl() + "/upload/" + p.channel_image.replace(" ", "%20"))
                        .placeholder(R.drawable.ic_thumbnail)
                        .resizeDimen(R.dimen.list_image_width, R.dimen.list_image_height)
                        .centerCrop()
                        .into(vItem.channel_image);
            }

            vItem.lyt_parent.setOnClickListener(view -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(view, p, position);
                }
            });

            vItem.overflow.setOnClickListener(view -> {
                if (mOnItemOverflowClickListener != null) {
                    mOnItemOverflowClickListener.onItemOverflowClick(view, p, position);
                }
            });

        } else if (holder instanceof NativeAdViewHolder) {

            final NativeAdViewHolder vItem = (NativeAdViewHolder) holder;

            int padding;
            if (sharedPref.getChannelViewType() == CHANNEL_LIST_DEFAULT) {
                padding = context.getResources().getDimensionPixelOffset(R.dimen.spacing_middle);
            } else {
                padding = context.getResources().getDimensionPixelOffset(R.dimen.grid_space_channel);
            }

            vItem.loadNativeAd(context,
                    adsPref.getAdStatus(),
                    NATIVE_AD_CHANNEL_LIST,
                    adsPref.getAdType(),
                    adsPref.getBackupAds(),
                    adsPref.getAdMobNativeId(),
                    adsPref.getAdManagerNativeId(),
                    adsPref.getFanNativeId(),
                    adsPref.getAppLovinNativeAdManualUnitId(),
                    sharedPref.getIsDarkTheme(),
                    LEGACY_GDPR,
                    "default"
            );

            vItem.setNativeAdPadding(padding, padding, padding, padding);

        } else {
            ((ProgressViewHolder) holder).progressBar.setIndeterminate(true);
        }

        if (getItemViewType(position) == VIEW_PROG || getItemViewType(position) == VIEW_AD) {
            StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setFullSpan(true);
        } else {
            StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setFullSpan(false);
        }

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        Channel channel = items.get(position);
        if (channel != null) {
            if (channel.channel_name == null || channel.channel_name.equals("")) {
                return VIEW_AD;
            }
            return VIEW_ITEM;
        } else {
            return VIEW_PROG;
        }
    }

    public void insertData(List<Channel> items) {
        setLoaded();
        int positionStart = getItemCount();
        int itemCount = items.size();
        this.items.addAll(items);
        notifyItemRangeInserted(positionStart, itemCount);
    }

    public void insertDataWithNativeAd(List<Channel> items) {
        setLoaded();
        int positionStart = getItemCount();
        for (Channel post : items) {
            Log.d("item", "TITLE: " + post.channel_name);
        }
        if (items.size() >= adsPref.getNativeAdIndex()) {
            if (sharedPref.getChannelViewType() == CHANNEL_GRID_2_COLUMN) {
                items.add(4, new Channel());
            } else if (sharedPref.getChannelViewType() == CHANNEL_GRID_3_COLUMN) {
                items.add(6, new Channel());
            } else {
                items.add(adsPref.getNativeAdIndex(), new Channel());
            }
        }
        int itemCount = items.size();
        this.items.addAll(items);
        notifyItemRangeInserted(positionStart, itemCount);
    }

    public void setLoaded() {
        loading = false;
        for (int i = 0; i < getItemCount(); i++) {
            if (items.get(i) == null) {
                items.remove(i);
                notifyItemRemoved(i);
            }
        }
    }

    public void setLoading() {
        if (getItemCount() != 0) {
            this.items.add(null);
            notifyItemInserted(getItemCount() - 1);
            loading = true;
        }
    }

    public void resetListData() {
        this.items.clear();
        notifyDataSetChanged();
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    private void lastItemViewDetector(RecyclerView recyclerView) {

        if (recyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            final StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    int lastPos = getLastVisibleItem(layoutManager.findLastVisibleItemPositions(null));
                    if (!loading && lastPos == getItemCount() - 1 && onLoadMoreListener != null) {
                        if (NATIVE_AD_CHANNEL_LIST != 0) {
                            switch (adsPref.getAdType()) {
                                case ADMOB:
                                case GOOGLE_AD_MANAGER:
                                case FAN:
                                case STARTAPP:
                                case APPLOVIN:
                                case APPLOVIN_MAX: {
                                    int current_page = getItemCount() / (LOAD_MORE + 1); //posts per page plus 1 Ad
                                    onLoadMoreListener.onLoadMore(current_page);
                                    break;
                                }
                                default: {
                                    int current_page = getItemCount() / (LOAD_MORE);
                                    onLoadMoreListener.onLoadMore(current_page);
                                    break;
                                }
                            }
                        } else {
                            int current_page = getItemCount() / (LOAD_MORE);
                            onLoadMoreListener.onLoadMore(current_page);
                        }
                    }
                }
            });
        }
    }

    public interface OnLoadMoreListener {
        void onLoadMore(int current_page);
    }

    private int getLastVisibleItem(int[] into) {
        int last_idx = into[0];
        for (int i : into) {
            if (last_idx < i) last_idx = i;
        }
        return last_idx;
    }

}