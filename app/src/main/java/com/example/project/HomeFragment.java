package com.example.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

/**
 * Home screen fragment showing the colony overview, quick actions, and current threat status.
 */
public class HomeFragment extends Fragment {

    private TextView tvCrewStat;
    private TextView tvResourceStat;
    private TextView tvDayStat;
    private TextView tvMissionStat;
    private TextView tvStatusSummary;
    private TextView tvActiveThreat;
    private TextView tvGameState;
    private MainActivity mainActivity;
    private CrewAdapter crewAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();

        tvCrewStat = view.findViewById(R.id.tv_crew_stat);
        tvResourceStat = view.findViewById(R.id.tv_resource_stat);
        tvDayStat = view.findViewById(R.id.tv_day_stat);
        tvMissionStat = view.findViewById(R.id.tv_mission_stat);
        tvStatusSummary = view.findViewById(R.id.tv_status_summary);
        tvActiveThreat = view.findViewById(R.id.tv_active_threat);
        tvGameState = view.findViewById(R.id.tv_game_state);

        RecyclerView recyclerView = view.findViewById(R.id.rv_crew_status);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setNestedScrollingEnabled(false);
        crewAdapter = new CrewAdapter(mainActivity.getStorage().getAllCrew(), null);
        recyclerView.setAdapter(crewAdapter);

        view.findViewById(R.id.card_recruit)
                .setOnClickListener(v -> navigateToBottomNavFragment(new RecruitFragment(), R.id.nav_recruit));
        view.findViewById(R.id.card_mission)
                .setOnClickListener(v -> navigateToBottomNavFragment(new MissionControlFragment(), R.id.nav_mission));
        view.findViewById(R.id.card_quarters)
                .setOnClickListener(v -> navigateToBottomNavFragment(new QuartersFragment(), R.id.nav_quarters));
        view.findViewById(R.id.card_simulator)
                .setOnClickListener(v -> navigateToBottomNavFragment(new SimulatorFragment(), R.id.nav_simulator));
        view.findViewById(R.id.card_supply)
                .setOnClickListener(v -> {
                    Toast.makeText(getContext(), mainActivity.requestSupplyDrop(), Toast.LENGTH_LONG).show();
                    updateUI();
                    mainActivity.showGameOverScreenIfNeeded();
                });
        view.findViewById(R.id.card_settings)
                .setOnClickListener(v -> mainActivity.openAuxiliaryFragment(new SettingsFragment()));
        view.findViewById(R.id.card_how_to_play)
                .setOnClickListener(v -> mainActivity.openAuxiliaryFragment(new HowToPlayFragment()));

        updateUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    /**
     * Refreshes the overview each time the fragment becomes visible again.
     */
    private void updateUI() {
        if (mainActivity == null) {
            return;
        }

        Threat threat = mainActivity.ensureThreatReady();
        List<CrewMember> allCrew = mainActivity.getStorage().getAllCrew();

        int injuredCount = 0;
        int simulatorCount = 0;
        int readyCount = 0;
        int missionCount = 0;
        for (CrewMember member : allCrew) {
            if (member.isInjured()) {
                injuredCount++;
            }
            if (CrewMember.LOCATION_SIMULATOR.equals(member.getLocation())) {
                simulatorCount++;
            }
            if (CrewMember.LOCATION_MISSION_READY.equals(member.getLocation())) {
                readyCount++;
            }
            if (CrewMember.LOCATION_ON_MISSION.equals(member.getLocation())) {
                missionCount++;
            }
        }

        tvCrewStat.setText("Crew: " + allCrew.size() + "/" + mainActivity.getStorage().getMaxCrew());
        tvResourceStat.setText("Resources: " + mainActivity.getColonyResources());
        tvDayStat.setText("Day: " + mainActivity.getCurrentDay());
        tvMissionStat.setText("Completed Missions: " + mainActivity.getMissionControl().getCompletedMissions());
        tvStatusSummary.setText("Injured: " + injuredCount + " | Training: " + simulatorCount
                + " | Ready: " + readyCount + " | On Mission: " + missionCount);

        if (mainActivity.isGameOver()) {
            tvActiveThreat.setText("Threat pressure collapsed the colony.");
            tvGameState.setText(mainActivity.getGameOverReason());
        } else if (threat != null) {
            tvActiveThreat.setText("Threat: " + threat.getName() + " [" + threat.getCategory()
                    + " / " + threat.getArchetype() + "] | HP " + threat.getCurrentEnergy()
                    + "/" + threat.getMaxEnergy());
            tvGameState.setText("Deadline Day " + threat.getDeadlineDay() + " | Reward "
                    + threat.getResourceReward()
                    + " resources | Training, Rest All, supply drops, and mission resolution advance time.");
        } else {
            tvActiveThreat.setText("Threat: none");
            tvGameState.setText("No threat is currently active.");
        }

        crewAdapter.updateList(allCrew);
    }

    /**
     * Replaces the current fragment and updates the selected bottom navigation item.
     */
    private void navigateToBottomNavFragment(Fragment fragment, int navItemId) {
        if (!(getActivity() instanceof MainActivity)) {
            return;
        }

        MainActivity activity = (MainActivity) getActivity();
        activity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        BottomNavigationView navigationView = activity.findViewById(R.id.bottom_navigation);
        navigationView.setSelectedItemId(navItemId);
    }
}
