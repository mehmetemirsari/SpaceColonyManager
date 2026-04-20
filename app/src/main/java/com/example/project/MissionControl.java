package com.example.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private static final int MAX_MISSION_ROUNDS = 12;
    private static final int MAX_PREVIEW_LINES = 3;

    private static final int PILOT_ATTACK_BONUS = 2;
    private static final int PILOT_ATTACK_GUARD = 2;
    private static final int PILOT_DEFEND_GUARD = 4;
    private static final int PILOT_DEFEND_ATTACK_BONUS = 2;
    private static final int PILOT_FLYING_BONUS = 3;

    private static final int ENGINEER_ATTACK_BONUS = 1;
    private static final int ENGINEER_ATTACK_SUPPRESSION = 2;
    private static final int ENGINEER_ATTACK_VULNERABILITY = 1;
    private static final int ENGINEER_DEFEND_GUARD = 3;
    private static final int ENGINEER_DEFEND_TEAM_GUARD = 2;
    private static final int ENGINEER_TECHNICAL_BONUS = 3;

    private static final int MEDIC_ATTACK_BONUS = 1;
    private static final int MEDIC_BIOLOGICAL_BONUS = 1;
    private static final int MEDIC_DEFEND_HEAL = 4;
    private static final int MEDIC_DEFEND_ATTACK_BONUS = 3;
    private static final int MEDIC_DEFEND_NEXT_STRIKE_HEAL = 2;
    private static final int MEDIC_DEFEND_GUARD = 2;
    private static final int MEDIC_BIOLOGICAL_PRESSURE_REDUCTION = 2;

    private static final int SCIENTIST_ATTACK_BONUS = 1;
    private static final int SCIENTIST_ATTACK_VULNERABILITY = 3;
    private static final int SCIENTIST_DEFEND_GUARD = 2;
    private static final int SCIENTIST_DEFEND_VULNERABILITY = 4;
    private static final int SCIENTIST_DEFEND_SUPPRESSION = 1;
    private static final int SCIENTIST_ANOMALY_BONUS = 3;

    private static final int SOLDIER_ATTACK_BONUS = 3;
    private static final int SOLDIER_ATTACK_GUARD = 1;
    private static final int SOLDIER_DEFEND_GUARD = 5;
    private static final int SOLDIER_DEFEND_COUNTER = 3;
    private static final int SOLDIER_DEFEND_TEAM_GUARD = 2;
    private static final int SOLDIER_COMBAT_BONUS = 3;

    private static final int DEFEND_RETALIATION_REDUCTION_PERCENT = 40;
    private static final int MEDIC_SCIENTIST_RETALIATION_REDUCTION = 2;
    private static final int SOLDIER_MEDIC_RETALIATION_REDUCTION = 2;
    private static final int SOLDIER_MEDIC_DAMAGE_BONUS = 2;
    private static final int PILOT_ENGINEER_DAMAGE_PERCENT = 25;
    private static final int MEDIC_SCIENTIST_DAMAGE_PERCENT = 15;
    private static final double PILOT_ENGINEER_DAMAGE_MULTIPLIER = 1.25d;
    private static final double MEDIC_SCIENTIST_DAMAGE_MULTIPLIER = 1.15d;

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
     * Builds a read-only tactic preview for the Mission screen.
     *
     * @param actor selected crew member for this slot
     * @param ally other selected crew member
     * @param threat active threat the squad is preparing for
     * @return preview payload for both tactics
     */
    public MissionTacticPreview buildTacticPreview(CrewMember actor, CrewMember ally, Threat threat) {
        if (actor == null || ally == null) {
            return MissionTacticPreview.placeholder("Select mission-ready crew to see tactic effects.");
        }
        if (threat == null) {
            return MissionTacticPreview.placeholder("An active threat is required before tactics can be previewed.");
        }

        return new MissionTacticPreview(actor.getName() + " (" + actor.getSpecialization() + ")",
                buildAttackPreview(actor, ally, threat), buildDefendPreview(actor, ally, threat), false);
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
        BattleState battleState = new BattleState();
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

        while (threat.getCurrentEnergy() > 0 && (activeMemberA != null || activeMemberB != null)
                && round <= MAX_MISSION_ROUNDS) {
            events.add(new MissionEvent(MissionEvent.TYPE_INFO, "Round " + round,
                    "Threat HP " + threat.getCurrentEnergy() + "/" + threat.getMaxEnergy()));

            if (activeMemberA != null) {
                activeMemberA = resolveCrewTurn(activeMemberA, activeMemberB, tacticA, threat,
                        events, newlyPenalizedIds, battleState);
            }

            if (threat.getCurrentEnergy() <= 0 || (activeMemberA == null && activeMemberB == null)) {
                break;
            }

            if (activeMemberB != null) {
                activeMemberB = resolveCrewTurn(activeMemberB, activeMemberA, tacticB, threat,
                        events, newlyPenalizedIds, battleState);
            }

            round++;
        }

        boolean success = threat.getCurrentEnergy() <= 0;
        boolean timedOut = !success && round > MAX_MISSION_ROUNDS;
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
            events.add(new MissionEvent(MissionEvent.TYPE_OUTCOME,
                    timedOut ? "Mission Stalled" : "Mission Failed",
                    timedOut
                            ? "The encounter dragged on too long. " + threat.getName()
                                    + " remains active at " + threat.getCurrentEnergy() + "/"
                                    + threat.getMaxEnergy() + " HP."
                            : "The crew could not stop " + threat.getName()
                                    + ". The threat remains active at "
                                    + threat.getCurrentEnergy() + "/" + threat.getMaxEnergy()
                                    + " HP."));

            for (CrewMember member : deployedCrew) {
                member.gainExperience(FAILURE_XP_REWARD);
                if (!member.isInjured()) {
                    member.setLocation(CrewMember.LOCATION_QUARTERS);
                }
                events.add(new MissionEvent(MissionEvent.TYPE_REWARD,
                        member.getName() + " gains " + FAILURE_XP_REWARD + " XP",
                        "Retreated with HP " + member.getCurrentEnergy() + "/" + member.getMaxEnergy()
                                + "."));
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
            List<MissionEvent> events, Set<Integer> newlyPenalizedIds, BattleState battleState) {
        if (TACTIC_DEFEND.equals(tactic)) {
            events.add(new MissionEvent(MissionEvent.TYPE_DEFENSE,
                    actor.getName() + " takes a defensive stance",
                    "Incoming damage this retaliation will be reduced."));
            applyDefendAbility(actor, ally, threat, battleState, events);
        } else {
            int rawAttack = computeCrewAttack(actor, ally, threat, battleState, events);
            int dealt = threat.takeDamage(rawAttack);
            events.add(new MissionEvent(MissionEvent.TYPE_ATTACK,
                    actor.getName() + " attacks " + threat.getName(),
                    "Dealt " + dealt + " damage. Threat HP: " + threat.getCurrentEnergy() + "/"
                            + threat.getMaxEnergy()));
            applyAttackRecovery(actor, ally, dealt, battleState, events);
        }

        if (threat.getCurrentEnergy() <= 0) {
            return actor;
        }

        int retaliation = computeThreatRetaliation(actor, ally, tactic, threat, battleState, events);
        int taken = actor.takeDamage(retaliation);
        events.add(new MissionEvent(MissionEvent.TYPE_DEFENSE,
                threat.getName() + " retaliates against " + actor.getName(),
                actor.getName() + " took " + taken + " damage and is now at "
                        + actor.getCurrentEnergy() + "/" + actor.getMaxEnergy() + " HP."));

        int counterDamage = battleState.consumeCounterDamage(actor);
        if (counterDamage > 0 && actor.getCurrentEnergy() > 0 && threat.getCurrentEnergy() > 0) {
            int reflected = threat.takeDamage(counterDamage);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Counterattack",
                    actor.getName() + " turns defense into offense and reflects " + reflected
                            + " damage back to " + threat.getName() + "."));
            if (threat.getCurrentEnergy() <= 0) {
                return actor;
            }
        }

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
            BattleState battleState, List<MissionEvent> events) {
        int rawAttack = getBaseAttack(actor, threat);
        int supportBonus = battleState.consumeAttackBonus(actor);
        if (supportBonus > 0) {
            rawAttack += supportBonus;
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Support Bonus",
                    actor.getName() + " receives +" + supportBonus
                            + " damage from allied support."));
        }

        int vulnerabilityBonus = battleState.consumeThreatVulnerability();
        if (vulnerabilityBonus > 0) {
            rawAttack += vulnerabilityBonus;
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Weak Point Exploited",
                    threat.getName() + " was exposed for +" + vulnerabilityBonus + " damage."));
        }

        rawAttack += applyAttackAbility(actor, ally, threat, battleState, events);

        if (hasPilotFlyingBonus(actor, threat)) {
            rawAttack += PILOT_FLYING_BONUS;
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Pilot Bonus",
                    actor.getName() + " exploits the flying target for +" + PILOT_FLYING_BONUS
                            + " damage."));
        }
        if (hasEngineerTechnicalBonus(actor, threat)) {
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Engineer Bonus",
                    actor.getName() + " destabilizes the technical threat for +"
                            + ENGINEER_TECHNICAL_BONUS + " damage."));
        }
        if (hasScientistAnomalyBonus(actor, threat)) {
            rawAttack += SCIENTIST_ANOMALY_BONUS;
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Scientist Bonus",
                    actor.getName() + " predicts the anomaly pattern for +"
                            + SCIENTIST_ANOMALY_BONUS + " damage."));
        }
        if (hasSoldierCombatBonus(actor, threat)) {
            rawAttack += SOLDIER_COMBAT_BONUS;
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Soldier Bonus",
                    actor.getName() + " overwhelms the combat target for +"
                            + SOLDIER_COMBAT_BONUS + " damage."));
        }
        if (hasMedicBiologicalBonus(actor, threat)) {
            rawAttack += MEDIC_BIOLOGICAL_BONUS;
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Medic Bonus",
                    actor.getName() + " understands the biological threat for +"
                            + MEDIC_BIOLOGICAL_BONUS + " damage."));
        }

        if (hasPilotEngineerDamageBoost(actor, ally, threat)) {
            rawAttack = (int) Math.round(rawAttack * PILOT_ENGINEER_DAMAGE_MULTIPLIER);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Composition Bonus",
                    "Pilot + Engineer coordination boosts damage by "
                            + PILOT_ENGINEER_DAMAGE_PERCENT + "%."));
        }

        if (hasMedicScientistDamageBoost(actor, ally, threat)) {
            rawAttack = (int) Math.round(rawAttack * MEDIC_SCIENTIST_DAMAGE_MULTIPLIER);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Composition Bonus",
                    "Medic + Scientist analysis boosts damage by "
                            + MEDIC_SCIENTIST_DAMAGE_PERCENT + "%."));
        }

        if (hasSoldierMedicDamageBoost(actor, ally, threat)) {
            rawAttack += SOLDIER_MEDIC_DAMAGE_BONUS;
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Composition Bonus",
                    "Soldier + Medic frontline support adds +" + SOLDIER_MEDIC_DAMAGE_BONUS
                            + " damage."));
        }

        return Math.max(1, rawAttack);
    }

    /**
     * Calculates retaliation after tactic and composition mitigation.
     */
    private int computeThreatRetaliation(CrewMember actor, CrewMember ally, String tactic, Threat threat,
            BattleState battleState, List<MissionEvent> events) {
        int retaliation = threat.act();
        int suppression = battleState.consumeThreatSuppression();
        if (suppression > 0) {
            retaliation = Math.max(0, retaliation - suppression);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Threat Disrupted",
                    threat.getName() + " loses " + suppression + " retaliation power."));
        }

        if (TACTIC_DEFEND.equals(tactic)) {
            retaliation = (int) Math.ceil(retaliation * 0.6d);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Defend Tactic",
                    actor.getName() + " braces for impact and reduces retaliation by about "
                            + DEFEND_RETALIATION_REDUCTION_PERCENT + "%."));
        }

        if (hasMedicBiologicalPressureControl(actor, threat)) {
            retaliation = Math.max(0, retaliation - MEDIC_BIOLOGICAL_PRESSURE_REDUCTION);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Medic Pressure Control",
                    actor.getName() + " reduces biological threat pressure by "
                            + MEDIC_BIOLOGICAL_PRESSURE_REDUCTION + "."));
        }

        if (hasMedicScientistRetaliationShield(actor, ally, threat)) {
            retaliation = Math.max(0, retaliation - MEDIC_SCIENTIST_RETALIATION_REDUCTION);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Composition Bonus",
                    "Medic + Scientist preparation cuts retaliation by "
                            + MEDIC_SCIENTIST_RETALIATION_REDUCTION + "."));
        }

        if (hasSoldierMedicRetaliationShield(actor, ally, threat)) {
            retaliation = Math.max(0, retaliation - SOLDIER_MEDIC_RETALIATION_REDUCTION);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Composition Bonus",
                    "Soldier + Medic sustain reduces combat retaliation by "
                            + SOLDIER_MEDIC_RETALIATION_REDUCTION + "."));
        }

        int barrierReduction = battleState.consumeDamageReduction(actor);
        if (barrierReduction > 0) {
            retaliation = Math.max(0, retaliation - barrierReduction);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Defense Bonus",
                    actor.getName() + " blocks " + barrierReduction + " retaliation damage."));
        }

        return retaliation;
    }

    /**
     * Applies role-specific attack traits and immediate combat setup.
     */
    private int applyAttackAbility(CrewMember actor, CrewMember ally, Threat threat,
            BattleState battleState, List<MissionEvent> events) {
        int bonus = 0;
        if (actor instanceof Pilot) {
            bonus += PILOT_ATTACK_BONUS;
            battleState.addDamageReduction(actor, PILOT_ATTACK_GUARD);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Pilot Attack",
                    actor.getName() + " performs a flyby strike for +" + PILOT_ATTACK_BONUS
                            + " damage and prepares an evasive retreat."));
        } else if (actor instanceof Engineer) {
            bonus += ENGINEER_ATTACK_BONUS;
            battleState.addThreatSuppression(ENGINEER_ATTACK_SUPPRESSION);
            battleState.addThreatVulnerability(ENGINEER_ATTACK_VULNERABILITY);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Engineer Attack",
                    actor.getName() + " sabotages the target, reducing retaliation by "
                            + ENGINEER_ATTACK_SUPPRESSION + " and exposing a weak point."));
        } else if (actor instanceof Medic) {
            bonus += MEDIC_ATTACK_BONUS;
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Medic Attack",
                    actor.getName() + " channels a siphon strike that can restore allied health."));
        } else if (actor instanceof Scientist) {
            bonus += SCIENTIST_ATTACK_BONUS;
            battleState.addThreatVulnerability(SCIENTIST_ATTACK_VULNERABILITY);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Scientist Attack",
                    actor.getName() + " marks the threat, granting +"
                            + SCIENTIST_ATTACK_VULNERABILITY + " damage to the next hit."));
        } else if (actor instanceof Soldier) {
            bonus += SOLDIER_ATTACK_BONUS;
            battleState.addDamageReduction(actor, SOLDIER_ATTACK_GUARD);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Soldier Attack",
                    actor.getName() + " delivers a heavy impact for +" + SOLDIER_ATTACK_BONUS
                            + " damage and stays braced for the counter."));
        }
        return bonus;
    }

    /**
     * Applies role-specific recovery and support that trigger after an attack lands.
     */
    private void applyAttackRecovery(CrewMember actor, CrewMember ally, int dealt,
            BattleState battleState, List<MissionEvent> events) {
        int queuedSupportHeal = battleState.consumeAttackHeal(actor);
        if (queuedSupportHeal > 0) {
            int supportedHeal = healCrew(actor, queuedSupportHeal);
            if (supportedHeal > 0) {
                events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Support Recovery",
                        actor.getName() + " restores " + supportedHeal
                                + " HP from a defensive support boost."));
            }
        }

        if (actor instanceof Medic && dealt > 0) {
            int selfHeal = healCrew(actor, Math.max(2, dealt / 3));
            int allyHeal = healCrew(ally, 2);
            if (selfHeal > 0 || allyHeal > 0) {
                events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Medic Lifesteal",
                        actor.getName() + " restores " + selfHeal + " HP and stabilizes the ally for "
                                + allyHeal + " HP."));
            }
        }
    }

    /**
     * Applies role-specific defend abilities before retaliation resolves.
     */
    private void applyDefendAbility(CrewMember actor, CrewMember ally, Threat threat,
            BattleState battleState, List<MissionEvent> events) {
        CrewMember supportedMember = ally != null ? ally : actor;
        if (actor instanceof Pilot) {
            battleState.addDamageReduction(actor, PILOT_DEFEND_GUARD);
            battleState.addAttackBonus(supportedMember, PILOT_DEFEND_ATTACK_BONUS);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Pilot Defend",
                    actor.getName() + " creates evasive cover, gaining " + PILOT_DEFEND_GUARD
                            + " defense and granting " + supportedMember.getName() + " +"
                            + PILOT_DEFEND_ATTACK_BONUS + " attack."));
        } else if (actor instanceof Engineer) {
            battleState.addDamageReduction(actor, ENGINEER_DEFEND_GUARD);
            battleState.addThreatSuppression(ENGINEER_ATTACK_SUPPRESSION);
            if (ally != null) {
                battleState.addDamageReduction(ally, ENGINEER_DEFEND_TEAM_GUARD);
            }
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Engineer Defend",
                    actor.getName() + " deploys a barrier rig, reducing retaliation and reinforcing the squad."));
        } else if (actor instanceof Medic) {
            int healed = healCrew(supportedMember, MEDIC_DEFEND_HEAL);
            battleState.addAttackBonus(supportedMember, MEDIC_DEFEND_ATTACK_BONUS);
            battleState.addAttackHeal(supportedMember, MEDIC_DEFEND_NEXT_STRIKE_HEAL);
            battleState.addDamageReduction(actor, MEDIC_DEFEND_GUARD);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Field Support",
                    actor.getName() + " restores " + healed + " HP to " + supportedMember.getName()
                            + " and grants them +" + MEDIC_DEFEND_ATTACK_BONUS
                            + " attack plus recovery on their next strike."));
        } else if (actor instanceof Scientist) {
            battleState.addDamageReduction(actor, SCIENTIST_DEFEND_GUARD);
            battleState.addThreatVulnerability(SCIENTIST_DEFEND_VULNERABILITY);
            battleState.addThreatSuppression(SCIENTIST_DEFEND_SUPPRESSION);
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Scientist Defend",
                    actor.getName() + " scans the threat, adding +"
                            + SCIENTIST_DEFEND_VULNERABILITY
                            + " vulnerability and reducing retaliation by "
                            + SCIENTIST_DEFEND_SUPPRESSION + "."));
        } else if (actor instanceof Soldier) {
            battleState.addDamageReduction(actor, SOLDIER_DEFEND_GUARD);
            battleState.addCounterDamage(actor, SOLDIER_DEFEND_COUNTER);
            if (ally != null) {
                battleState.addDamageReduction(ally, SOLDIER_DEFEND_TEAM_GUARD);
            }
            events.add(new MissionEvent(MissionEvent.TYPE_BONUS, "Soldier Defend",
                    actor.getName() + " anchors the front line, gains " + SOLDIER_DEFEND_GUARD
                            + " defense, and prepares a " + SOLDIER_DEFEND_COUNTER
                            + "-damage counter."));
        }
    }

    /**
     * Builds the attack tactic description shown in the Mission tab.
     */
    private MissionTacticPreview.TacticOption buildAttackPreview(CrewMember actor, CrewMember ally,
            Threat threat) {
        List<String> lines = new ArrayList<>();
        String title = "Attack";

        if (actor instanceof Pilot) {
            title = "Flyby Strike";
            addPreviewLine(lines, "Gain +" + PILOT_ATTACK_BONUS + " damage and +" + PILOT_ATTACK_GUARD
                    + " self-defense before the retaliation.");
        } else if (actor instanceof Engineer) {
            title = "Sabotage Burst";
            addPreviewLine(lines, "Gain +" + ENGINEER_ATTACK_BONUS + " damage, cut retaliation by "
                    + ENGINEER_ATTACK_SUPPRESSION + ", and expose a weak point.");
        } else if (actor instanceof Medic) {
            title = "Siphon Strike";
            addPreviewLine(lines, "Gain +" + MEDIC_ATTACK_BONUS
                    + " damage, heal on hit, and stabilize the ally.");
        } else if (actor instanceof Scientist) {
            title = "Weak Point Scan";
            addPreviewLine(lines, "Gain +" + SCIENTIST_ATTACK_BONUS + " damage and mark the target for +"
                    + SCIENTIST_ATTACK_VULNERABILITY + " damage on the next hit.");
        } else if (actor instanceof Soldier) {
            title = "Heavy Impact";
            addPreviewLine(lines, "Gain +" + SOLDIER_ATTACK_BONUS + " damage and +" + SOLDIER_ATTACK_GUARD
                    + " self-defense before the counterattack.");
        }

        if (hasPilotFlyingBonus(actor, threat)) {
            addPreviewLine(lines, "Flying target: +" + PILOT_FLYING_BONUS + " extra damage.");
        } else if (hasEngineerTechnicalBonus(actor, threat)) {
            addPreviewLine(lines, "Technical target: +" + ENGINEER_TECHNICAL_BONUS
                    + " engineering damage.");
        } else if (hasScientistAnomalyBonus(actor, threat)) {
            addPreviewLine(lines, "Anomaly target: +" + SCIENTIST_ANOMALY_BONUS
                    + " prediction damage.");
        } else if (hasSoldierCombatBonus(actor, threat)) {
            addPreviewLine(lines, "Combat target: +" + SOLDIER_COMBAT_BONUS + " extra damage.");
        } else if (hasMedicBiologicalBonus(actor, threat)) {
            addPreviewLine(lines, "Biological target: +" + MEDIC_BIOLOGICAL_BONUS + " bonus damage.");
        }

        if (hasPilotEngineerDamageBoost(actor, ally, threat)) {
            addPreviewLine(lines, "Pilot + Engineer: +" + PILOT_ENGINEER_DAMAGE_PERCENT
                    + "% damage on this threat.");
        } else if (hasMedicScientistDamageBoost(actor, ally, threat)) {
            addPreviewLine(lines, "Medic + Scientist: +" + MEDIC_SCIENTIST_DAMAGE_PERCENT
                    + "% damage on this threat.");
        } else if (hasSoldierMedicDamageBoost(actor, ally, threat)) {
            addPreviewLine(lines, "Soldier + Medic: +" + SOLDIER_MEDIC_DAMAGE_BONUS
                    + " frontline damage on this threat.");
        }

        return new MissionTacticPreview.TacticOption(title, lines);
    }

    /**
     * Builds the defend tactic description shown in the Mission tab.
     */
    private MissionTacticPreview.TacticOption buildDefendPreview(CrewMember actor, CrewMember ally,
            Threat threat) {
        List<String> lines = new ArrayList<>();
        String title = "Defend";

        addPreviewLine(lines, "Base Defend softens the next retaliation by about "
                + DEFEND_RETALIATION_REDUCTION_PERCENT + "%.");

        if (actor instanceof Pilot) {
            title = "Evasive Cover";
            addPreviewLine(lines, "Gain +" + PILOT_DEFEND_GUARD + " defense and grant the ally +"
                    + PILOT_DEFEND_ATTACK_BONUS + " attack.");
        } else if (actor instanceof Engineer) {
            title = "Barrier Rig";
            addPreviewLine(lines, "Gain +" + ENGINEER_DEFEND_GUARD + " defense, shield the ally for +"
                    + ENGINEER_DEFEND_TEAM_GUARD + ", and reduce retaliation.");
        } else if (actor instanceof Medic) {
            title = "Field Support";
            addPreviewLine(lines, "Heal the ally for " + MEDIC_DEFEND_HEAL + ", grant +"
                    + MEDIC_DEFEND_ATTACK_BONUS + " attack, and restore "
                    + MEDIC_DEFEND_NEXT_STRIKE_HEAL + " HP on their next strike.");
        } else if (actor instanceof Scientist) {
            title = "Threat Scan";
            addPreviewLine(lines, "Gain +" + SCIENTIST_DEFEND_GUARD + " defense, add +"
                    + SCIENTIST_DEFEND_VULNERABILITY + " vulnerability, and cut retaliation by "
                    + SCIENTIST_DEFEND_SUPPRESSION + ".");
        } else if (actor instanceof Soldier) {
            title = "Frontline Guard";
            addPreviewLine(lines, "Gain +" + SOLDIER_DEFEND_GUARD + " defense, prepare a "
                    + SOLDIER_DEFEND_COUNTER + "-damage counter, and shield the ally for +"
                    + SOLDIER_DEFEND_TEAM_GUARD + ".");
        }

        if (hasMedicBiologicalPressureControl(actor, threat)) {
            addPreviewLine(lines, "Biological target: reduce retaliation by "
                    + MEDIC_BIOLOGICAL_PRESSURE_REDUCTION + ".");
        } else if (hasMedicScientistRetaliationShield(actor, ally, threat)) {
            addPreviewLine(lines, "Medic + Scientist: reduce retaliation by "
                    + MEDIC_SCIENTIST_RETALIATION_REDUCTION + " on this threat.");
        } else if (hasSoldierMedicRetaliationShield(actor, ally, threat)) {
            addPreviewLine(lines, "Soldier + Medic: reduce combat retaliation by "
                    + SOLDIER_MEDIC_RETALIATION_REDUCTION + ".");
        }

        return new MissionTacticPreview.TacticOption(title, lines);
    }

    /**
     * Adds one preview line while keeping the Mission UI compact.
     */
    private void addPreviewLine(List<String> lines, String line) {
        if (line == null || line.isEmpty() || lines.size() >= MAX_PREVIEW_LINES) {
            return;
        }
        lines.add(line);
    }

    /**
     * @return {@code true} when the selected crew member gets the flying target bonus
     */
    private boolean hasPilotFlyingBonus(CrewMember actor, Threat threat) {
        return actor instanceof Pilot && threat != null
                && Threat.CATEGORY_FLYING.equals(threat.getCategory());
    }

    /**
     * @return {@code true} when the selected engineer gets the technical target bonus
     */
    private boolean hasEngineerTechnicalBonus(CrewMember actor, Threat threat) {
        return actor instanceof Engineer && threat != null
                && Threat.CATEGORY_TECHNICAL.equals(threat.getCategory());
    }

    /**
     * @return {@code true} when the selected scientist gets the anomaly target bonus
     */
    private boolean hasScientistAnomalyBonus(CrewMember actor, Threat threat) {
        return actor instanceof Scientist && threat != null
                && Threat.CATEGORY_ANOMALY.equals(threat.getCategory());
    }

    /**
     * @return {@code true} when the selected soldier gets the combat target bonus
     */
    private boolean hasSoldierCombatBonus(CrewMember actor, Threat threat) {
        return actor instanceof Soldier && threat != null
                && Threat.CATEGORY_COMBAT.equals(threat.getCategory());
    }

    /**
     * @return {@code true} when the selected medic gets the biological target bonus
     */
    private boolean hasMedicBiologicalBonus(CrewMember actor, Threat threat) {
        return actor instanceof Medic && threat != null
                && Threat.CATEGORY_BIOLOGICAL.equals(threat.getCategory());
    }

    /**
     * @return {@code true} when the selected medic also reduces biological retaliation
     */
    private boolean hasMedicBiologicalPressureControl(CrewMember actor, Threat threat) {
        return hasMedicBiologicalBonus(actor, threat);
    }

    /**
     * @return {@code true} when Pilot + Engineer earns the damage multiplier
     */
    private boolean hasPilotEngineerDamageBoost(CrewMember actor, CrewMember ally, Threat threat) {
        return threat != null && isPair(actor, ally, Pilot.class, Engineer.class)
                && (Threat.CATEGORY_FLYING.equals(threat.getCategory())
                || Threat.CATEGORY_TECHNICAL.equals(threat.getCategory()));
    }

    /**
     * @return {@code true} when Medic + Scientist earns the shared damage bonus
     */
    private boolean hasMedicScientistDamageBoost(CrewMember actor, CrewMember ally, Threat threat) {
        return threat != null && isPair(actor, ally, Medic.class, Scientist.class)
                && (Threat.CATEGORY_BIOLOGICAL.equals(threat.getCategory())
                || Threat.CATEGORY_ANOMALY.equals(threat.getCategory()));
    }

    /**
     * @return {@code true} when Medic + Scientist reduces retaliation together
     */
    private boolean hasMedicScientistRetaliationShield(CrewMember actor, CrewMember ally,
            Threat threat) {
        return hasMedicScientistDamageBoost(actor, ally, threat);
    }

    /**
     * @return {@code true} when Soldier + Medic earns the combat damage bonus
     */
    private boolean hasSoldierMedicDamageBoost(CrewMember actor, CrewMember ally, Threat threat) {
        return threat != null && isPair(actor, ally, Soldier.class, Medic.class)
                && Threat.CATEGORY_COMBAT.equals(threat.getCategory());
    }

    /**
     * @return {@code true} when Soldier + Medic reduces combat retaliation
     */
    private boolean hasSoldierMedicRetaliationShield(CrewMember actor, CrewMember ally,
            Threat threat) {
        return hasSoldierMedicDamageBoost(actor, ally, threat);
    }

    /**
     * Restores health to a living crew member without exceeding maximum HP.
     */
    private int healCrew(CrewMember member, int amount) {
        if (member == null || amount <= 0 || member.getCurrentEnergy() <= 0) {
            return 0;
        }

        int previousEnergy = member.getCurrentEnergy();
        member.setCurrentEnergy(previousEnergy + amount);
        return member.getCurrentEnergy() - previousEnergy;
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

    /**
     * Tracks temporary combat bonuses for the current mission resolution only.
     */
    private static final class BattleState {
        private final Map<Integer, Integer> attackBonuses = new HashMap<>();
        private final Map<Integer, Integer> damageReductions = new HashMap<>();
        private final Map<Integer, Integer> attackHeals = new HashMap<>();
        private final Map<Integer, Integer> counterDamages = new HashMap<>();
        private int threatVulnerability;
        private int threatSuppression;

        void addAttackBonus(CrewMember member, int amount) {
            mergeBonus(attackBonuses, member, amount);
        }

        int consumeAttackBonus(CrewMember member) {
            return consumeBonus(attackBonuses, member);
        }

        void addDamageReduction(CrewMember member, int amount) {
            mergeBonus(damageReductions, member, amount);
        }

        int consumeDamageReduction(CrewMember member) {
            return consumeBonus(damageReductions, member);
        }

        void addAttackHeal(CrewMember member, int amount) {
            mergeBonus(attackHeals, member, amount);
        }

        int consumeAttackHeal(CrewMember member) {
            return consumeBonus(attackHeals, member);
        }

        void addCounterDamage(CrewMember member, int amount) {
            mergeBonus(counterDamages, member, amount);
        }

        int consumeCounterDamage(CrewMember member) {
            return consumeBonus(counterDamages, member);
        }

        void addThreatVulnerability(int amount) {
            threatVulnerability += Math.max(0, amount);
        }

        int consumeThreatVulnerability() {
            int value = threatVulnerability;
            threatVulnerability = 0;
            return value;
        }

        void addThreatSuppression(int amount) {
            threatSuppression += Math.max(0, amount);
        }

        int consumeThreatSuppression() {
            int value = threatSuppression;
            threatSuppression = 0;
            return value;
        }

        private void mergeBonus(Map<Integer, Integer> bonuses, CrewMember member, int amount) {
            if (member == null || amount <= 0) {
                return;
            }
            bonuses.put(member.getId(), bonuses.getOrDefault(member.getId(), 0) + amount);
        }

        private int consumeBonus(Map<Integer, Integer> bonuses, CrewMember member) {
            if (member == null) {
                return 0;
            }
            Integer value = bonuses.remove(member.getId());
            return value == null ? 0 : value;
        }
    }
}
