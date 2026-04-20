package com.example.project;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import java.util.Random;

/**
 * Main activity for the Space Colony Manager application.
 * <p>
 * This activity owns the colony state, persists the game between launches, manages menu music,
 * and exposes shared gameplay actions to the fragment layer.
 */
public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "SpaceColonyPrefs";
    private static final String MANUAL_SAVE_PREFIX = "manual_";
    private static final String KEY_SAVE_PRESENT = "save_present";
    private static final String KEY_CREW_DATA = "crew_data";
    private static final String KEY_COLONY_RESOURCES = "colony_resources";
    private static final String KEY_CURRENT_DAY = "current_day";
    private static final String KEY_TOTAL_MISSIONS = "total_missions";
    private static final String KEY_TOTAL_WINS = "total_wins";
    private static final String KEY_COMPLETED_MISSIONS = "completed_missions";
    private static final String KEY_GAME_OVER = "game_over";
    private static final String KEY_GAME_OVER_REASON = "game_over_reason";
    private static final String KEY_THREAT_DATA = "threat_data";
    private static final String KEY_ACTIVE_MISSION = "active_mission";
    private static final String KEY_ACTIVE_MEMBER_A = "active_member_a";
    private static final String KEY_ACTIVE_MEMBER_B = "active_member_b";
    private static final String KEY_TACTIC_A = "tactic_a";
    private static final String KEY_TACTIC_B = "tactic_b";

    private Storage storage;
    private Quarters quarters;
    private Simulator simulator;
    private StatisticsManager statisticsManager;
    private MissionControl missionControl;
    private SaveLoadManager saveLoadManager;
    private MusicManager musicManager;
    private Random random;
    private BottomNavigationView bottomNav;

    private int colonyResources;
    private int currentDay;
    private boolean gameOver;
    private String gameOverReason;
    private final OnBackPressedCallback gameOverBackCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            // Game-over screen is intentionally non-dismissible.
        }
    };

    /**
     * Creates the activity, restores colony state, and wires bottom navigation.
     *
     * @param savedInstanceState previously saved instance state, if any
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeGameSystems();
        loadGame();
        if (!gameOver) {
            ensureThreatReady();
        }

        bottomNav = findViewById(R.id.bottom_navigation);
        getOnBackPressedDispatcher().addCallback(this, gameOverBackCallback);
        bottomNav.setOnItemSelectedListener(item -> {
            if (gameOver) {
                showGameOverScreenIfNeeded();
                return false;
            }

            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_recruit) {
                selectedFragment = new RecruitFragment();
            } else if (itemId == R.id.nav_quarters) {
                selectedFragment = new QuartersFragment();
            } else if (itemId == R.id.nav_simulator) {
                selectedFragment = new SimulatorFragment();
            } else if (itemId == R.id.nav_mission) {
                selectedFragment = new MissionControlFragment();
            } else if (itemId == R.id.nav_statistics) {
                selectedFragment = new StatisticsFragment();
            }

            if (selectedFragment != null) {
                showPrimaryFragment(selectedFragment);
            }
            return true;
        });

        updateGameOverUi();
        if (gameOver) {
            showGameOverScreenIfNeeded();
        } else if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    /**
     * Resumes menu music when the activity returns to the foreground.
     */
    @Override
    protected void onResume() {
        super.onResume();
        musicManager.resume();
    }

    /**
     * Persists the colony state and pauses menu music.
     */
    @Override
    protected void onPause() {
        super.onPause();
        saveGame();
        musicManager.pause();
    }

    /**
     * Releases audio resources when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        musicManager.release();
    }

    /**
     * Initializes all core managers and default colony values.
     */
    private void initializeGameSystems() {
        if (musicManager == null) {
            musicManager = new MusicManager(this);
        }
        resetRuntimeState();
    }

    /**
     * Saves the current colony state into SharedPreferences.
     */
    private void saveGame() {
        saveGameToSlot("");
    }

    /**
     * Saves the current colony state into the requested preference slot.
     *
     * @param prefix key prefix for the target save slot
     */
    private void saveGameToSlot(@NonNull String prefix) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(prefix + KEY_SAVE_PRESENT, true);
        editor.putString(prefix + KEY_CREW_DATA, saveLoadManager.createJsonFromStorage(storage));
        editor.putInt(prefix + KEY_COLONY_RESOURCES, colonyResources);
        editor.putInt(prefix + KEY_CURRENT_DAY, currentDay);
        editor.putInt(prefix + KEY_TOTAL_MISSIONS, statisticsManager.getTotalMissions());
        editor.putInt(prefix + KEY_TOTAL_WINS, statisticsManager.getTotalWins());
        editor.putInt(prefix + KEY_COMPLETED_MISSIONS, missionControl.getCompletedMissions());
        editor.putBoolean(prefix + KEY_GAME_OVER, gameOver);
        editor.putString(prefix + KEY_GAME_OVER_REASON, gameOverReason);
        editor.putString(prefix + KEY_THREAT_DATA,
                saveLoadManager.createJsonFromThreat(missionControl.getCurrentThreat()));

        if (missionControl.hasActiveMission()
                && missionControl.getActiveMemberA() != null
                && missionControl.getActiveMemberB() != null) {
            editor.putBoolean(prefix + KEY_ACTIVE_MISSION, true);
            editor.putInt(prefix + KEY_ACTIVE_MEMBER_A, missionControl.getActiveMemberA().getId());
            editor.putInt(prefix + KEY_ACTIVE_MEMBER_B, missionControl.getActiveMemberB().getId());
            editor.putString(prefix + KEY_TACTIC_A, missionControl.getTacticA());
            editor.putString(prefix + KEY_TACTIC_B, missionControl.getTacticB());
        } else {
            editor.putBoolean(prefix + KEY_ACTIVE_MISSION, false);
            editor.remove(prefix + KEY_ACTIVE_MEMBER_A);
            editor.remove(prefix + KEY_ACTIVE_MEMBER_B);
            editor.remove(prefix + KEY_TACTIC_A);
            editor.remove(prefix + KEY_TACTIC_B);
        }

        musicManager.writePreferences(editor, prefix);
        editor.apply();
    }

    /**
     * Loads any previously saved colony state from SharedPreferences.
     */
    private void loadGame() {
        loadGameFromSlot("");
    }

    /**
     * Loads colony state from the requested preference slot.
     *
     * @param prefix key prefix for the target save slot
     * @return {@code true} when a save was restored
     */
    private boolean loadGameFromSlot(@NonNull String prefix) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!hasSaveData(sharedPreferences, prefix)) {
            return false;
        }

        String crewJson = sharedPreferences.getString(prefix + KEY_CREW_DATA, null);
        List<CrewMember> loadedCrew;
        Threat savedThreat;
        int savedResources;
        int savedDay;
        int savedTotalMissions;
        int savedTotalWins;
        int savedCompletedMissions;
        boolean savedGameOver;
        String savedGameOverReason;
        boolean savedActiveMission;
        int savedMemberAId;
        int savedMemberBId;
        String savedTacticA;
        String savedTacticB;

        try {
            loadedCrew = crewJson == null ? new java.util.ArrayList<>()
                    : saveLoadManager.loadStorageFromJson(crewJson);
            savedThreat = saveLoadManager.loadThreatFromJson(
                    sharedPreferences.getString(prefix + KEY_THREAT_DATA, null));
            savedResources = sharedPreferences.getInt(prefix + KEY_COLONY_RESOURCES, 150);
            savedDay = sharedPreferences.getInt(prefix + KEY_CURRENT_DAY, 1);
            savedTotalMissions = sharedPreferences.getInt(prefix + KEY_TOTAL_MISSIONS, 0);
            savedTotalWins = sharedPreferences.getInt(prefix + KEY_TOTAL_WINS, 0);
            savedCompletedMissions = sharedPreferences.getInt(prefix + KEY_COMPLETED_MISSIONS, 0);
            savedGameOver = sharedPreferences.getBoolean(prefix + KEY_GAME_OVER, false);
            savedGameOverReason = sharedPreferences.getString(prefix + KEY_GAME_OVER_REASON, "");
            savedActiveMission = sharedPreferences.getBoolean(prefix + KEY_ACTIVE_MISSION, false);
            savedMemberAId = sharedPreferences.getInt(prefix + KEY_ACTIVE_MEMBER_A, -1);
            savedMemberBId = sharedPreferences.getInt(prefix + KEY_ACTIVE_MEMBER_B, -1);
            savedTacticA = sharedPreferences.getString(prefix + KEY_TACTIC_A,
                    MissionControl.TACTIC_ATTACK);
            savedTacticB = sharedPreferences.getString(prefix + KEY_TACTIC_B,
                    MissionControl.TACTIC_ATTACK);
        } catch (RuntimeException exception) {
            return false;
        }

        resetRuntimeState();

        for (CrewMember crewMember : loadedCrew) {
            storage.addCrewMember(crewMember);
        }

        colonyResources = savedResources;
        currentDay = savedDay;
        statisticsManager.setTotalMissions(savedTotalMissions);
        statisticsManager.setTotalWins(savedTotalWins);
        missionControl.setCompletedMissions(savedCompletedMissions);
        gameOver = savedGameOver;
        gameOverReason = savedGameOverReason == null ? "" : savedGameOverReason;
        musicManager.restorePreferences(sharedPreferences, prefix);
        missionControl.setCurrentThreat(savedThreat);

        if (savedActiveMission) {
            CrewMember memberA = storage.getCrewMember(savedMemberAId);
            CrewMember memberB = storage.getCrewMember(savedMemberBId);
            missionControl.restoreActiveMission(memberA, memberB, savedTacticA, savedTacticB);
        }

        updateGameOverUi();
        return true;
    }

    /**
     * Advances the colony to the next in-game day and applies passive recovery.
     *
     * @param reason brief gameplay summary used in the UI log
     * @return user-facing day advancement summary
     */
    public String advanceDay(@NonNull String reason) {
        if (gameOver) {
            return gameOverReason;
        }

        currentDay++;
        storage.advanceRecoveryDay();

        StringBuilder summary = new StringBuilder();
        summary.append("Day ").append(currentDay).append(": ").append(reason);

        if (missionControl.isThreatOverdue(currentDay)) {
            Threat overdueThreat = missionControl.getCurrentThreat();
            triggerGameOver("Day " + currentDay + ": " + overdueThreat.getName()
                    + " remained active for too long. The colony has fallen.");
            summary.append("\n").append(gameOverReason);
            return summary.toString();
        }

        boolean spawnedThreat = missionControl.getCurrentThreat() == null;
        Threat threat = ensureThreatReady();
        if (spawnedThreat && threat != null) {
            summary.append("\nNew threat detected: ")
                    .append(threat.getName())
                    .append(" [")
                    .append(threat.getCategory())
                    .append(" / ")
                    .append(threat.getArchetype())
                    .append("], deadline Day ")
                    .append(threat.getDeadlineDay())
                    .append(".");
        }

        return summary.toString();
    }

    /**
     * Trains one crew member and consumes one in-game day on success.
     *
     * @param member crew member selected in the Simulator
     * @return user-facing training result
     */
    public String trainCrewMember(CrewMember member) {
        if (gameOver) {
            return gameOverReason;
        }
        if (missionControl.hasActiveMission()) {
            return "Resolve or cancel the active mission before training the colony.";
        }
        if (!simulator.trainCrew(member)) {
            return member.getName() + " could not train right now.";
        }
        return advanceDay(member.getName() + " completed simulator drills and gained 25 XP.");
    }

    /**
     * Applies a colony-wide recovery day in Quarters.
     *
     * @return user-facing rest result
     */
    public String restAllCrew() {
        if (gameOver) {
            return gameOverReason;
        }
        if (missionControl.hasActiveMission()) {
            return "Resolve or cancel the active mission before resting the whole colony.";
        }
        if (!quarters.hasCrewToRest(storage)) {
            return storage.getCrewCount() == 0
                    ? "There are no crew members to recover yet."
                    : "There are no crew members in Quarters to recover right now.";
        }

        quarters.restAll(storage);
        return advanceDay("The colony spent the day recovering in Quarters.");
    }

    /**
     * Resolves the active mission, applies rewards, and advances the day.
     *
     * @return structured mission replay result
     */
    public MissionResolution resolveActiveMission() {
        if (gameOver) {
            List<MissionEvent> events = new java.util.ArrayList<>();
            events.add(new MissionEvent(MissionEvent.TYPE_OUTCOME, "Colony Lost", gameOverReason));
            return new MissionResolution(false, false, 0, 0, events);
        }

        MissionResolution resolution = missionControl.resolveMission();
        if (!resolution.isResolved()) {
            return resolution;
        }

        if (resolution.isSuccess() && resolution.getResourceReward() > 0) {
            addResources(resolution.getResourceReward());
            resolution.addEvent(new MissionEvent(MissionEvent.TYPE_REWARD, "Colony Reward",
                    "The colony gained " + resolution.getResourceReward() + " resources."));
        }

        resolution.addEvent(new MissionEvent(MissionEvent.TYPE_INFO, "Day Advanced",
                advanceDay(resolution.isSuccess()
                        ? "A mission concluded in the colony's favor."
                        : "The mission attempt ended without defeating the threat.")));

        if (gameOver) {
            resolution.addEvent(new MissionEvent(MissionEvent.TYPE_OUTCOME, "Colony Lost",
                    gameOverReason));
        }

        return resolution;
    }

    /**
     * Requests a supply drop and spends a day to receive it.
     *
     * @return user-facing supply drop result
     */
    public String requestSupplyDrop() {
        if (gameOver) {
            return gameOverReason;
        }
        if (missionControl.hasActiveMission()) {
            return "Resolve or cancel the active mission before requesting a supply drop.";
        }

        int resourceGain = 30 + random.nextInt(31) + (currentDay * 3);
        addResources(resourceGain);
        return advanceDay("A supply drop delivered +" + resourceGain + " resources.");
    }

    /**
     * Ensures one colony threat always exists unless the game has already ended.
     *
     * @return active threat, or {@code null} after game over
     */
    public Threat ensureThreatReady() {
        if (gameOver) {
            return missionControl.getCurrentThreat();
        }
        return missionControl.ensureActiveThreat(currentDay);
    }

    /**
     * Marks the colony as defeated.
     *
     * @param reason visible game-over reason
     */
    public void triggerGameOver(@NonNull String reason) {
        gameOver = true;
        gameOverReason = reason;
        updateGameOverUi();
    }

    /**
     * Opens an auxiliary fragment from the Home screen.
     *
     * @param fragment destination fragment
     */
    public void openAuxiliaryFragment(@NonNull Fragment fragment) {
        if (gameOver) {
            showGameOverScreenIfNeeded();
            return;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Opens the full-screen colony loss UI when needed.
     */
    public void showGameOverScreenIfNeeded() {
        updateGameOverUi();
        if (!gameOver) {
            return;
        }

        clearFragmentBackStack();
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof GameOverFragment) {
            return;
        }

        showPrimaryFragment(new GameOverFragment());
    }

    /**
     * Stores a manual save snapshot that can be restored later from Settings or the game-over
     * screen.
     *
     * @return user-facing save result
     */
    @NonNull
    public String createManualSave() {
        saveGameToSlot(MANUAL_SAVE_PREFIX);
        return "Manual save stored for Day " + currentDay + ".";
    }

    /**
     * Restores the manual save slot when it exists.
     *
     * @return user-facing load result
     */
    @NonNull
    public String loadManualSave() {
        if (!hasManualSave()) {
            return "No manual save was found yet.";
        }
        if (!loadGameFromSlot(MANUAL_SAVE_PREFIX)) {
            return "Manual save data was invalid and could not be restored.";
        }

        if (!gameOver) {
            ensureThreatReady();
            saveGame();
            showHomeScreen();
            return "Manual save loaded. Colony restored to Day " + currentDay + ".";
        }

        saveGame();
        showGameOverScreenIfNeeded();
        return "Manual save loaded, but that save was already in a collapsed state.";
    }

    /**
     * Starts a fresh colony run and returns the user to Home.
     *
     * @return user-facing reset result
     */
    @NonNull
    public String resetColony() {
        resetRuntimeState();
        ensureThreatReady();
        saveGame();
        showHomeScreen();
        return "The colony has been reset to a fresh start.";
    }

    /**
     * @return whether a manual save slot currently exists
     */
    public boolean hasManualSave() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getBoolean(MANUAL_SAVE_PREFIX + KEY_SAVE_PRESENT, false);
    }

    /**
     * @return colony crew storage
     */
    public Storage getStorage() {
        return storage;
    }

    /**
     * @return quarters manager
     */
    public Quarters getQuarters() {
        return quarters;
    }

    /**
     * @return simulator manager
     */
    public Simulator getSimulator() {
        return simulator;
    }

    /**
     * @return mission manager
     */
    public MissionControl getMissionControl() {
        return missionControl;
    }

    /**
     * @return colony statistics manager
     */
    public StatisticsManager getStatisticsManager() {
        return statisticsManager;
    }

    /**
     * @return menu music manager
     */
    public MusicManager getMusicManager() {
        return musicManager;
    }

    /**
     * @return current colony resource total
     */
    public int getColonyResources() {
        return colonyResources;
    }

    /**
     * @return current in-game day
     */
    public int getCurrentDay() {
        return currentDay;
    }

    /**
     * @return whether the colony has already been defeated
     */
    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * @return visible game-over reason
     */
    public String getGameOverReason() {
        return gameOverReason;
    }

    /**
     * Spends a fixed amount of colony resources.
     *
     * @param amount amount to subtract
     */
    public void spendResources(int amount) {
        colonyResources = Math.max(0, colonyResources - amount);
    }

    /**
     * Adds a fixed amount of colony resources.
     *
     * @param amount amount to add
     */
    public void addResources(int amount) {
        colonyResources += Math.max(0, amount);
    }

    /**
     * Recreates all mutable runtime state to its starting values.
     */
    private void resetRuntimeState() {
        storage = new Storage();
        quarters = new Quarters();
        simulator = new Simulator();
        statisticsManager = new StatisticsManager();
        missionControl = new MissionControl(storage, statisticsManager);
        saveLoadManager = new SaveLoadManager();
        random = new Random();
        colonyResources = 150;
        currentDay = 1;
        gameOver = false;
        gameOverReason = "";
        updateGameOverUi();
    }

    /**
     * Replaces the main content fragment without adding a back-stack entry.
     *
     * @param fragment fragment to show
     */
    private void showPrimaryFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /**
     * Removes any stacked auxiliary fragments before a hard navigation change.
     */
    private void clearFragmentBackStack() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    /**
     * Updates bottom-navigation visibility and back navigation rules to match the current
     * colony state.
     */
    private void updateGameOverUi() {
        gameOverBackCallback.setEnabled(gameOver);
        if (bottomNav != null) {
            bottomNav.setVisibility(gameOver ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Returns to the Home screen and restores bottom navigation after resets or manual loads.
     */
    private void showHomeScreen() {
        updateGameOverUi();
        clearFragmentBackStack();
        showPrimaryFragment(new HomeFragment());
        if (bottomNav != null) {
            bottomNav.getMenu().findItem(R.id.nav_home).setChecked(true);
        }
    }

    /**
     * Checks whether a preference slot contains enough data to attempt a restore.
     */
    private boolean hasSaveData(@NonNull SharedPreferences sharedPreferences, @NonNull String prefix) {
        return sharedPreferences.getBoolean(prefix + KEY_SAVE_PRESENT, false)
                || sharedPreferences.contains(prefix + KEY_CREW_DATA)
                || sharedPreferences.contains(prefix + KEY_CURRENT_DAY)
                || sharedPreferences.contains(prefix + KEY_THREAT_DATA);
    }
}
