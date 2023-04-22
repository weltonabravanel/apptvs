package com.app.braziltube.utils;

import android.app.Activity;
import android.view.MenuInflater;
import android.view.View;

import com.app.braziltube.R;
import com.app.braziltube.activities.MainActivity;
import com.app.braziltube.databases.dao.AppDatabase;
import com.app.braziltube.databases.dao.ChannelEntity;
import com.app.braziltube.databases.dao.DAO;
import com.app.braziltube.models.Channel;
import com.google.android.material.snackbar.Snackbar;

public class PopupMenu {

    CharSequence charSequence = null;
    boolean flag_read_later;
    Activity activity;
    DAO dao;

    public PopupMenu(Activity activity) {
        this.activity = activity;
        this.dao = AppDatabase.getDatabase(activity).get();
    }

    public void onClickItemOverflow(View view, Channel channel, View viewSnackBar) {

        android.widget.PopupMenu popup = new android.widget.PopupMenu(activity, view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_popup, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_context_favorite:
                    if (charSequence.equals(activity.getString(R.string.option_set_favorite))) {
                        dao.insertChannel(ChannelEntity.entity(channel));
                        Snackbar.make(viewSnackBar, activity.getString(R.string.favorite_added), Snackbar.LENGTH_SHORT).show();
                    } else if (charSequence.equals(activity.getString(R.string.option_unset_favorite))) {
                        dao.deleteChannel(channel.channel_id);
                        Snackbar.make(viewSnackBar, activity.getString(R.string.favorite_removed), Snackbar.LENGTH_SHORT).show();
                    }
                    return true;

                case R.id.menu_context_share:
                    Tools.share(activity, channel.channel_name);
                    return true;

                case R.id.menu_context_quick_play:
                    Tools.startPlayer(activity, viewSnackBar, channel);
                    ((MainActivity) activity).showInterstitialAd();
                    return true;

                default:
            }
            return false;
        });
        popup.show();

        flag_read_later = dao.getChannel(channel.channel_id) != null;
        if (flag_read_later) {
            popup.getMenu().findItem(R.id.menu_context_favorite).setTitle(R.string.option_unset_favorite);
            charSequence = popup.getMenu().findItem(R.id.menu_context_favorite).getTitle();
        } else {
            popup.getMenu().findItem(R.id.menu_context_favorite).setTitle(R.string.option_set_favorite);
            charSequence = popup.getMenu().findItem(R.id.menu_context_favorite).getTitle();
        }
    }

}
