package com.inc.minst1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class InsightsAdapter extends RecyclerView.Adapter<InsightsAdapter.ViewHolder> {

    private final List<Insight> insights = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_insight_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Insight insight = insights.get(position);
        holder.icon.setImageResource(insight.getIconRes());
        holder.title.setText(insight.getTitle());
        holder.value.setText(insight.getValue());

        // Handle the new subtitle
        if (insight.getSubtitle() != null && !insight.getSubtitle().isEmpty()) {
            holder.subtitle.setText(insight.getSubtitle());
            holder.subtitle.setVisibility(View.VISIBLE);
        } else {
            holder.subtitle.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return insights.size();
    }

    public void updateInsights(List<Insight> newInsights) {
        insights.clear();
        insights.addAll(newInsights);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView value;
        TextView subtitle; // NEW

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.ivInsightIcon);
            title = itemView.findViewById(R.id.tvInsightTitle);
            value = itemView.findViewById(R.id.tvInsightValue);
            subtitle = itemView.findViewById(R.id.tvInsightSubtitle); // NEW
        }
    }
}