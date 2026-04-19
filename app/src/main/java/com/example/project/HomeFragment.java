package com.example.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * Home screen fragment showing the colony overview, quick actions, and current crew status.
 */
public class HomeFragment extends Fragment {

    private TextView tvCrewStat, tvResourceStat, tvDayStat, tvMissionStat, tvStatusSummary;
    private MainActivity mainActivity;

    /**
     * Inflates the home screen layout.
     *
     * @param inflater layout inflater
     * @param container parent container
     * @param savedInstanceState saved instance state, if any
     * @return inflated home fragment view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    /**
     * Binds views, configures quick navigation cards, and populates the initial UI.
     *
     * @param view created fragment view
     * @param savedInstanceState saved instance state, if any
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();

        tvCrewStat = view.findViewById(R.id.tv_crew_stat);
        tvResourceStat = view.findViewById(R.id.tv_resource_stat);
        tvDayStat = view.findViewById(R.id.tv_day_stat);
        tvMissionStat = view.findViewById(R.id.tv_mission_stat);
        tvStatusSummary = view.findViewById(R.id.tv_status_summary);

        view.findViewById(R.id.card_recruit).setOnClickListener(v ->
                navigateToFragment(new RecruitFragment(), R.id.nav_recruit));
        view.findViewById(R.id.card_mission).setOnClickListener(v ->
                navigateToFragment(new MissionControlFragment(), R.id.nav_mission));
        view.findViewById(R.id.card_quarters).setOnClickListener(v ->
                navigateToFragment(new QuartersFragment(), R.id.nav_quarters));
        view.findViewById(R.id.card_simulator).setOnClickListener(v ->
                navigateToFragment(new SimulatorFragment(), R.id.nav_simulator));

        RecyclerView rvCrewStatus = view.findViewById(R.id.rv_crew_status);
        rvCrewStatus.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCrewStatus.setNestedScrollingEnabled(false);
        rvCrewStatus.setAdapter(new CrewAdapter(mainActivity.getStorage().getAllCrew(), null));

        updateUI();
    }

    /**
     * Refreshes the overview each time the fragment becomes visible again.
     */
    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    /**
     * Rebuilds the overview statistics and crew list using the current game state.
     */
    private void updateUI() {
        if (mainActivity == null || getView() == null) return;
        List<CrewMember> allCrew = mainActivity.getStorage().getAllCrew();

        int injuredCount = 0;
        int simulatorCount = 0;
        int missionCount = 0;
        for (CrewMember member : allCrew) {
            if (member.isInjured()) injuredCount++;
            if ("Simulator".equals(member.getLocation())) simulatorCount++;
            if ("MissionControl".equals(member.getLocation())) missionCount++;
        }

        tvCrewStat.setText("Crew: " + allCrew.size() + "/" + mainActivity.getStorage().getMaxCrew());
        tvResourceStat.setText("Resources: " + mainActivity.getColonyResources());
        tvDayStat.setText("Day: " + mainActivity.getCurrentDay());
        tvMissionStat.setText("Completed Missions: " + mainActivity.getMissionControl().getCompletedMissions());
        tvStatusSummary.setText("Injured: " + injuredCount + "   •   Training: " + simulatorCount + "   •   On Mission: " + missionCount);

        RecyclerView rv = getView().findViewById(R.id.rv_crew_status);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new CrewAdapter(allCrew, null));
    }

    /**
     * Replaces the current fragment and updates the selected bottom navigation item.
     *
     * @param fragment destination fragment
     * @param navItemId bottom navigation item id to select
     */
    private void navigateToFragment(Fragment fragment, int navItemId) {
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
            com.google.android.material.bottomnavigation.BottomNavigationView nav =
                    activity.findViewById(R.id.bottom_navigation);
            nav.setSelectedItemId(navItemId);
        }
    }
}
