package com.example.project;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
    private TextView tvPreviewCrew1;
    private TextView tvPreviewCrew1AttackTitle;
    private TextView tvPreviewCrew1AttackDetails;
    private TextView tvPreviewCrew1DefendTitle;
    private TextView tvPreviewCrew1DefendDetails;
    private TextView tvPreviewCrew2;
    private TextView tvPreviewCrew2AttackTitle;
    private TextView tvPreviewCrew2AttackDetails;
    private TextView tvPreviewCrew2DefendTitle;
    private TextView tvPreviewCrew2DefendDetails;
    private View previewCrew1AttackCard;
    private View previewCrew1DefendCard;
    private View previewCrew2AttackCard;
    private View previewCrew2DefendCard;
    private View btnLaunch;
    private View btnCancel;
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
        tvPreviewCrew1 = view.findViewById(R.id.tv_preview_crew_1);
        tvPreviewCrew1AttackTitle = view.findViewById(R.id.tv_preview_1_attack_title);
        tvPreviewCrew1AttackDetails = view.findViewById(R.id.tv_preview_1_attack_details);
        tvPreviewCrew1DefendTitle = view.findViewById(R.id.tv_preview_1_defend_title);
        tvPreviewCrew1DefendDetails = view.findViewById(R.id.tv_preview_1_defend_details);
        tvPreviewCrew2 = view.findViewById(R.id.tv_preview_crew_2);
        tvPreviewCrew2AttackTitle = view.findViewById(R.id.tv_preview_2_attack_title);
        tvPreviewCrew2AttackDetails = view.findViewById(R.id.tv_preview_2_attack_details);
        tvPreviewCrew2DefendTitle = view.findViewById(R.id.tv_preview_2_defend_title);
        tvPreviewCrew2DefendDetails = view.findViewById(R.id.tv_preview_2_defend_details);
        previewCrew1AttackCard = view.findViewById(R.id.card_preview_1_attack);
        previewCrew1DefendCard = view.findViewById(R.id.card_preview_1_defend);
        previewCrew2AttackCard = view.findViewById(R.id.card_preview_2_attack);
        previewCrew2DefendCard = view.findViewById(R.id.card_preview_2_defend);
        btnLaunch = view.findViewById(R.id.btn_launch_mission);
        btnCancel = view.findViewById(R.id.btn_cancel_mission);
        rvMissionLog = view.findViewById(R.id.rv_mission_log);

        rvMissionLog.setLayoutManager(new LinearLayoutManager(getContext()));
        missionLogAdapter = new MissionLogAdapter();
        rvMissionLog.setAdapter(missionLogAdapter);
        rvMissionLog.setNestedScrollingEnabled(false);

        setupTacticSpinners();
        setupCrewSpinners();
        attachPreviewListeners();
        refreshThreatState();
        refreshTacticPreviews();
        updateButtonStates();

        btnLaunch.setOnClickListener(v -> startMissionProcess());
        btnCancel.setOnClickListener(v -> {
            missionLogAdapter.clear();
            missionLogAdapter.addEvent(new MissionEvent(MissionEvent.TYPE_INFO, "Mission Cancelled",
                    mainActivity.getMissionControl().cancelMission()));
            refreshThreatState();
            setupCrewSpinners();
            refreshTacticPreviews();
            updateButtonStates();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        setupCrewSpinners();
        refreshThreatState();
        refreshTacticPreviews();
        updateButtonStates();
        if (mainActivity.getMissionControl().hasActiveMission()) {
            resolveMissionProcess();
        }
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
        MissionControl missionControl = mainActivity.getMissionControl();
        if (missionControl.hasActiveMission()
                && missionControl.getActiveMemberA() != null
                && missionControl.getActiveMemberB() != null) {
            availableCrew = Collections.emptyList();
            bindCrewSpinner(spinnerCrew1, missionControl.getActiveMemberA(), false);
            bindCrewSpinner(spinnerCrew2, missionControl.getActiveMemberB(), false);
            syncLockedTacticSpinner(spinnerTactic1, missionControl.getTacticA());
            syncLockedTacticSpinner(spinnerTactic2, missionControl.getTacticB());
            return;
        }

        availableCrew = new ArrayList<>();
        for (CrewMember crewMember : mainActivity.getStorage().getAllCrew()) {
            if (CrewMember.LOCATION_MISSION_READY.equals(crewMember.getLocation())
                    && crewMember.isAvailableForMission()) {
                availableCrew.add(crewMember);
            }
        }

        List<String> crewLabels = new ArrayList<>();
        for (CrewMember crewMember : availableCrew) {
            crewLabels.add(buildCrewLabel(crewMember));
        }

        if (crewLabels.isEmpty()) {
            crewLabels = Collections.singletonList("No mission-ready crew");
        }

        ThemedSpinnerAdapter adapter = new ThemedSpinnerAdapter(requireContext(), crewLabels);
        spinnerCrew1.setAdapter(adapter);
        spinnerCrew2.setAdapter(adapter);
        spinnerCrew1.setEnabled(true);
        spinnerCrew2.setEnabled(true);
        spinnerCrew1.setAlpha(1f);
        spinnerCrew2.setAlpha(1f);
        spinnerTactic1.setEnabled(true);
        spinnerTactic2.setEnabled(true);
        spinnerTactic1.setAlpha(1f);
        spinnerTactic2.setAlpha(1f);
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
        String tactic1 = String.valueOf(spinnerTactic1.getSelectedItem());
        String tactic2 = String.valueOf(spinnerTactic2.getSelectedItem());
        showLaunchConfirmation(member1, member2, tactic1, tactic2);
    }

    /**
     * Passes the selected tactics to mission control and plays back the resulting mission replay.
     */
    private void resolveMissionProcess() {
        MissionControl missionControl = mainActivity.getMissionControl();
        if (!missionControl.hasActiveMission()) {
            return;
        }

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
            refreshTacticPreviews();
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
            refreshTacticPreviews();
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
     * Shows one fixed crew member in a spinner while a mission is active.
     */
    private void bindCrewSpinner(@NonNull Spinner spinner, @NonNull CrewMember crewMember,
            boolean enabled) {
        spinner.setAdapter(new ThemedSpinnerAdapter(requireContext(),
                Collections.singletonList(buildCrewLabel(crewMember))));
        spinner.setSelection(0);
        spinner.setEnabled(enabled);
        spinner.setAlpha(enabled ? 1f : 0.72f);
    }

    /**
     * Builds one consistent crew label for mission setup and active mission display.
     */
    @NonNull
    private String buildCrewLabel(@NonNull CrewMember crewMember) {
        return crewMember.getName() + " (" + crewMember.getSpecialization() + ")"
                + " | Lv." + crewMember.getLevel()
                + " | HP " + crewMember.getCurrentEnergy() + "/" + crewMember.getMaxEnergy();
    }

    /**
     * Opens a confirmation dialog, then immediately launches and resolves the mission on approval.
     */
    private void showLaunchConfirmation(@NonNull CrewMember member1, @NonNull CrewMember member2,
            @NonNull String tactic1, @NonNull String tactic2) {
        Threat threat = mainActivity.ensureThreatReady();
        if (threat == null) {
            showToast("There is no active threat to deploy against.");
            return;
        }

        String message = "Threat: " + threat.getName() + " [" + threat.getCategory() + " / "
                + threat.getArchetype() + "]\n"
                + member1.getName() + ": " + tactic1 + "\n"
                + member2.getName() + ": " + tactic2 + "\n\n"
                + "The mission will start and resolve immediately after confirmation.";

        new AlertDialog.Builder(requireContext())
                .setTitle("Start mission?")
                .setMessage(message)
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", (dialog, which) ->
                        executeMissionLaunch(member1, member2, tactic1, tactic2))
                .show();
    }

    /**
     * Launches the mission, applies tactics, and immediately plays the resulting replay.
     */
    private void executeMissionLaunch(@NonNull CrewMember member1, @NonNull CrewMember member2,
            @NonNull String tactic1, @NonNull String tactic2) {
        MissionControl missionControl = mainActivity.getMissionControl();
        String launchResult = missionControl.launchMission(member1, member2, mainActivity.getCurrentDay());
        if (!launchResult.startsWith("Mission launched")) {
            missionLogAdapter.clear();
            missionLogAdapter.addEvent(new MissionEvent(MissionEvent.TYPE_INFO, "Launch Blocked",
                    launchResult));
            showToast(launchResult);
            refreshThreatState();
            setupCrewSpinners();
            refreshTacticPreviews();
            updateButtonStates();
            scrollLogToBottom();
            return;
        }

        missionControl.setTactics(tactic1, tactic2);
        MissionResolution resolution = mainActivity.resolveActiveMission();
        MissionResolution decoratedResolution = prependLaunchEvent(resolution, member1, member2,
                tactic1, tactic2, launchResult);
        playResolution(decoratedResolution);
    }

    /**
     * Adds a launch event ahead of the combat replay so the player sees the confirmed deployment.
     */
    @NonNull
    private MissionResolution prependLaunchEvent(@NonNull MissionResolution resolution,
            @NonNull CrewMember member1, @NonNull CrewMember member2, @NonNull String tactic1,
            @NonNull String tactic2, @NonNull String launchResult) {
        List<MissionEvent> replayEvents = new ArrayList<>();
        replayEvents.add(new MissionEvent(MissionEvent.TYPE_INFO, "Mission Launch",
                launchResult + "\n"
                        + member1.getName() + ": " + tactic1 + "\n"
                        + member2.getName() + ": " + tactic2));
        replayEvents.addAll(resolution.getEvents());
        return new MissionResolution(resolution.isResolved(), resolution.isSuccess(),
                resolution.getResourceReward(), resolution.getExperienceReward(), replayEvents);
    }

    /**
     * Enables or disables mission buttons depending on mission state and game-over state.
     */
    private void updateButtonStates() {
        boolean hasActiveMission = mainActivity.getMissionControl().hasActiveMission();
        boolean canPlay = !mainActivity.isGameOver();
        btnLaunch.setEnabled(canPlay && !hasActiveMission && availableCrew.size() >= 2);
        btnCancel.setVisibility(canPlay && hasActiveMission ? View.VISIBLE : View.GONE);
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

    /**
     * Refreshes tactic previews whenever crew or tactic choices change.
     */
    private void attachPreviewListeners() {
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshTacticPreviews();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                refreshTacticPreviews();
            }
        };
        spinnerCrew1.setOnItemSelectedListener(listener);
        spinnerCrew2.setOnItemSelectedListener(listener);
        spinnerTactic1.setOnItemSelectedListener(listener);
        spinnerTactic2.setOnItemSelectedListener(listener);
    }

    /**
     * Updates both tactic preview cards based on the selected crew pair and current threat.
     */
    private void refreshTacticPreviews() {
        MissionControl missionControl = mainActivity.getMissionControl();
        if (mainActivity.isGameOver()) {
            bindPreviewCard(MissionTacticPreview.placeholder(mainActivity.getGameOverReason()),
                    tvPreviewCrew1, tvPreviewCrew1AttackTitle, tvPreviewCrew1AttackDetails,
                    tvPreviewCrew1DefendTitle, tvPreviewCrew1DefendDetails,
                    previewCrew1AttackCard, previewCrew1DefendCard, false, false);
            bindPreviewCard(MissionTacticPreview.placeholder(mainActivity.getGameOverReason()),
                    tvPreviewCrew2, tvPreviewCrew2AttackTitle, tvPreviewCrew2AttackDetails,
                    tvPreviewCrew2DefendTitle, tvPreviewCrew2DefendDetails,
                    previewCrew2AttackCard, previewCrew2DefendCard, false, false);
            return;
        }

        Threat threat = missionControl.getCurrentThreat();
        if (threat == null) {
            threat = mainActivity.ensureThreatReady();
        }

        CrewMember member1;
        CrewMember member2;
        if (missionControl.hasActiveMission()
                && missionControl.getActiveMemberA() != null
                && missionControl.getActiveMemberB() != null) {
            member1 = missionControl.getActiveMemberA();
            member2 = missionControl.getActiveMemberB();
        } else if (availableCrew.size() >= 2) {
            member1 = getSelectedCrew(spinnerCrew1);
            member2 = getSelectedCrew(spinnerCrew2);
        } else {
            MissionTacticPreview placeholder = MissionTacticPreview.placeholder(
                    "Select mission-ready crew to see tactic effects.");
            bindPreviewCard(placeholder, tvPreviewCrew1, tvPreviewCrew1AttackTitle,
                    tvPreviewCrew1AttackDetails, tvPreviewCrew1DefendTitle,
                    tvPreviewCrew1DefendDetails, previewCrew1AttackCard, previewCrew1DefendCard,
                    false, false);
            bindPreviewCard(placeholder, tvPreviewCrew2, tvPreviewCrew2AttackTitle,
                    tvPreviewCrew2AttackDetails, tvPreviewCrew2DefendTitle,
                    tvPreviewCrew2DefendDetails, previewCrew2AttackCard, previewCrew2DefendCard,
                    false, false);
            return;
        }

        MissionTacticPreview preview1 = missionControl.buildTacticPreview(member1, member2, threat);
        MissionTacticPreview preview2 = missionControl.buildTacticPreview(member2, member1, threat);
        bindPreviewCard(preview1, tvPreviewCrew1, tvPreviewCrew1AttackTitle,
                tvPreviewCrew1AttackDetails, tvPreviewCrew1DefendTitle, tvPreviewCrew1DefendDetails,
                previewCrew1AttackCard, previewCrew1DefendCard, isAttackSelected(spinnerTactic1),
                isDefendSelected(spinnerTactic1));
        bindPreviewCard(preview2, tvPreviewCrew2, tvPreviewCrew2AttackTitle,
                tvPreviewCrew2AttackDetails, tvPreviewCrew2DefendTitle, tvPreviewCrew2DefendDetails,
                previewCrew2AttackCard, previewCrew2DefendCard, isAttackSelected(spinnerTactic2),
                isDefendSelected(spinnerTactic2));
    }

    /**
     * Binds one preview model into one slot card and highlights the active tactic.
     */
    private void bindPreviewCard(@NonNull MissionTacticPreview preview, @NonNull TextView crewView,
            @NonNull TextView attackTitleView, @NonNull TextView attackDetailsView,
            @NonNull TextView defendTitleView, @NonNull TextView defendDetailsView,
            @NonNull View attackCard, @NonNull View defendCard, boolean attackSelected,
            boolean defendSelected) {
        crewView.setText(preview.getCrewLabel());
        attackTitleView.setText((attackSelected ? "ATTACK SELECTED - " : "ATTACK - ")
                + preview.getAttackOption().getTitle());
        attackDetailsView.setText(preview.getAttackOption().getSummaryText());
        defendTitleView.setText((defendSelected ? "DEFEND SELECTED - " : "DEFEND - ")
                + preview.getDefendOption().getTitle());
        defendDetailsView.setText(preview.getDefendOption().getSummaryText());

        stylePreviewOption(attackCard, attackSelected, true);
        stylePreviewOption(defendCard, defendSelected, false);

        float alpha = preview.isPlaceholder() ? 0.78f : 1f;
        crewView.setAlpha(alpha);
        attackDetailsView.setAlpha(alpha);
        defendDetailsView.setAlpha(alpha);
    }

    /**
     * Resolves the currently selected crew member from one spinner.
     */
    @Nullable
    private CrewMember getSelectedCrew(@NonNull Spinner spinner) {
        int index = spinner.getSelectedItemPosition();
        if (index < 0 || index >= availableCrew.size()) {
            return null;
        }
        return availableCrew.get(index);
    }

    /**
     * Applies locked tactic state during legacy active mission restores.
     */
    private void syncLockedTacticSpinner(@NonNull Spinner spinner, @NonNull String tactic) {
        spinner.setSelection(MissionControl.TACTIC_DEFEND.equals(tactic) ? 1 : 0);
        spinner.setEnabled(false);
        spinner.setAlpha(0.72f);
    }

    /**
     * @return {@code true} when the given spinner currently points to Attack
     */
    private boolean isAttackSelected(@NonNull Spinner spinner) {
        return MissionControl.TACTIC_ATTACK.equals(String.valueOf(spinner.getSelectedItem()));
    }

    /**
     * @return {@code true} when the given spinner currently points to Defend
     */
    private boolean isDefendSelected(@NonNull Spinner spinner) {
        return MissionControl.TACTIC_DEFEND.equals(String.valueOf(spinner.getSelectedItem()));
    }

    /**
     * Styles one tactic preview block for its current selected or idle state.
     */
    private void stylePreviewOption(@NonNull View view, boolean selected, boolean attackTone) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(22f);

        int fillColor;
        int strokeColor;
        if (selected) {
            fillColor = Color.parseColor(attackTone ? "#2F2316" : "#18283B");
            strokeColor = Color.parseColor(attackTone ? "#FFB74D" : "#64B5F6");
        } else {
            fillColor = Color.parseColor("#131A2A");
            strokeColor = Color.parseColor("#2F3A59");
        }

        drawable.setColor(fillColor);
        drawable.setStroke(2, strokeColor);
        view.setBackground(drawable);
    }
}
