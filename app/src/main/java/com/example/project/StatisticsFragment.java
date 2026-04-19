package com.example.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Fragment that displays colony-wide mission statistics and a detailed per-crew summary.
 */
public class StatisticsFragment extends Fragment {

    private MainActivity mainActivity;

    /**
     * Inflates the statistics screen layout.
     *
     * @param inflater layout inflater
     * @param container parent container
     * @param savedInstanceState saved instance state, if any
     * @return inflated statistics fragment view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    /**
     * Binds the activity reference and fills the statistics view.
     *
     * @param view created fragment view
     * @param savedInstanceState saved instance state, if any
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        refresh(view);
    }

    /**
     * Refreshes visible statistics whenever the fragment resumes.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) refresh(getView());
    }

    /**
     * Updates the colony statistics and crew detail text blocks.
     *
     * @param view root fragment view
     */
    private void refresh(View view) {
        TextView tvStats = view.findViewById(R.id.tv_statistics);
        TextView tvCrewList = view.findViewById(R.id.tv_crew_stats_list);

        if (mainActivity == null) return;

        tvStats.setText(mainActivity.getStatisticsManager().getStats());

        StringBuilder sb = new StringBuilder("=== CREW DETAILS ===\n");
        for (CrewMember c : mainActivity.getStorage().getAllCrew()) {
            sb.append(c.getName())
                    .append(" (").append(c.getSpecialization()).append(")")
                    .append(" — Lv.").append(c.getLevel())
                    .append(" | EXP: ").append(c.getExperience())
                    .append(" | Energy: ").append(c.getCurrentEnergy()).append("/").append(c.getMaxEnergy())
                    .append(" | Location: ").append(c.getLocation())
                    .append(c.isInjured() ? " [INJURED]" : "")
                    .append("\n");
        }
        if (mainActivity.getStorage().getAllCrew().isEmpty()) {
            sb.append("No crew members recruited yet. Visit Recruitment to hire your first specialist.\n");
        }
        tvCrewList.setText(sb.toString());
    }
}
