package com.app.braziltube.fragments;

import static com.app.braziltube.utils.Constant.CATEGORY_GRID_2_COLUMN;
import static com.app.braziltube.utils.Constant.CATEGORY_GRID_3_COLUMN;
import static com.app.braziltube.utils.Constant.CATEGORY_LIST_DEFAULT;
import static com.app.braziltube.utils.Constant.CHANNEL_GRID_2_COLUMN;
import static com.app.braziltube.utils.Constant.CHANNEL_GRID_3_COLUMN;
import static com.app.braziltube.utils.Constant.CHANNEL_LIST_DEFAULT;
import static com.app.braziltube.utils.Constant.PLAYER_MODE_LANDSCAPE;
import static com.app.braziltube.utils.Constant.PLAYER_MODE_PORTRAIT;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.app.braziltube.BuildConfig;
import com.app.braziltube.R;
import com.app.braziltube.activities.ActivityPrivacyPolicy;
import com.app.braziltube.activities.MainActivity;
import com.app.braziltube.databases.prefs.SharedPref;
import com.google.android.material.switchmaterial.SwitchMaterial;


public class FragmentSettings extends Fragment {

    View root_view;
    SharedPref sharedPref;
    SwitchMaterial switch_theme;
    TextView txt_current_video_list;
    TextView txt_current_category_list;
    TextView txt_current_player_mode;
    private Activity activity;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (Activity) context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root_view = inflater.inflate(R.layout.fragment_settings, container, false);
        sharedPref = new SharedPref(activity);

        initComponent();

        return root_view;
    }

    private void initComponent() {

        txt_current_video_list = root_view.findViewById(R.id.txt_current_video_list);
        if (sharedPref.getChannelViewType() == CHANNEL_LIST_DEFAULT) {
            txt_current_video_list.setText(getResources().getString(R.string.single_choice_default));
        } else if (sharedPref.getChannelViewType() == CHANNEL_GRID_2_COLUMN) {
            txt_current_video_list.setText(getResources().getString(R.string.single_choice_grid2));
        } else if (sharedPref.getChannelViewType() == CHANNEL_GRID_3_COLUMN) {
            txt_current_video_list.setText(getResources().getString(R.string.single_choice_grid3));
        }

        txt_current_category_list = root_view.findViewById(R.id.txt_current_category_list);
        if (sharedPref.getCategoryViewType() == CATEGORY_LIST_DEFAULT) {
            txt_current_category_list.setText(getResources().getString(R.string.single_choice_list));
        } else if (sharedPref.getCategoryViewType() == CATEGORY_GRID_2_COLUMN) {
            txt_current_category_list.setText(getResources().getString(R.string.single_choice_grid_2));
        } else if (sharedPref.getCategoryViewType() == CATEGORY_GRID_3_COLUMN) {
            txt_current_category_list.setText(getResources().getString(R.string.single_choice_grid_3));
        }

        txt_current_player_mode = root_view.findViewById(R.id.txt_current_player_mode);
        if (sharedPref.getPlayerMode() == PLAYER_MODE_PORTRAIT) {
            txt_current_player_mode.setText(getResources().getString(R.string.player_portrait));
        } else if (sharedPref.getPlayerMode() == PLAYER_MODE_LANDSCAPE) {
            txt_current_player_mode.setText(getResources().getString(R.string.player_landscape));
        }

        onThemeChanged();
        changeVideoListViewType();
        changeCategoryListViewType();
        changePlayerMode();

        root_view.findViewById(R.id.btn_privacy_policy).setOnClickListener(view -> startActivity(new Intent(activity, ActivityPrivacyPolicy.class)));
        root_view.findViewById(R.id.btn_rate).setOnClickListener(view -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID))));
        root_view.findViewById(R.id.btn_more).setOnClickListener(view -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(sharedPref.getMoreAppsUrl()))));
        root_view.findViewById(R.id.btn_about).setOnClickListener(view -> aboutDialog());
    }

    private void onThemeChanged() {
        switch_theme = root_view.findViewById(R.id.switch_theme);
        switch_theme.setChecked(sharedPref.getIsDarkTheme());
        switch_theme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPref.setIsDarkTheme(isChecked);
            activity.recreate();
        });

        root_view.findViewById(R.id.btn_switch_theme).setOnClickListener(v -> {
            if (switch_theme.isChecked()) {
                sharedPref.setIsDarkTheme(false);
                switch_theme.setChecked(false);
            } else {
                sharedPref.setIsDarkTheme(true);
                switch_theme.setChecked(true);
            }
            new Handler().postDelayed(() -> activity.recreate(), 10);
        });
    }

    private void changeVideoListViewType() {

        root_view.findViewById(R.id.btn_switch_list).setOnClickListener(view -> {
            String[] items = getResources().getStringArray(R.array.dialog_video_list);
            int itemSelected = sharedPref.getChannelViewType();
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.title_setting_list)
                    .setSingleChoiceItems(items, itemSelected, (dialogInterface, position) -> {
                        sharedPref.updateChannelViewType(position);

                        if (position == 0) {
                            txt_current_video_list.setText(getResources().getString(R.string.single_choice_default));
                        } else if (position == 1) {
                            txt_current_video_list.setText(getResources().getString(R.string.single_choice_grid2));
                        }

                        Intent intent = new Intent(activity, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);

                        dialogInterface.dismiss();
                    })
                    .show();
        });
    }

    private void changeCategoryListViewType() {

        root_view.findViewById(R.id.btn_switch_category).setOnClickListener(view -> {
            String[] items = getResources().getStringArray(R.array.dialog_category_list);
            int itemSelected = sharedPref.getCategoryViewType();
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.title_setting_category)
                    .setSingleChoiceItems(items, itemSelected, (dialogInterface, position) -> {
                        sharedPref.updateCategoryViewType(position);

                        if (position == 0) {
                            txt_current_category_list.setText(getResources().getString(R.string.single_choice_list));
                        } else if (position == 1) {
                            txt_current_category_list.setText(getResources().getString(R.string.single_choice_grid_2));
                        } else if (position == 2) {
                            txt_current_category_list.setText(getResources().getString(R.string.single_choice_grid_3));
                        }

                        Intent intent = new Intent(activity, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("category_position", "category_position");
                        startActivity(intent);

                        dialogInterface.dismiss();
                    })
                    .show();
        });
    }

    private void changePlayerMode() {

        root_view.findViewById(R.id.btn_switch_player_mode).setOnClickListener(view -> {
            String[] items = getResources().getStringArray(R.array.dialog_player_mode);
            int itemSelected = sharedPref.getPlayerMode();
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.title_setting_player)
                    .setSingleChoiceItems(items, itemSelected, (dialogInterface, position) -> {
                        sharedPref.updatePlayerMode(position);
                        if (position == 0) {
                            txt_current_player_mode.setText(getResources().getString(R.string.player_portrait));
                        } else if (position == 1) {
                            txt_current_player_mode.setText(getResources().getString(R.string.player_landscape));
                        }
                        dialogInterface.dismiss();
                    })
                    .show();
        });
    }

    public void aboutDialog() {
        LayoutInflater layoutInflater = LayoutInflater.from(activity);
        View view = layoutInflater.inflate(R.layout.custom_dialog_about, null);
        final AlertDialog.Builder alert = new AlertDialog.Builder(activity);

        TextView txtAppVersion = view.findViewById(R.id.txt_app_version);
        txtAppVersion.setText(getString(R.string.sub_about_app_version) + " " + BuildConfig.VERSION_NAME);

        alert.setView(view);
        alert.setCancelable(false);
        alert.setPositiveButton(R.string.option_ok, (dialog, which) -> dialog.dismiss());
        alert.show();
    }

}