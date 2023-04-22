package com.app.braziltube.fragments;

import static com.app.braziltube.utils.Constant.CHANNEL_GRID_2_COLUMN;
import static com.app.braziltube.utils.Constant.CHANNEL_GRID_3_COLUMN;
import static com.app.braziltube.utils.Constant.CHANNEL_LIST_DEFAULT;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.braziltube.R;
import com.app.braziltube.activities.ActivityChannelDetail;
import com.app.braziltube.activities.ActivityChannelDetailOffline;
import com.app.braziltube.activities.MainActivity;
import com.app.braziltube.adapters.AdapterFavorite;
import com.app.braziltube.databases.dao.AppDatabase;
import com.app.braziltube.databases.dao.ChannelEntity;
import com.app.braziltube.databases.dao.DAO;
import com.app.braziltube.databases.prefs.SharedPref;
import com.app.braziltube.models.Channel;
import com.app.braziltube.utils.Constant;
import com.app.braziltube.utils.Tools;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class FragmentFavorite extends Fragment {

    List<Channel> channels = new ArrayList<>();
    View root_view;
    LinearLayout parent_view;
    AdapterFavorite adapterFavorite;
    boolean flag_read_later;
    private DAO dao;
    RecyclerView recyclerView;
    View lyt_no_favorite;
    private CharSequence charSequence = null;
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
        root_view = inflater.inflate(R.layout.fragment_favorite, container, false);

        sharedPref = new SharedPref(activity);
        dao = AppDatabase.getDatabase(getContext()).get();

        parent_view = root_view.findViewById(R.id.parent_view);
        lyt_no_favorite = root_view.findViewById(R.id.lyt_no_favorite);
        recyclerView = root_view.findViewById(R.id.recyclerView);

        if (sharedPref.getChannelViewType() == CHANNEL_LIST_DEFAULT) {
            recyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
        } else if (sharedPref.getChannelViewType() == CHANNEL_GRID_2_COLUMN) {
            recyclerView.setLayoutManager(new GridLayoutManager(activity, 2));
        } else if (sharedPref.getChannelViewType() == CHANNEL_GRID_3_COLUMN) {
            recyclerView.setLayoutManager(new GridLayoutManager(activity, 3));
        }

        int padding = getResources().getDimensionPixelOffset(R.dimen.recycler_view_padding);
        if (sharedPref.getChannelViewType() == CHANNEL_LIST_DEFAULT) {
            recyclerView.setPadding(0, padding, 0, padding);
        } else {
            recyclerView.setPadding(padding, padding, padding, padding);
        }

        adapterFavorite = new AdapterFavorite(activity, recyclerView, channels);
        recyclerView.setAdapter(adapterFavorite);
        onChannelClickListener();
        addFavorite();

        if (channels.size() == 0) {
            lyt_no_favorite.setVisibility(View.VISIBLE);
        } else {
            lyt_no_favorite.setVisibility(View.INVISIBLE);
        }

        return root_view;
    }

    @Override
    public void onResume() {
        super.onResume();
        displayData(dao.getAllChannel());
    }

    private void displayData(final List<ChannelEntity> radios) {
        ArrayList<Channel> items = new ArrayList<>();
        for (ChannelEntity radio : radios) items.add(radio.original());
        showNoItemView(false);
        adapterFavorite.resetListData();
        adapterFavorite.insertData(items);
        if (radios.size() == 0) {
            showNoItemView(true);
        }
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = root_view.findViewById(R.id.lyt_no_favorite);
        ((TextView) root_view.findViewById(R.id.no_item_message)).setText(R.string.no_favorite_found);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_no_item.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_no_item.setVisibility(View.GONE);
        }
    }

    public void onChannelClickListener() {
        adapterFavorite.setOnItemClickListener((v, obj, position) -> {
            if (Tools.isConnect(activity)) {
                Intent intent = new Intent(activity, ActivityChannelDetail.class);
                intent.putExtra(Constant.EXTRA_OBJC, obj);
                startActivity(intent);
                ((MainActivity) activity).showInterstitialAd();
                ((MainActivity) activity).destroyBannerAd();
            } else {
                Intent intent = new Intent(activity, ActivityChannelDetailOffline.class);
                intent.putExtra(Constant.EXTRA_OBJC, obj);
                startActivity(intent);
            }
        });
    }

    public void addFavorite() {
        adapterFavorite.setOnItemOverflowClickListener((v, obj, position) -> {
            PopupMenu popup = new PopupMenu(activity, v);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.menu_popup, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.menu_context_favorite:
                        if (charSequence.equals(getString(R.string.option_set_favorite))) {
                            dao.insertChannel(ChannelEntity.entity(obj));
                            Snackbar.make(parent_view, getString(R.string.favorite_added), Snackbar.LENGTH_SHORT).show();
                        } else if (charSequence.equals(getString(R.string.option_unset_favorite))) {
                            dao.deleteChannel(obj.channel_id);
                            Snackbar.make(parent_view, getString(R.string.favorite_removed), Snackbar.LENGTH_SHORT).show();
                            displayData(dao.getAllChannel());
                        }
                        return true;

                    case R.id.menu_context_quick_play:
                        Tools.startPlayer(activity, parent_view, obj);
                        return true;

                    default:
                }
                return false;
            });
            popup.show();

            flag_read_later = dao.getChannel(obj.channel_id) != null;
            if (flag_read_later) {
                popup.getMenu().findItem(R.id.menu_context_favorite).setTitle(R.string.option_unset_favorite);
                charSequence = popup.getMenu().findItem(R.id.menu_context_favorite).getTitle();
            } else {
                popup.getMenu().findItem(R.id.menu_context_favorite).setTitle(R.string.option_set_favorite);
                charSequence = popup.getMenu().findItem(R.id.menu_context_favorite).getTitle();
            }

        });
    }

}
