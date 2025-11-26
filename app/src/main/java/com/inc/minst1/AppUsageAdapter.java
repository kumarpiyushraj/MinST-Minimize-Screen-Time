package com.inc.minst1;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class AppUsageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_APP = 1;
    private static final int VIEW_TYPE_DIVIDER = 2;

    private final Context ctx;
    private List<Object> list;
    private final Clicks clicks;
    private String packageToHighlight;

    public interface Clicks {
        void onAppClick(AppUsageModel model, int pos);
        void onMoreClick(AppUsageModel model, View anchorView);
    }

    public AppUsageAdapter(Context ctx, List<Object> list, Clicks clicks, String packageToHighlight) {
        this.ctx = ctx;
        this.list = new ArrayList<>(list);
        this.clicks = clicks;
        this.packageToHighlight = packageToHighlight;
    }

    public void updateList(List<Object> newList) {
        list = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (list.get(position) instanceof HeaderModel) return VIEW_TYPE_HEADER;
        if (list.get(position) instanceof DividerModel) return VIEW_TYPE_DIVIDER;
        return VIEW_TYPE_APP;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            return new HeaderVH(LayoutInflater.from(ctx).inflate(R.layout.item_header, parent, false));
        } else if (viewType == VIEW_TYPE_DIVIDER) {
            return new DividerVH(LayoutInflater.from(ctx).inflate(R.layout.item_divider, parent, false));
        } else {
            return new AppVH(LayoutInflater.from(ctx).inflate(R.layout.item_app_usage, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
            HeaderModel m = (HeaderModel) list.get(position);
            ((HeaderVH) holder).tvTitle.setText(m.getTitle());
        } else if (holder.getItemViewType() == VIEW_TYPE_APP) {
            AppUsageModel m = (AppUsageModel) list.get(position);
            AppVH appHolder = (AppVH) holder;

            if (packageToHighlight != null && m.getPkg().equals(packageToHighlight)) {
                appHolder.itemCard.setStrokeColor(ContextCompat.getColor(ctx, R.color.highlight_green));
                appHolder.itemCard.setStrokeWidth(6);
            } else {
                appHolder.itemCard.setStrokeWidth(0);
            }

            appHolder.tvName.setText(m.getName());
            appHolder.tvUsage.setText(UsageStatsHelper.formatTime(m.getMs()));

            // --- THIS IS THE FIX ---
            // Load the icon dynamically here instead of relying on the model.
            try {
                Drawable icon = ctx.getPackageManager().getApplicationIcon(m.getPkg());
                appHolder.ivIcon.setImageDrawable(icon);
            } catch (PackageManager.NameNotFoundException e) {
                // Set a fallback icon if the app's icon can't be found
                appHolder.ivIcon.setImageResource(R.mipmap.ic_launcher);
            }
            // --- End of Fix ---

            appHolder.itemView.setOnClickListener(v -> {
                int pos = appHolder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) clicks.onAppClick(m, pos);
            });

            appHolder.btnMoreOptions.setOnClickListener(v -> {
                int pos = appHolder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    clicks.onMoreClick(m, appHolder.btnMoreOptions);
                }
            });

            appHolder.usageProgressBar.setProgress(m.getPercent());
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class DividerVH extends RecyclerView.ViewHolder { DividerVH(@NonNull View v) { super(v); }}
    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tvTitle;
        HeaderVH(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvHeaderTitle);
        }
    }
    static class AppVH extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName, tvUsage;
        ProgressBar usageProgressBar;
        ImageButton btnMoreOptions;
        MaterialCardView itemCard;
        AppVH(@NonNull View v) {
            super(v);
            ivIcon = v.findViewById(R.id.ivAppIcon);
            tvName = v.findViewById(R.id.tvAppName);
            tvUsage = v.findViewById(R.id.tvAppUsage);
            usageProgressBar = v.findViewById(R.id.usageProgressBar);
            btnMoreOptions = v.findViewById(R.id.btnMoreOptions);
            itemCard = v.findViewById(R.id.itemCard);
        }
    }
}