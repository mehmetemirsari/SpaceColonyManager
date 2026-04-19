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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        refresh(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            refresh(getView());
        }
    }

    /**
     * Updates the colony statistics and crew detail text blocks.
     */
    private void refresh(@NonNull View view) {
        TextView tvStats = view.findViewById(R.id.tv_statistics);
        TextView tvCrewList = view.findViewById(R.id.tv_crew_stats_list);

        Threat threat = mainActivity.ensureThreatReady();
        StringBuilder statsBuilder = new StringBuilder(mainActivity.getStatisticsManager().getStats());
        statsBuilder.append("\nCurrent Day: ").append(mainActivity.getCurrentDay());
        statsBuilder.append("\nResources: ").append(mainActivity.getColonyResources());
        if (mainActivity.isGameOver()) {
            statsBuilder.append("\nStatus: LOST");
            statsBuilder.append("\nReason: ").append(mainActivity.getGameOverReason());
        } else if (threat != null) {
            statsBuilder.append("\nActive Threat: ").append(threat.getName())
                    .append(" [").append(threat.getCategory()).append("]")
                    .append("\nThreat Deadline: Day ").append(threat.getDeadlineDay());
        }
        tvStats.setText(statsBuilder.toString());

        StringBuilder crewBuilder = new StringBuilder("=== CREW DETAILS ===\n");
        for (CrewMember member : mainActivity.getStorage().getAllCrew()) {
            crewBuilder.append(member.getName())
                    .append(" (").append(member.getSpecialization()).append(")")
                    .append(" | Lv.").append(member.getLevel())
                    .append(" | XP: ").append(member.getExperience())
                    .append(" | Next: ").append(member.getXpNeededForNextLevel()).append(" XP")
                    .append(" | ATK: ").append(member.getSkill())
                    .append(" | HP: ").append(member.getCurrentEnergy()).append("/")
                    .append(member.getMaxEnergy())
                    .append(" | Location: ").append(member.getLocation());
            if (member.isInjured()) {
                crewBuilder.append(" | Injured");
            }
            if (member.getMissionPenaltyRemaining() > 0) {
                crewBuilder.append(" | Penalty: ").append(member.getMissionPenaltyRemaining());
            }
            crewBuilder.append("\n");
        }

        if (mainActivity.getStorage().getAllCrew().isEmpty()) {
            crewBuilder.append("No crew members recruited yet. Visit Recruitment to hire your first specialist.\n");
        }
        tvCrewList.setText(crewBuilder.toString());
    }
}
