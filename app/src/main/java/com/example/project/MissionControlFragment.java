package com.example.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment responsible for mission setup, tactical choices, mission cancellation, and resolution.
 */
public class MissionControlFragment extends Fragment {

    private MainActivity mainActivity;
    private Spinner spinner1, spinner2, spinnerTactic1, spinnerTactic2;
    private TextView tvThreatName, tvMissionLog;
    private View btnLaunch, btnCancel, btnResolve;
    private ScrollView scrollLog;
    private List<CrewMember> availableCrew;

    /**
     * Inflates the Mission Control screen layout.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mission_control, container, false);
    }

    /**
     * Binds views, loads available crew, restores any active mission state, and wires mission controls.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();

        spinner1 = view.findViewById(R.id.spinner_crew_1);
        spinner2 = view.findViewById(R.id.spinner_crew_2);
        spinnerTactic1 = view.findViewById(R.id.spinner_tactic_1);
        spinnerTactic2 = view.findViewById(R.id.spinner_tactic_2);
        tvThreatName = view.findViewById(R.id.tv_threat_name);
        tvMissionLog = view.findViewById(R.id.tv_mission_log);
        btnLaunch = view.findViewById(R.id.btn_launch_mission);
        btnCancel = view.findViewById(R.id.btn_cancel_mission);
        btnResolve = view.findViewById(R.id.btn_resolve_mission);
        scrollLog = view.findViewById(R.id.scroll_mission_log);

        setupCrewSpinners();
        setupTacticSpinners();
        refreshActiveMissionState();
        updateButtonStates();

        btnLaunch.setOnClickListener(v -> startMissionProcess());
        btnResolve.setOnClickListener(v -> resolveMissionProcess());
        btnCancel.setOnClickListener(v -> {
            String result = mainActivity.getMissionControl().cancelMission();
            tvMissionLog.setText(result);
            tvThreatName.setText("No Active Threat");
            setupCrewSpinners();
            updateButtonStates();
            Toast.makeText(getContext(), "Mission cancelled.", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Refreshes available crew and active mission state when the fragment becomes visible again.
     */
    @Override
    public void onResume() {
        super.onResume();
        setupCrewSpinners();
        refreshActiveMissionState();
        updateButtonStates();
    }

    /**
     * Populates the tactic spinners with Attack and Defend options.
     */
    private void setupTacticSpinners() {
        String[] tactics = {"Attack", "Defend"};
        ArrayAdapter<String> tacticAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, tactics);
        spinnerTactic1.setAdapter(tacticAdapter);
        spinnerTactic2.setAdapter(tacticAdapter);
    }

    /**
     * Populates the crew selection spinners with healthy crew members not already on a mission.
     */
    private void setupCrewSpinners() {
        availableCrew = new ArrayList<>();
        for (CrewMember c : mainActivity.getStorage().getAllCrew()) {
            if (!c.isInjured() && !"MissionControl".equals(c.getLocation())) {
                availableCrew.add(c);
            }
        }

        List<String> crewNames = new ArrayList<>();
        for (CrewMember c : availableCrew) {
            crewNames.add(c.getName() + " (" + c.getSpecialization() + ") — EXP: " + c.getExperience()
                    + " | " + c.getCurrentEnergy() + "/" + c.getMaxEnergy() + " HP");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, crewNames);
        spinner1.setAdapter(adapter);
        spinner2.setAdapter(adapter);

        if (availableCrew.size() >= 2) {
            spinner2.setSelection(1);
        }
    }

    /**
     * Starts a new mission using the selected crew members.
     */
    private void startMissionProcess() {
        if (mainActivity.getMissionControl().getCurrentThreat() != null) {
            Toast.makeText(getContext(), "A mission is already active!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (availableCrew == null || availableCrew.size() < 2) {
            Toast.makeText(getContext(), "Need at least 2 healthy crew members!", Toast.LENGTH_SHORT).show();
            return;
        }

        int idx1 = spinner1.getSelectedItemPosition();
        int idx2 = spinner2.getSelectedItemPosition();

        if (idx1 == idx2) {
            Toast.makeText(getContext(), "Select two different crew members!", Toast.LENGTH_SHORT).show();
            return;
        }

        CrewMember m1 = availableCrew.get(idx1);
        CrewMember m2 = availableCrew.get(idx2);

        MissionControl mc = mainActivity.getMissionControl();
        String launchLog = mc.launchMission(m1, m2);
        tvThreatName.setText("ACTIVE THREAT DETECTED");
        tvMissionLog.setText(launchLog + "\n\nMission is active. Choose tactics and resolve, or cancel.");
        setupCrewSpinners();
        updateButtonStates();
    }

    /**
     * Passes the selected tactics to mission control and resolves the active mission.
     */
    private void resolveMissionProcess() {
        MissionControl mc = mainActivity.getMissionControl();
        if (mc.getCurrentThreat() == null) {
            Toast.makeText(getContext(), "No active mission to resolve.", Toast.LENGTH_SHORT).show();
            return;
        }

        String tacticA = spinnerTactic1.getSelectedItem().toString();
        String tacticB = spinnerTactic2.getSelectedItem().toString();
        mc.setTactics(tacticA, tacticB);

        String missionLog = mc.resolveMission();
        tvMissionLog.setText(missionLog);
        tvThreatName.setText("Mission Concluded");
        setupCrewSpinners();
        updateButtonStates();

        if (scrollLog != null) {
            scrollLog.post(() -> scrollLog.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }

    /**
     * Restores the mission info panel if a mission is already active.
     */
    private void refreshActiveMissionState() {
        MissionControl mc = mainActivity.getMissionControl();
        if (mc.getCurrentThreat() != null) {
            Threat threat = mc.getCurrentThreat();
            tvThreatName.setText("ACTIVE THREAT DETECTED");
            tvMissionLog.setText("Mission already active!\n\nThreat encountered: "
                    + threat.getName() + " [" + threat.getThreatType() + "]"
                    + "\nThreat — skill: " + threat.getSkill()
                    + ", resilience: " + threat.getResilience()
                    + ", energy: " + threat.getEnergy() + "/" + threat.getMaxEnergy()
                    + "\n\nChoose tactics and resolve, or cancel.");
        }
    }

    /**
     * Enables or disables mission buttons depending on whether a mission is active.
     */
    private void updateButtonStates() {
        boolean hasActiveMission = mainActivity.getMissionControl().getCurrentThreat() != null;
        btnLaunch.setEnabled(!hasActiveMission);
        btnResolve.setEnabled(hasActiveMission);
        btnCancel.setEnabled(hasActiveMission);
    }
}
