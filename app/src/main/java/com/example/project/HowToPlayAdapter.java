package com.example.project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * RecyclerView adapter that renders the How to Play guide as grouped section cards.
 */
public class HowToPlayAdapter extends RecyclerView.Adapter<HowToPlayAdapter.HowToPlayViewHolder> {

    /**
     * Simple immutable section model for the guide.
     */
    public static class GuideSection {
        private final String title;
        private final String body;

        /**
         * Creates one guide section.
         *
         * @param title section headline
         * @param body section explanation
         */
        public GuideSection(String title, String body) {
            this.title = title;
            this.body = body;
        }

        /**
         * @return section headline
         */
        public String getTitle() {
            return title;
        }

        /**
         * @return section explanation
         */
        public String getBody() {
            return body;
        }
    }

    private final List<GuideSection> sections;

    /**
     * Creates the adapter for the static help guide.
     *
     * @param sections ordered guide sections to display
     */
    public HowToPlayAdapter(List<GuideSection> sections) {
        this.sections = sections;
    }

    @NonNull
    @Override
    public HowToPlayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_how_to_play_section, parent, false);
        return new HowToPlayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HowToPlayViewHolder holder, int position) {
        GuideSection section = sections.get(position);
        holder.tvIndex.setText(String.valueOf(position + 1));
        holder.tvTitle.setText(section.getTitle());
        holder.tvBody.setText(section.getBody());
    }

    @Override
    public int getItemCount() {
        return sections.size();
    }

    /**
     * View holder for one help section card.
     */
    static class HowToPlayViewHolder extends RecyclerView.ViewHolder {
        final TextView tvIndex;
        final TextView tvTitle;
        final TextView tvBody;

        /**
         * Caches the section card views.
         *
         * @param itemView inflated item view
         */
        HowToPlayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tv_section_index);
            tvTitle = itemView.findViewById(R.id.tv_section_title);
            tvBody = itemView.findViewById(R.id.tv_section_body);
        }
    }
}
