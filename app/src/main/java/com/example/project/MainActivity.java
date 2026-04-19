package com.example.project;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.List;

/**
 * Main activity for the Space Colony Manager application.
 * <p>
 * This activity owns the core game state and manager objects, handles tab navigation,
 * and persists the colony between app sessions.
 */
public class MainActivity extends AppCompatActivity {

    /** Core crew storage for the colony. */
    private Storage storage;
    /** Quarters manager used for rest and recovery actions. */
    private Quarters quarters;
    /** Simulator manager used for training actions. */
    private Simulator simulator;
    /** Statistics tracker for colony mission performance. */
    private StatisticsManager statisticsManager;
    /** Mission manager for launch, cancel, and resolve flows. */
    private MissionControl missionControl;
    /** Save/load helper for persistence. */
    private SaveLoadManager saveLoadManager;
    /** Current colony resource pool. */
    private int colonyResources;
    /** Current in-game day. */
    private int currentDay;

    /**
     * Creates the activity, initializes the game systems, restores saved state,
     * and wires bottom navigation to fragments.
     *
     * @param savedInstanceState previously saved instance state, if any
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeGameSystems();
        loadGame();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
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
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    /**
     * Saves the current game automatically whenever the app is paused.
     */
    @Override
    protected void onPause() {
        super.onPause();
        saveGame();
    }

    /**
     * Initializes all core managers and default colony values.
     */
    private void initializeGameSystems() {
        this.storage = new Storage();
        this.quarters = new Quarters();
        this.simulator = new Simulator();
        this.statisticsManager = new StatisticsManager();
        this.missionControl = new MissionControl(storage, statisticsManager);
        this.saveLoadManager = new SaveLoadManager();
        this.colonyResources = 150;
        this.currentDay = 1;
    }

    /**
     * Saves the current colony state into SharedPreferences.
     */
    private void saveGame() {
        SharedPreferences sp = getSharedPreferences("SpaceColonyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("crew_data", saveLoadManager.createJsonFromStorage(storage));
        editor.putInt("colony_resources", colonyResources);
        editor.putInt("current_day", currentDay);
        editor.putInt("total_missions", statisticsManager.getTotalMissions());
        editor.putInt("total_wins", statisticsManager.getTotalWins());
        editor.putInt("completed_missions", missionControl.getCompletedMissions());
        editor.apply();
    }

    /**
     * Loads any previously saved colony state from SharedPreferences.
     */
    private void loadGame() {
        SharedPreferences sp = getSharedPreferences("SpaceColonyPrefs", MODE_PRIVATE);
        String crewJson = sp.getString("crew_data", null);
        if (crewJson != null) {
            List<CrewMember> loadedCrew = saveLoadManager.loadStorageFromJson(crewJson);
            for (CrewMember c : loadedCrew) {
                storage.addCrewMember(c);
            }
        }
        colonyResources = sp.getInt("colony_resources", 150);
        currentDay = sp.getInt("current_day", 1);
        statisticsManager.setTotalMissions(sp.getInt("total_missions", 0));
        statisticsManager.setTotalWins(sp.getInt("total_wins", 0));
        missionControl.setCompletedMissions(sp.getInt("completed_missions", 0));
    }

    /**
     * @return colony crew storage
     */
    public Storage getStorage() { return storage; }

    /**
     * @return quarters manager
     */
    public Quarters getQuarters() { return quarters; }

    /**
     * @return simulator manager
     */
    public Simulator getSimulator() { return simulator; }

    /**
     * @return mission control manager
     */
    public MissionControl getMissionControl() { return missionControl; }

    /**
     * @return colony statistics manager
     */
    public StatisticsManager getStatisticsManager() { return statisticsManager; }

    /**
     * @return current colony resource total
     */
    public int getColonyResources() { return colonyResources; }

    /**
     * @return current in-game day
     */
    public int getCurrentDay() { return currentDay; }

    /**
     * Spends a fixed amount of colony resources.
     *
     * @param amount amount to subtract
     */
    public void spendResources(int amount) {
        this.colonyResources -= amount;
    }

    /**
     * Advances the in-game day counter by one.
     */
    public void incrementDay() {
        this.currentDay++;
    }
}
