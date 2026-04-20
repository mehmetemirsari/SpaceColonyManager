package com.example.project;

import java.util.Locale;

/**
 * Tracks colony-wide mission statistics shown in the Statistics screen.
 */
public class StatisticsManager {
    private int totalMissions;
    private int totalWins;

    /**
     * Creates an empty statistics tracker.
     */
    public StatisticsManager() {
        this.totalMissions = 0;
        this.totalWins = 0;
    }

    /**
     * Records that the colony started or attempted one mission.
     */
    public void recordMission() {
        totalMissions++;
    }

    /**
     * Records a successful mission outcome.
     */
    public void recordWin() {
        totalWins++;
    }

    /**
     * Records a failed mission outcome.
     * <p>
     * Losses are derived implicitly from total missions minus total wins.
     */
    public void recordLoss() {
        // Loss is tracked implicitly via totalMissions - totalWins
    }

    /**
     * Builds a formatted summary for display in the statistics UI.
     *
     * @return formatted colony statistics text block
     */
    public String getStats() {
        int totalLosses = totalMissions - totalWins;
        double winRate = totalMissions == 0 ? 0 : ((double) totalWins / totalMissions) * 100;

        return "=== COLONY STATISTICS ===\n"
                + "Total Missions: " + totalMissions + "\n"
                + "Wins: " + totalWins + "\n"
                + "Losses: " + totalLosses + "\n"
                + "Win Rate: " + String.format(Locale.getDefault(), "%.1f", winRate) + "%";
    }

    /**
     * @return total attempted missions
     */
    public int getTotalMissions() { return totalMissions; }

    /**
     * @return total successful missions
     */
    public int getTotalWins() { return totalWins; }

    /**
     * Restores total mission count from saved data.
     *
     * @param totalMissions saved mission count
     */
    public void setTotalMissions(int totalMissions) { this.totalMissions = totalMissions; }

    /**
     * Restores total win count from saved data.
     *
     * @param totalWins saved win count
     */
    public void setTotalWins(int totalWins) { this.totalWins = totalWins; }
}
