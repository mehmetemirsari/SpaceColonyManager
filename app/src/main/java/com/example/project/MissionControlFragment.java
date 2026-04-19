package com.example.project;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fragment responsible for mission setup, tactical choices, and mission replay playback.
 */
public class MissionControlFragment extends Fragment {

    private MainActivity mainActivity;
    private Spinner spinnerCrew1;
    private Spinner spinnerCrew2;
    private Spinner spinnerTactic1;
    private Spinner spinnerTactic2;
    private TextView tvThreatName;
    private TextView tvThreatDetails;
    private View btnLaunch;
    private View btnCancel;
    private View btnResolve;
    private RecyclerView rvMissionLog;
    private MissionLogAdapter missionLogAdapter;
    private final Handler replayHandler = new Handler(Looper.getMainLooper());
    private List<CrewMember> availableCrew = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mission_control, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();

        spinnerCrew1 = view.findViewById(R.id.spinner_crew_1);
        spinnerCrew2 = view.findViewById(R.id.spinner_crew_2);
        spinnerTactic1 = view.findViewById(R.id.spinner_tactic_1);
        spinnerTactic2 = view.findViewById(R.id.spinner_tactic_2);
        tvThreatName = view.findViewById(R.id.tv_threat_name);
        tvThreatDetails = view.findViewById(R.id.tv_threat_details);
        btnLaunch = view.findViewById(R.id.btn_launch_mission);
        btnCancel = view.findViewById(R.id.btn_cancel_mission);
        btnResolve = view.findViewById(R.id.btn_resolve_mission);
        rvMissionLog = view.findViewById(R.id.rv_mission_log);

        rvMissionLog.setLayoutManager(new LinearLayoutManager(getContext()));
        missionLogAdapter = new MissionLogAdapter();
        rvMissionLog.setAdapter(missionLogAdapter);

        setupTacticSpinners();
        setupCrewSpinners();
        refreshThreatState();
        updateButtonStates();

        btnLaunch.setOnClickListener(v -> startMissionProcess());
        btnResolve.setOnClickListener(v -> resolveMissionProcess());
        btnCancel.setOnClickListener(v -> {
            missionLogAdapter.clear();
            missionLogAdapter.addEvent(new MissionEvent(MissionEvent.TYPE_INFO, "Mission Cancelled",
                    mainActivity.getMissionControl().cancelMission()));
            refreshThreatState();
            setupCrewSpinners();
            updateButtonStates();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        setupCrewSpinners();
        refreshThreatState();
        updateButtonStates();
    }

    @Override
    public void onPause() {
        super.onPause();
        replayHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Populates the tactic spinners with Attack and Defend options.
     */
    private void setupTacticSpinners() {
        String[] tactics = {MissionControl.TACTIC_ATTACK, MissionControl.TACTIC_DEFEND};
        ThemedSpinnerAdapter tacticAdapter = new ThemedSpinnerAdapter(requireContext(), tactics);
        spinnerTactic1.setAdapter(tacticAdapter);
        spinnerTactic2.setAdapter(tacticAdapter);
    }

    /**
     * Populates the crew selection spinners with mission-ready crew.
     */
    private void setupCrewSpinners() {
        availableCrew = new ArrayList<>();
        for (CrewMember crewMember : mainActivity.getStorage().getAllCrew()) {
            if (CrewMember.LOCATION_MISSION_READY.equals(crewMember.getLocation())
                    && crewMember.isAvailableForMission()) {
                availableCrew.add(crewMember);
            }
        }

        List<String> crewLabels = new ArrayList<>();
        for (CrewMember crewMember : availableCrew) {
            crewLabels.add(crewMember.getName() + " (" + crewMember.getSpecialization() + ")"
                    + " | Lv." + crewMember.getLevel()
                    + " | HP " + crewMember.getCurrentEnergy() + "/" + crewMember.getMaxEnergy());
        }

        if (crewLabels.isEmpty()) {
            crewLabels = Collections.singletonList("No mission-ready crew");
        }

        ThemedSpinnerAdapter adapter = new ThemedSpinnerAdapter(requireContext(), crewLabels);
        spinnerCrew1.setAdapter(adapter);
        spinnerCrew2.setAdapter(adapter);
        if (availableCrew.size() >= 2) {
            spinnerCrew2.setSelection(1);
        }
    }

    /**
     * Starts a new mission using the selected crew members.
     */
    private void startMissionProcess() {
        if (mainActivity.isGameOver()) {
            showToast(mainActivity.getGameOverReason());
            return;
        }

        if (availableCrew.size() < 2) {
            showToast("Move at least two penalty-free crew members to Mission Ready.");
            return;
        }

        int idx1 = spinnerCrew1.getSelectedItemPosition();
        int idx2 = spinnerCrew2.getSelectedItemPosition();
        if (idx1 == idx2) {
            showToast("Select two different crew members.");
            return;
        }

        CrewMember member1 = availableCrew.get(idx1);
        CrewMember member2 = availableCrew.get(idx2);

        String launchResult = mainActivity.getMissionControl().launchMission(member1, member2,
                mainActivity.getCurrentDay());
        missionLogAdapter.clear();
        missionLogAdapter.addEvent(new MissionEvent(MissionEvent.TYPE_INFO, "Mission Launch",
                launchResult));
        refreshThreatState();
        setupCrewSpinners();
        updateButtonStates();
        scrollLogToBottom();
    }

    /**
     * Passes the selected tactics to mission control and plays back the resulting mission replay.
     */
    private void resolveMissionProcess() {
        MissionControl missionControl = mainActivity.getMissionControl();
        if (!missionControl.hasActiveMission()) {
            showToast("No active mission to resolve.");
            return;
        }

        missionControl.setTactics(
                String.valueOf(spinnerTactic1.getSelectedItem()),
                String.valueOf(spinnerTactic2.getSelectedItem()));

        MissionResolution resolution = mainActivity.resolveActiveMission();
        playResolution(resolution);
    }

    /**
     * Smoothly reveals one event at a time in the mission log.
     */
    private void playResolution(MissionResolution resolution) {
        replayHandler.removeCallbacksAndMessages(null);
        missionLogAdapter.clear();

        List<MissionEvent> events = resolution.getEvents();
        if (events.isEmpty()) {
            refreshThreatState();
            setupCrewSpinners();
            updateButtonStates();
            return;
        }

        tvThreatName.setText("MISSION REPLAY");
        tvThreatDetails.setText("Reviewing the most recent encounter step by step.");

        for (int i = 0; i < events.size(); i++) {
            final MissionEvent event = events.get(i);
            replayHandler.postDelayed(() -> {
                missionLogAdapter.addEvent(event);
                scrollLogToBottom();
            }, i * 420L);
        }

        replayHandler.postDelayed(() -> {
            refreshThreatState();
            setupCrewSpinners();
            updateButtonStates();
            mainActivity.showGameOverScreenIfNeeded();
        }, events.size() * 420L + 120L);
    }

    /**
     * Updates the active threat header for the current colony state.
     */
    private void refreshThreatState() {
        if (mainActivity.isGameOver()) {
            tvThreatName.setText("COLONY LOST");
            tvThreatDetails.setText(mainActivity.getGameOverReason());
            return;
        }

        Threat threat = mainActivity.ensureThreatReady();
        if (threat == null) {
            tvThreatName.setText("No Active Threat");
            tvThreatDetails.setText("The colony currently has no active danger.");
            return;
        }

        tvThreatName.setText((mainActivity.getMissionControl().hasActiveMission() ? "ACTIVE MISSION: "
                : "ACTIVE THREAT: ") + threat.getName());
        tvThreatDetails.setText(threat.getCategory() + " / " + threat.getArchetype()
                + "\nHP: " + threat.getCurrentEnergy() + "/" + threat.getMaxEnergy()
                + "\nDeadline: Day " + threat.getDeadlineDay()
                + "\nReward: " + threat.getResourceReward() + " resources, "
                + threat.getExperienceReward() + " XP");
    }

    /**
     * Enables or disables mission buttons depending on mission state and game-over state.
     */
    private void updateButtonStates() {
        boolean hasActiveMission = mainActivity.getMissionControl().hasActiveMission();
        boolean canPlay = !mainActivity.isGameOver();
        btnLaunch.setEnabled(canPlay && !hasActiveMission && availableCrew.size() >= 2);
        btnResolve.setEnabled(canPlay && hasActiveMission);
        btnCancel.setEnabled(canPlay && hasActiveMission);
    }

    /**
     * Keeps the latest event visible as the replay animates.
     */
    private void scrollLogToBottom() {
        if (missionLogAdapter.getItemCount() > 0) {
            rvMissionLog.scrollToPosition(missionLogAdapter.getItemCount() - 1);
        }
    }

    /**
     * Convenience Toast helper.
     */
    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }
}
