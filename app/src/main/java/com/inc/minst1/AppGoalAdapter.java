package com.inc.minst1;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AppGoalAdapter extends RecyclerView.Adapter<AppGoalAdapter.ViewHolder> {
    private List<AppGoalModel> appList;
    private Map<String, Long> goalMap;
    private Map<String, Long> usageMap; // To hold current usage
    private final OnAppClickListener listener;

    public interface OnAppClickListener {
        void onAppClick(AppGoalModel appModel, Long usage);
    }

    public AppGoalAdapter(List<AppGoalModel> appList, Map<String, Long> goalMap, Map<String, Long> usageMap, OnAppClickListener listener) {
        this.appList = appList;
        this.goalMap = goalMap;
        this.usageMap = usageMap;
        this.listener = listener;
    }

    // THIS IS THE CORRECTED METHOD SIGNATURE
    public void updateData(List<AppGoalModel> newAppList, Map<String, Long> newGoalMap, Map<String, Long> newUsageMap) {
        this.appList = newAppList;
        this.goalMap = newGoalMap;
        this.usageMap = newUsageMap;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_goal_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppGoalModel appModel = appList.get(position);
        Long goal = goalMap.get(appModel.appInfo.packageName);
        Long usage = usageMap.getOrDefault(appModel.appInfo.packageName, 0L);
        holder.bind(appModel, goal, usage, listener);
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAppIcon;
        TextView tvAppName, tvAppGoalStatus;
        ProgressBar pbGoalProgress;
        Context context;

        ViewHolder(View view) {
            super(view);
            context = view.getContext();
            ivAppIcon = view.findViewById(R.id.ivAppIcon);
            tvAppName = view.findViewById(R.id.tvAppName);
            tvAppGoalStatus = view.findViewById(R.id.tvAppGoalStatus);
            pbGoalProgress = view.findViewById(R.id.pbGoalProgress);
        }

        void bind(final AppGoalModel appModel, Long goal, Long usage, final OnAppClickListener listener) {
            ivAppIcon.setImageDrawable(appModel.appIcon);
            tvAppName.setText(appModel.appName);

            if (goal != null && goal > 0) {
                pbGoalProgress.setVisibility(View.VISIBLE);

                long remainingMillis = goal - usage;
                int progress = (int) ((usage * 100) / goal);
                pbGoalProgress.setProgress(Math.min(100, progress)); // Cap progress at 100

                if (remainingMillis > 0) {
                    tvAppGoalStatus.setText(UsageStatsHelper.formatTime(remainingMillis) + " left");

                    if (remainingMillis < TimeUnit.MINUTES.toMillis(15)) { // Less than 15 mins left
                        tvAppGoalStatus.setTextColor(ContextCompat.getColor(context, R.color.warning_orange));
                        pbGoalProgress.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(context, R.color.warning_orange), PorterDuff.Mode.SRC_IN));
                    } else { // Plenty of time left
                        tvAppGoalStatus.setTextColor(ContextCompat.getColor(context, R.color.textColorSecondary));
                        pbGoalProgress.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(context, R.color.highlight_green), PorterDuff.Mode.SRC_IN));
                    }
                } else {
                    tvAppGoalStatus.setText("Limit Reached");
                    tvAppGoalStatus.setTextColor(ContextCompat.getColor(context, R.color.warning_red));
                    pbGoalProgress.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(context, R.color.warning_red), PorterDuff.Mode.SRC_IN));
                }
            } else {
                pbGoalProgress.setVisibility(View.GONE);
                tvAppGoalStatus.setText("Set Limit");
                tvAppGoalStatus.setTextColor(ContextCompat.getColor(context, R.color.brand_purple_primary));
            }
            itemView.setOnClickListener(v -> listener.onAppClick(appModel, usage));
        }
    }
}