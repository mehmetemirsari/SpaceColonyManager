package com.example.project;

import java.util.Random;

/**
 * Coordinates mission lifecycle and combat resolution.
 * <p>
 * This class launches missions, generates threats, resolves turn-based combat, applies rewards,
 * and updates colony-level statistics.
 */
public class MissionControl {
    private boolean isCancelled;
    private CrewMember activeMemberA;
    private CrewMember activeMemberB;
    private Threat currentThreat;
    private Storage storage;
    private StatisticsManager statisticsManager;
    private int completedMissions;
    private String tacticA = "Attack";
    private String tacticB = "Attack";

    /**
     * Creates mission control for the current colony.
     *
     * @param storage crew storage used for mission participation and defeat removal
     * @param statisticsManager colony statistics tracker updated by mission outcomes
     */
    public MissionControl(Storage storage, StatisticsManager statisticsManager) {
        this.isCancelled = false;
        this.storage = storage;
        this.statisticsManager = statisticsManager;
        this.completedMissions = 0;
    }

    /**
     * Launches a mission using two healthy crew members.
     *
     * @param a first crew member
     * @param b second crew member
     * @return mission start log text or validation failure message
     */
    public String launchMission(CrewMember a, CrewMember b) {
        if (a.isInjured() || b.isInjured()) {
            return "Injured crew members cannot go on a mission!";
        }
        if (a.getId() == b.getId()) {
            return "You must select two different crew members!";
        }

        this.activeMemberA = a;
        this.activeMemberB = b;
        this.isCancelled = false;
        this.tacticA = "Attack";
        this.tacticB = "Attack";

        a.setLocation("MissionControl");
        b.setLocation("MissionControl");

        this.currentThreat = generateThreat();
        return "Mission started! Threat encountered: " + currentThreat.getName()
                + " [" + currentThreat.getThreatType() + "]"
                + "\nThreat — skill: " + currentThreat.getSkill()
                + ", resilience: " + currentThreat.getResilience()
                + ", energy: " + currentThreat.getEnergy() + "/" + currentThreat.getMaxEnergy();
    }

    /**
     * Sets the chosen tactics for the active mission.
     *
     * @param tacticA tactic for crew member A
     * @param tacticB tactic for crew member B
     */
    public void setTactics(String tacticA, String tacticB) {
        this.tacticA = tacticA == null ? "Attack" : tacticA;
        this.tacticB = tacticB == null ? "Attack" : tacticB;
    }

    /**
     * Generates the next mission threat.
     * <p>
     * Threat energy and skill scale gradually with the number of completed missions.
     *
     * @return randomly selected threat instance
     */
    public Threat generateThreat() {
        Random rand = new Random();
        int type = rand.nextInt(3);
        int difficultyBonus = completedMissions;

        switch (type) {
            case 0:
                return new Threat("Asteroid Storm", 5 + difficultyBonus, 2, 22 + difficultyBonus * 4, "Environmental");
            case 1:
                return new Threat("Alien Parasite", 7 + difficultyBonus, 1, 18 + difficultyBonus * 4, "Biological");
            default:
                return new Threat("System Failure", 4 + difficultyBonus, 3, 26 + difficultyBonus * 4, "Technical");
        }
    }

    /**
     * Cancels the currently active mission and returns surviving crew members to Quarters.
     *
     * @return cancellation log text
     */
    public String cancelMission() {
        this.isCancelled = true;
        StringBuilder log = new StringBuilder("Mission cancelled! Crew returning to Quarters.\n");

        if (activeMemberA != null) {
            activeMemberA.setLocation("Quarters");
            activeMemberA.restoreEnergy();
            log.append(activeMemberA.getName()).append(" returned to Quarters with full energy restored.\n");
        }
        if (activeMemberB != null) {
            activeMemberB.setLocation("Quarters");
            activeMemberB.restoreEnergy();
            log.append(activeMemberB.getName()).append(" returned to Quarters with full energy restored.\n");
        }

        activeMemberA = null;
        activeMemberB = null;
        currentThreat = null;
        return log.toString();
    }

    /**
     * Resolves the active mission using a turn-based combat loop.
     * <p>
     * On success, surviving crew members gain 100 experience and return to Quarters.
     * Defeated crew members are removed from storage.
     *
     * @return full mission log
     */
    public String resolveMission() {
        if (currentThreat == null || activeMemberA == null || activeMemberB == null) {
            return "No active mission. Please launch a mission first.";
        }
        if (isCancelled) {
            return "Mission was cancelled.";
        }

        StringBuilder log = new StringBuilder();
        log.append("=== MISSION: ").append(currentThreat.getName()).append(" ===\n");
        log.append("Threat — skill: ").append(currentThreat.getSkill())
                .append(", energy: ").append(currentThreat.getEnergy())
                .append("/").append(currentThreat.getMaxEnergy()).append("\n");
        log.append(activeMemberA.getName()).append(" tactic: ").append(tacticA).append("\n");
        log.append(activeMemberB.getName()).append(" tactic: ").append(tacticB).append("\n\n");

        statisticsManager.recordMission();

        int round = 1;

        while (currentThreat.getEnergy() > 0 && (activeMemberA != null || activeMemberB != null)) {
            if (isCancelled) break;

            log.append("--- Round ").append(round).append(" ---\n");

            if (activeMemberA != null && !activeMemberA.isInjured()) {
                if ("Defend".equals(tacticA)) {
                    log.append(activeMemberA.getName()).append(" defends and prepares to reduce incoming damage.\n");
                } else {
                    int attack = getAttack(activeMemberA);
                    int before = currentThreat.getEnergy();
                    currentThreat.defend(attack);
                    int dealt = before - currentThreat.getEnergy();
                    log.append(activeMemberA.getName()).append(" attacks → dealt ").append(dealt)
                            .append(" dmg | Threat energy: ").append(currentThreat.getEnergy())
                            .append("/").append(currentThreat.getMaxEnergy()).append("\n");
                }

                if (currentThreat.getEnergy() <= 0) break;

                int threatDmg = currentThreat.act();
                if ("Defend".equals(tacticA)) {
                    threatDmg = Math.max(0, threatDmg / 2);
                }
                int aEnergyBefore = activeMemberA.getCurrentEnergy();
                activeMemberA.defend(threatDmg);
                int aDealt = aEnergyBefore - activeMemberA.getCurrentEnergy();
                log.append("Threat retaliates on ").append(activeMemberA.getName())
                        .append(" → dealt ").append(aDealt)
                        .append(" dmg | ").append(activeMemberA.getName())
                        .append(" energy: ").append(activeMemberA.getCurrentEnergy())
                        .append("/").append(activeMemberA.getMaxEnergy()).append("\n");

                if (activeMemberA.isInjured()) {
                    log.append(activeMemberA.getName()).append(" has been defeated and removed from the crew!\n");
                    storage.removeCrewMember(activeMemberA.getId());
                    activeMemberA = null;
                }
            }

            if (currentThreat.getEnergy() <= 0) break;

            if (activeMemberB != null && !activeMemberB.isInjured()) {
                if ("Defend".equals(tacticB)) {
                    log.append(activeMemberB.getName()).append(" defends and prepares to reduce incoming damage.\n");
                } else {
                    int attack = getAttack(activeMemberB);
                    int before = currentThreat.getEnergy();
                    currentThreat.defend(attack);
                    int dealt = before - currentThreat.getEnergy();
                    log.append(activeMemberB.getName()).append(" attacks → dealt ").append(dealt)
                            .append(" dmg | Threat energy: ").append(currentThreat.getEnergy())
                            .append("/").append(currentThreat.getMaxEnergy()).append("\n");
                }

                if (currentThreat.getEnergy() <= 0) break;

                int threatDmg = currentThreat.act();
                if ("Defend".equals(tacticB)) {
                    threatDmg = Math.max(0, threatDmg / 2);
                }
                int bEnergyBefore = activeMemberB.getCurrentEnergy();
                activeMemberB.defend(threatDmg);
                int bDealt = bEnergyBefore - activeMemberB.getCurrentEnergy();
                log.append("Threat retaliates on ").append(activeMemberB.getName())
                        .append(" → dealt ").append(bDealt)
                        .append(" dmg | ").append(activeMemberB.getName())
                        .append(" energy: ").append(activeMemberB.getCurrentEnergy())
                        .append("/").append(activeMemberB.getMaxEnergy()).append("\n");

                if (activeMemberB.isInjured()) {
                    log.append(activeMemberB.getName()).append(" has been defeated and removed from the crew!\n");
                    storage.removeCrewMember(activeMemberB.getId());
                    activeMemberB = null;
                }
            }

            round++;
            log.append("\n");
        }

        if (currentThreat.getEnergy() <= 0) {
            log.append("=== MISSION COMPLETE ===\n");
            log.append("The ").append(currentThreat.getName()).append(" has been neutralized!\n");

            if (activeMemberA != null) {
                activeMemberA.addExperience(100);
                activeMemberA.setLocation("Quarters");
                activeMemberA.restoreEnergy();
                log.append(activeMemberA.getName()).append(" gains 100 XP and returns to Quarters with full energy restored. (exp: ")
                        .append(activeMemberA.getExperience()).append(")\n");
            }
            if (activeMemberB != null) {
                activeMemberB.addExperience(100);
                activeMemberB.setLocation("Quarters");
                activeMemberB.restoreEnergy();
                log.append(activeMemberB.getName()).append(" gains 100 XP and returns to Quarters with full energy restored. (exp: ")
                        .append(activeMemberB.getExperience()).append(")\n");
            }
            statisticsManager.recordWin();
            completedMissions++;
        } else {
            log.append("=== MISSION FAILED ===\n");
            log.append("All crew members have been defeated.\n");
            statisticsManager.recordLoss();
        }

        activeMemberA = null;
        activeMemberB = null;
        currentThreat = null;
        return log.toString();
    }

    /**
     * Resolves the attack logic for a crew member, including engineer threat bonuses.
     *
     * @param member acting crew member
     * @return attack value for this turn
     */
    private int getAttack(CrewMember member) {
        if (member instanceof Engineer && currentThreat != null) {
            return ((Engineer) member).act(currentThreat);
        }
        return member.act();
    }

    /**
     * @return {@code true} if the mission has been cancelled
     */
    public boolean isCancelled() { return isCancelled; }

    /**
     * @return active threat or {@code null} when no mission is running
     */
    public Threat getCurrentThreat() { return currentThreat; }

    /**
     * @return number of completed missions
     */
    public int getCompletedMissions() { return completedMissions; }

    /**
     * Restores completed mission count from saved data.
     *
     * @param count saved completed mission count
     */
    public void setCompletedMissions(int count) { this.completedMissions = count; }
}
