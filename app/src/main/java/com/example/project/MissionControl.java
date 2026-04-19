package com.example.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Coordinates threat generation, mission lifecycle, and combat resolution.
 * <p>
 * Missions always resolve against the colony's currently active threat, and the resulting
 * structured event log is replayed by the UI step by step.
 */
public class MissionControl {

    /** Mission tactic constant for aggressive actions. */
    public static final String TACTIC_ATTACK = "Attack";
    /** Mission tactic constant for defensive actions. */
    public static final String TACTIC_DEFEND = "Defend";

    private static final int THREAT_DEADLINE_DAYS = 5;
    private static final int FAILURE_XP_REWARD = 50;

    private final Storage storage;
    private final StatisticsManager statisticsManager;
    private final Random random;

    private boolean activeMission;
    private CrewMember activeMemberA;
    private CrewMember activeMemberB;
    private Threat currentThreat;
    private int completedMissions;
    private String tacticA = TACTIC_ATTACK;
    private String tacticB = TACTIC_ATTACK;

    /**
     * Creates mission control for the current colony.
     *
     * @param storage crew storage used for mission participation and recovery
     * @param statisticsManager colony statistics tracker updated by mission outcomes
     */
    public MissionControl(Storage storage, StatisticsManager statisticsManager) {
        this.storage = storage;
        this.statisticsManager = statisticsManager;
        this.random = new Random();
        this.activeMission = false;
        this.completedMissions = 0;
    }

    /**
     * Ensures the colony always has one active threat to deal with.
     *
     * @param currentDay current colony day
     * @return existing or newly generated active threat
     */
    public Threat ensureActiveThreat(int currentDay) {
        if (currentThreat == null) {
            currentThreat = generateThreat(currentDay);
        }
        return currentThreat;
    }

    /**
     * Generates the next day-scaled threat.
     *
     * @param currentDay current colony day
     * @return randomly selected threat instance
     */
    public Threat generateThreat(int currentDay) {
        int dayBonus = Math.max(0, currentDay - 1);
        int missionBonus = Math.max(0, completedMissions / 2);

        int type = random.nextInt(7);
        switch (type) {
            case 0:
                return createThreat("Skybreaker Flock", Threat.CATEGORY_FLYING, "Interception",
                        8, 1, 26, 58, currentDay, dayBonus, missionBonus);
            case 1:
                return createThreat("Reactor Chain Failure", Threat.CATEGORY_TECHNICAL,
                        "Stabilization", 9, 2, 28, 62, currentDay, dayBonus, missionBonus);
            case 2:
                return createThreat("Spore Hive Bloom", Threat.CATEGORY_BIOLOGICAL, "Containment",
                        8, 2, 30, 60, currentDay, dayBonus, missionBonus);
            case 3:
                return createThreat("Marauder Boarding Party", Threat.CATEGORY_COMBAT, "Defense",
                        10, 1, 29, 68, currentDay, dayBonus, missionBonus);
            case 4:
                return createThreat("Meteor Tempest", Threat.CATEGORY_ENVIRONMENTAL, "Navigation",
                        9, 2, 31, 64, currentDay, dayBonus, missionBonus);
            case 5:
                return createThreat("Rift Echo", Threat.CATEGORY_ANOMALY, "Investigation",
                        10, 3, 27, 72, currentDay, dayBonus, missionBonus);
            default:
                return createThreat("Void Serpent", Threat.CATEGORY_FLYING, "Pursuit",
                        11, 2, 34, 80, currentDay, dayBonus, missionBonus);
        }
    }

    /**
     * Launches a mission using two prepared crew members.
     *
     * @param memberA first crew member
     * @param memberB second crew member
     * @param currentDay current colony day
     * @return launch confirmation or validation failure message
     */
    public String launchMission(CrewMember memberA, CrewMember memberB, int currentDay) {
        if (activeMission) {
            return "A mission is already active.";
        }
        if (memberA == null || memberB == null) {
            return "Select two crew members before launching a mission.";
        }
        if (memberA.getId() == memberB.getId()) {
            return "You must select two different crew members.";
        }
        if (!CrewMember.LOCATION_MISSION_READY.equals(memberA.getLocation())
                || !CrewMember.LOCATION_MISSION_READY.equals(memberB.getLocation())) {
            return "Move crew members to Mission Ready before launching.";
        }
        if (!memberA.isAvailableForMission() || !memberB.isAvailableForMission()) {
            return "Selected crew members are injured or still serving a mission penalty.";
        }

        Threat threat = ensureActiveThreat(currentDay);
        activeMemberA = memberA;
        activeMemberB = memberB;
        activeMission = true;
        tacticA = TACTIC_ATTACK;
        tacticB = TACTIC_ATTACK;

        activeMemberA.setLocation(CrewMember.LOCATION_ON_MISSION);
        activeMemberB.setLocation(CrewMember.LOCATION_ON_MISSION);

        return "Mission launched against " + threat.getName() + " [" + threat.getCategory()
                + " / " + threat.getArchetype() + "]\nDeadline: Day "
                + threat.getDeadlineDay() + "\nReward: " + threat.getResourceReward()
                + " resources";
    }

    /**
     * Sets the chosen tactics for the active mission.
     *
     * @param tacticA tactic for crew member A
     * @param tacticB tactic for crew member B
     */
    public void setTactics(String tacticA, String tacticB) {
        this.tacticA = tacticA == null ? TACTIC_ATTACK : tacticA;
        this.tacticB = tacticB == null ? TACTIC_ATTACK : tacticB;
    }

    /**
     * Cancels the currently active mission and returns crew members to Quarters.
     *
     * @return cancellation summary
     */
    public String cancelMission() {
        if (!activeMission) {
            return "No active mission to cancel.";
        }

        StringBuilder log = new StringBuilder("Mission cancelled. Crew returns to Quarters.\n");
        if (activeMemberA != null) {
            activeMemberA.setLocation(CrewMember.LOCATION_QUARTERS);
            log.append(activeMemberA.getName()).append(" returned to Quarters.\n");
        }
        if (activeMemberB != null) {
            activeMemberB.setLocation(CrewMember.LOCATION_QUARTERS);
            log.append(activeMemberB.getName()).append(" returned to Quarters.\n");
        }

        activeMission = false;
        activeMemberA = null;
        activeMemberB = null;
        tacticA = TACTIC_ATTACK;
        tacticB = TACTIC_ATTACK;
        return log.toString().trim();
    }

    /**
     * Resolves the active mission into a structured replay sequence.
     *
     * @return mission replay data and resulting rewards
     */
    public MissionResolution resolveMission() {
        if (!activeMission || currentThreat == null || activeMemberA == null || activeMemberB == null) {
            List<MissionEvent> events = new ArrayList<>();
            events.add(new MissionEvent(MissionEvent.TYPE_INFO, "No Active Mission",
                    "Launch a mission from prepared crew members first."));
            return new MissionResolution(false, false, 0, 0, events);
        }

        Threat threat = currentThreat;
        List<MissionEvent> events = new ArrayList<>();
        List<CrewMember> deployedCrew = getDeployedCrew();
        Set<Integer> newlyPenalizedIds = new HashSet<>();
        int successXpReward = threat.getExperienceReward() > 0 ? threat.getExperienceReward() : 100;

        events.add(new MissionEvent(MissionEvent.TYPE_INFO, "Mission Begins",
                threat.getName() + " [" + threat.getCategory() + " / " + threat.getArchetype()
                        + "]\nDeadline Day: " + threat.getDeadlineDay()
                        + "\nThreat HP: " + threat.getCurrentEnergy() + "/" + threat.getMaxEnergy()));
        events.add(new MissionEvent(MissionEvent.TYPE_INFO, "Crew Tactics",
                activeMemberA.getName() + ": " + tacticA + "\n"
                        + activeMemberB.getName() + ": " + tacticB));

        statisticsManager.recordMission();
        int round = 1;

        while (threat.getCurrentEnergy() > 0 && (activeMemberA != null || activeMemberB != null)) {
            events.add(new MissionEvent(MissionEvent.TYPE_INFO, "Round " + round,
                    "Threat HP " + threat.getCurrentEnergy() + "/" + threat.getMaxEnergy()));

            if (activeMemberA != null) {
                activeMemberA = resolveCrewTurn(activeMemberA, activeMemberB, tacticA, threat,
                        events, newlyPenalizedIds);
            }

            if (threat.getCurrentEnergy() <= 0 || (activeMemberA == null && activeMemberB == null)) {
                break;
            }

            if (activeMemberB != null) {
                activeMemberB = resolveCrewTurn(activeMemberB, activeMemberA, tacticB, threat,
                        events, newlyPenalizedIds);
            }

            round++;
        }

        boolean success = threat.getCurrentEnergy() <= 0;
        if (success) {
            statisticsManager.recordWin();
            completedMissions++;
            events.add(new MissionEvent(MissionEvent.TYPE_OUTCOME, "Threat Neutralized",
                    threat.getName() + " has been defeated. Survivors return to Quarters."));

            for (CrewMember member : deployedCrew) {
                member.gainExperience(successXpReward);
                if (!member.isInjured()) {
                    member.setLocation(CrewMember.LOCATION_QUARTERS);
                    member.restoreEnergy();
                }
                events.add(new MissionEvent(MissionEvent.TYPE_REWARD,
                        member.getName() + " gains " + successXpReward + " XP",
                        "Level " + member.getLevel() + " | HP " + member.getCurrentEnergy() + "/"
                                + member.getMaxEnergy()));
            }

            currentThreat = null;
        } else {
            statisticsManager.recordLoss();
            events.add(new MissionEvent(MissionEvent.TYPE_OUTCOME, "Mission Failed",
                    "The crew could not stop " + threat.getName() + ". The threat remains active at "
                            + threat.getCurrentEnergy() + "/" + threat.getMaxEnergy() + " HP."));

            for (CrewMember member : deployedCrew) {
                member.gainExperience(FAILURE_XP_REWARD);
                events.add(new MissionEvent(MissionEvent.TYPE_REWARD,
                        member.getName() + " gains " + FAILURE_XP_REWARD + " XP",
                        "Mission experience still improves the colony roster."));
            }
        }

        storage.decrementMissionPenaltiesExcluding(newlyPenalizedIds);
        activeMission = false;
        activeMemberA = null;
        activeMemberB = null;
        tacticA = TACTIC_ATTACK;
        tacticB = TACTIC_ATTACK;

        return new MissionResolution(true, success, success ? threat.getResourceReward() : 0,
                success ? successXpReward : FAILURE_XP_REWARD, events);
    }

    /**
     * Resolves one crew member turn and any retaliation that follows.
     *
     * @param actor crew member taking the turn
     * @param ally the other deployed crew member
     * @param tactic selected tactic for this crew member
     * @param threat active threat
     * @param events event list to append replay steps to
     * @param newlyPenalizedIds ids of crew knocked out during this mission
     * @return the surviving acting crew member, or {@code null} if knocked out
     */
    private CrewMember resolveCrewTurn(CrewMember actor, CrewMember ally, String tactic, Threat threat,
            List<MissionEvent> events, Set<Integer> newlyPenalizedIds) {
        if (TACTIC_DEFEND.equals(tactic)) {
            events.add(new MissionEvent(MissionEvent.TYPE_DEFENSE,
                    actor.getName() + " takes a defensive stance",
                    "Incoming damage this retaliation will be reduced."));
        } else {
            int rawAttack = computeCrewAttack(actor, ally, threat, events);
            int dealt = threat.takeDamage(rawAttack);
            events.add(new MissionEvent(MissionEvent.TYPE_ATTACK,
                    actor.getName() + " attacks " + threat.getName(),
                    "Dealt " + dealt + " damage. Threat HP: " + threat.getCurrentEnergy() + "/"
                            + threat.getMaxEnergy()));
        }

        if (threat.getCurrentEnergy() <= 0) {
            return actor;
        }

        int retaliation = computeThreatRetaliation(actor, ally, tactic, threat, events);
        int taken = actor.takeDamage(retaliation);
        events.add(new MissionEvent(MissionEvent.TYPE_DEFENSE,
                threat.getName() + " retaliates against " + actor.getName(),
                actor.getName() + " took " + taken + " damage and is now at "
                        + actor.getCurrentEnergy() + "/" + actor.getMaxEnergy() + " HP."));

        if (actor.getCurrentEnergy() == 0) {
            actor.assignMissionPenalty(1);
            newlyPenalizedIds.add(actor.getId());
            events.add(new MissionEvent(MissionEvent.TYPE_STATUS,
                    actor.getName() + " is knocked out",
                    "They were moved to Quarters, need recovery time, and must skip the next mission."));
            return null;
        }

        return actor;
    }

    /**
     * Calculates attack output with specialization and composition bonuses.
     */
    private int computeCrewAttack(CrewMember actor, CrewMember ally, Threat threat,
            List<MissionEvent> events) {
        int rawAttack = getBaseAttack(actor, threat);

        if (actor instanceof Pilot && Threat.CATEGORY_FLYING.equals(threat.getCategory())) {
            rawAttack += 3;
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Pilot Bonus",
                    actor.getName() + " exploits the flying target for +3 damage."));
        }
        if (actor instanceof Engineer && Threat.CATEGORY_TECHNICAL.equals(threat.getCategory())) {
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Engineer Bonus",
                    actor.getName() + " destabilizes the technical threat for bonus damage."));
        }
        if (actor instanceof Scientist && Threat.CATEGORY_ANOMALY.equals(threat.getCategory())) {
            rawAttack += 3;
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Scientist Bonus",
                    actor.getName() + " predicts the anomaly pattern for +3 damage."));
        }
        if (actor instanceof Soldier && Threat.CATEGORY_COMBAT.equals(threat.getCategory())) {
            rawAttack += 3;
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Soldier Bonus",
                    actor.getName() + " overwhelms the combat target for +3 damage."));
        }
        if (actor instanceof Medic && Threat.CATEGORY_BIOLOGICAL.equals(threat.getCategory())) {
            rawAttack += 1;
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Medic Bonus",
                    actor.getName() + " understands the biological threat for +1 damage."));
        }

        if (isPair(actor, ally, Pilot.class, Engineer.class)
                && (Threat.CATEGORY_FLYING.equals(threat.getCategory())
                || Threat.CATEGORY_TECHNICAL.equals(threat.getCategory()))) {
            rawAttack = (int) Math.round(rawAttack * 1.25d);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Composition Bonus",
                    "Pilot + Engineer coordination boosts damage by 25%."));
        }

        if (isPair(actor, ally, Medic.class, Scientist.class)
                && (Threat.CATEGORY_BIOLOGICAL.equals(threat.getCategory())
                || Threat.CATEGORY_ANOMALY.equals(threat.getCategory()))) {
            rawAttack = (int) Math.round(rawAttack * 1.15d);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Composition Bonus",
                    "Medic + Scientist analysis boosts damage by 15%."));
        }

        if (isPair(actor, ally, Soldier.class, Medic.class)
                && Threat.CATEGORY_COMBAT.equals(threat.getCategory())) {
            rawAttack += 2;
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Composition Bonus",
                    "Soldier + Medic frontline support adds +2 damage."));
        }

        return rawAttack;
    }

    /**
     * Calculates retaliation after tactic and composition mitigation.
     */
    private int computeThreatRetaliation(CrewMember actor, CrewMember ally, String tactic, Threat threat,
            List<MissionEvent> events) {
        int retaliation = threat.act();

        if (TACTIC_DEFEND.equals(tactic)) {
            retaliation = (int) Math.ceil(retaliation * 0.6d);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Defend Tactic",
                    actor.getName() + " braces for impact and reduces retaliation."));
        }

        if (actor instanceof Medic && Threat.CATEGORY_BIOLOGICAL.equals(threat.getCategory())) {
            retaliation = Math.max(0, retaliation - 2);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Medic Pressure Control",
                    actor.getName() + " reduces biological threat pressure by 2."));
        }

        if (isPair(actor, ally, Medic.class, Scientist.class)
                && (Threat.CATEGORY_BIOLOGICAL.equals(threat.getCategory())
                || Threat.CATEGORY_ANOMALY.equals(threat.getCategory()))) {
            retaliation = Math.max(0, retaliation - 2);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Composition Bonus",
                    "Medic + Scientist preparation cuts retaliation by 2."));
        }

        if (isPair(actor, ally, Soldier.class, Medic.class)
                && Threat.CATEGORY_COMBAT.equals(threat.getCategory())) {
            retaliation = Math.max(0, retaliation - 2);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Composition Bonus",
                    "Soldier + Medic sustain reduces combat retaliation by 2."));
        }

        return retaliation;
    }

    /**
     * Returns the specialization-adjusted base attack before mission bonuses.
     */
    private int getBaseAttack(CrewMember member, Threat threat) {
        if (member instanceof Engineer) {
            return ((Engineer) member).act(threat);
        }
        return member.act();
    }

    /**
     * Checks for unordered two-member composition bonuses.
     */
    private boolean isPair(CrewMember first, CrewMember second, Class<?> typeA, Class<?> typeB) {
        if (first == null || second == null) {
            return false;
        }
        return (typeA.isInstance(first) && typeB.isInstance(second))
                || (typeA.isInstance(second) && typeB.isInstance(first));
    }

    /**
     * Creates a scaled threat from a simple template.
     */
    private Threat createThreat(String name, String category, String archetype, int baseSkill,
            int baseResilience, int baseEnergy, int baseReward, int currentDay, int dayBonus,
            int missionBonus) {
        int skill = baseSkill + dayBonus + missionBonus;
        int resilience = baseResilience + (dayBonus / 2);
        int energy = baseEnergy + (dayBonus * 4) + (missionBonus * 3);
        int reward = baseReward + (dayBonus * 8) + (missionBonus * 4);
        return new Threat(name, category, archetype, skill, resilience, energy, reward, 100,
                currentDay, currentDay + THREAT_DEADLINE_DAYS - 1);
    }

    /**
     * @return deployed crew list at the moment resolution starts
     */
    private List<CrewMember> getDeployedCrew() {
        List<CrewMember> deployed = new ArrayList<>();
        if (activeMemberA != null) {
            deployed.add(activeMemberA);
        }
        if (activeMemberB != null) {
            deployed.add(activeMemberB);
        }
        return deployed;
    }

    /**
     * @param currentDay current colony day
     * @return {@code true} if the active threat has exceeded its deadline
     */
    public boolean isThreatOverdue(int currentDay) {
        return currentThreat != null && currentThreat.isOverdue(currentDay);
    }

    /**
     * Restores an active threat from persistence.
     *
     * @param currentThreat restored threat
     */
    public void setCurrentThreat(Threat currentThreat) {
        this.currentThreat = currentThreat;
    }

    /**
     * Restores active mission state from persistence.
     *
     * @param memberA first active crew member
     * @param memberB second active crew member
     * @param tacticA restored tactic for member A
     * @param tacticB restored tactic for member B
     */
    public void restoreActiveMission(CrewMember memberA, CrewMember memberB, String tacticA,
            String tacticB) {
        if (memberA == null || memberB == null || currentThreat == null) {
            activeMission = false;
            activeMemberA = null;
            activeMemberB = null;
            return;
        }

        this.activeMission = true;
        this.activeMemberA = memberA;
        this.activeMemberB = memberB;
        this.tacticA = tacticA == null ? TACTIC_ATTACK : tacticA;
        this.tacticB = tacticB == null ? TACTIC_ATTACK : tacticB;
    }

    /**
     * @return {@code true} if a mission is currently staged and awaiting resolution
     */
    public boolean hasActiveMission() {
        return activeMission;
    }

    /**
     * @return active threat or {@code null} when none is currently present
     */
    public Threat getCurrentThreat() {
        return currentThreat;
    }

    /**
     * @return first active mission participant
     */
    public CrewMember getActiveMemberA() {
        return activeMemberA;
    }

    /**
     * @return second active mission participant
     */
    public CrewMember getActiveMemberB() {
        return activeMemberB;
    }

    /**
     * @return tactic assigned to crew member A
     */
    public String getTacticA() {
        return tacticA;
    }

    /**
     * @return tactic assigned to crew member B
     */
    public String getTacticB() {
        return tacticB;
    }

    /**
     * @return number of completed successful missions
     */
    public int getCompletedMissions() {
        return completedMissions;
    }

    /**
     * Restores completed mission count from saved data.
     *
     * @param completedMissions saved mission count
     */
    public void setCompletedMissions(int completedMissions) {
        this.completedMissions = completedMissions;
    }

    /**
     * Sets a deterministic seed for threat generation during testing.
     *
     * @param seed random seed value
     */
    public void setRandomSeed(long seed) {
        random.setSeed(seed);
    }
}
