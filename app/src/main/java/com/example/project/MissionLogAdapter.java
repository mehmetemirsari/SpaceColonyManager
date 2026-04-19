package com.example.project;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the smooth step-by-step mission replay log.
 */
public class MissionLogAdapter extends RecyclerView.Adapter<MissionLogAdapter.MissionEventViewHolder> {

    private final List<MissionEvent> events = new ArrayList<>();

    /**
     * Clears all visible replay events.
     */
    public void clear() {
        events.clear();
        notifyDataSetChanged();
    }

    /**
     * Appends one replay event.
     *
     * @param event event to add
     */
    public void addEvent(MissionEvent event) {
        events.add(event);
        notifyItemInserted(events.size() - 1);
    }

    @NonNull
    @Override
    public MissionEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mission_event, parent, false);
        return new MissionEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MissionEventViewHolder holder, int position) {
        MissionEvent event = events.get(position);
        holder.title.setText(event.getTitle());
        holder.description.setText(event.getDescription());
        holder.cardView.setCardBackgroundColor(getColorForType(event.getType()));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * Colors mission log entries by event type so the replay is easier to scan.
     */
    private int getColorForType(String type) {
        switch (type) {
            case MissionEvent.TYPE_BONUS:
                return Color.parseColor("#193A38");
            case MissionEvent.TYPE_ATTACK:
                return Color.parseColor("#2D233B");
            case MissionEvent.TYPE_DEFENSE:
                return Color.parseColor("#1F2D40");
            case MissionEvent.TYPE_REWARD:
                return Color.parseColor("#3A3419");
            case MissionEvent.TYPE_OUTCOME:
                return Color.parseColor("#3B1F26");
            default:
                return Color.parseColor("#151A28");
        }
    }

    /**
     * View holder for one mission replay event.
     */
    static class MissionEventViewHolder extends RecyclerView.ViewHolder {
        final CardView cardView;
        final TextView title;
        final TextView description;

        MissionEventViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            title = itemView.findViewById(R.id.tv_event_title);
            description = itemView.findViewById(R.id.tv_event_description);
        }
    }
}
