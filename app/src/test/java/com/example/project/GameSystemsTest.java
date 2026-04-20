package com.example.project;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests covering the most important game system rules.
 */
public class GameSystemsTest {

    /**
     * Verifies that storage refuses to add a sixth crew member once the colony reaches its cap.
     */
    @Test
    public void storage_enforcesCrewCapOfFive() {
        Storage storage = new Storage();

        assertTrue(storage.addCrewMember(new Pilot(1, "P1")));
        assertTrue(storage.addCrewMember(new Engineer(2, "E1")));
        assertTrue(storage.addCrewMember(new Medic(3, "M1")));
        assertTrue(storage.addCrewMember(new Scientist(4, "S1")));
        assertTrue(storage.addCrewMember(new Soldier(5, "So1")));
        assertFalse(storage.addCrewMember(new Pilot(6, "P2")));

        assertEquals(5, storage.getCrewCount());
    }

    /**
     * Verifies that valid simulator training grants XP and consumes HP.
     */
    @Test
    public void simulator_training_addsExperienceAndCostsEnergy() {
        CrewMember pilot = new Pilot(1, "Nova");
        pilot.setLocation(CrewMember.LOCATION_SIMULATOR);
        int startEnergy = pilot.getCurrentEnergy();

        Simulator simulator = new Simulator();
        boolean trained = simulator.trainCrew(pilot);

        assertTrue(trained);
        assertEquals(25, pilot.getExperience());
        assertEquals(startEnergy - 10, pilot.getCurrentEnergy());
    }

    /**
     * Verifies that level thresholds increase both damage and maximum HP.
     */
    @Test
    public void crew_levelsIncreaseDamageAndHp() {
        CrewMember pilot = new Pilot(1, "Nova");

        assertEquals(1, pilot.getLevel());
        assertEquals(5, pilot.getSkill());
        assertEquals(20, pilot.getMaxEnergy());

        pilot.gainExperience(100);

        assertEquals(2, pilot.getLevel());
        assertEquals(6, pilot.getSkill());
        assertEquals(22, pilot.getMaxEnergy());
        assertEquals(22, pilot.getCurrentEnergy());
    }

    /**
     * Verifies that the save/load layer preserves progression and penalty fields.
     */
    @Test
    public void saveLoad_roundTripPreservesProgressionState() {
        Storage storage = new Storage();
        Pilot pilot = new Pilot(1, "Nova");
        pilot.gainExperience(150);
        pilot.setCurrentEnergy(12);
        pilot.setMissionPenaltyRemaining(1);
        pilot.setLocation(CrewMember.LOCATION_QUARTERS);
        storage.addCrewMember(pilot);

        SaveLoadManager saveLoadManager = new SaveLoadManager();
        String json = saveLoadManager.createJsonFromStorage(storage);
        List<CrewMember> restored = saveLoadManager.loadStorageFromJson(json);

        assertEquals(1, restored.size());
        CrewMember loaded = restored.get(0);
        assertEquals(150, loaded.getExperience());
        assertEquals(2, loaded.getLevel());
        assertEquals(12, loaded.getCurrentEnergy());
        assertEquals(1, loaded.getMissionPenaltyRemaining());
        assertEquals(CrewMember.LOCATION_QUARTERS, loaded.getLocation());
    }

    /**
     * Verifies that malformed save rows are skipped instead of crashing the load pass.
     */
    @Test
    public void saveLoad_skipsMalformedCrewRows() {
        SaveLoadManager saveLoadManager = new SaveLoadManager();
        String saveData = "1\tNova\tPilot\t20\t100\tfalse\tQuarters\t0\n"
                + "broken\trow\tthat\tshould\tbe\tskipped\tentirely\t!";

        List<CrewMember> restored = saveLoadManager.loadStorageFromJson(saveData);

        assertEquals(1, restored.size());
        assertEquals("Nova", restored.get(0).getName());
        assertEquals("Pilot", restored.get(0).getSpecialization());
    }

    /**
     * Verifies that malformed threat save data fails safely.
     */
    @Test
    public void saveLoad_returnsNullForMalformedThreatData() {
        SaveLoadManager saveLoadManager = new SaveLoadManager();

        Threat restored = saveLoadManager.loadThreatFromJson(
                "Bad\tThreat\tData\tnot-a-number\t1\t2\t3\t4\t5\t6\t7");

        assertNull(restored);
    }

    /**
     * Verifies that successful missions reward XP, resources, and clear the active threat.
     */
    @Test
    public void missionControl_successRewardsCrewAndClearsThreat() {
        Storage storage = new Storage();
        StatisticsManager statisticsManager = new StatisticsManager();
        MissionControl missionControl = new MissionControl(storage, statisticsManager);

        CrewMember alpha = new OverpoweredCrew(1, "Atlas");
        CrewMember beta = new OverpoweredCrew(2, "Vera");
        alpha.setLocation(CrewMember.LOCATION_MISSION_READY);
        beta.setLocation(CrewMember.LOCATION_MISSION_READY);
        storage.addCrewMember(alpha);
        storage.addCrewMember(beta);

        missionControl.setCurrentThreat(new Threat("Test Drone", Threat.CATEGORY_TECHNICAL,
                "Stabilization", 4, 1, 12, 45, 100, 1, 3));

        String launch = missionControl.launchMission(alpha, beta, 1);
        assertTrue(launch.contains("Mission launched"));

        MissionResolution resolution = missionControl.resolveMission();

        assertTrue(resolution.isResolved());
        assertTrue(resolution.isSuccess());
        assertEquals(45, resolution.getResourceReward());
        assertEquals(100, alpha.getExperience());
        assertEquals(100, beta.getExperience());
        assertNull(missionControl.getCurrentThreat());
        assertEquals(1, statisticsManager.getTotalWins());
        assertEquals(1, missionControl.getCompletedMissions());
    }

    /**
     * Verifies that failed missions preserve the threat and knock crew back to Quarters with a penalty.
     */
    @Test
    public void missionControl_failureLeavesThreatActiveAndAppliesPenalty() {
        Storage storage = new Storage();
        StatisticsManager statisticsManager = new StatisticsManager();
        MissionControl missionControl = new MissionControl(storage, statisticsManager);

        CrewMember alpha = new WeakCrew(1, "A");
        CrewMember beta = new WeakCrew(2, "B");
        alpha.setLocation(CrewMember.LOCATION_MISSION_READY);
        beta.setLocation(CrewMember.LOCATION_MISSION_READY);
        storage.addCrewMember(alpha);
        storage.addCrewMember(beta);

        missionControl.setCurrentThreat(new Threat("Titan", Threat.CATEGORY_COMBAT,
                "Defense", 12, 3, 40, 60, 100, 1, 3));

        missionControl.launchMission(alpha, beta, 1);
        MissionResolution resolution = missionControl.resolveMission();

        assertTrue(resolution.isResolved());
        assertFalse(resolution.isSuccess());
        assertNotNull(missionControl.getCurrentThreat());
        assertEquals(50, alpha.getExperience());
        assertEquals(50, beta.getExperience());
        assertEquals(CrewMember.LOCATION_QUARTERS, alpha.getLocation());
        assertEquals(1, alpha.getMissionPenaltyRemaining());
        assertTrue(alpha.isInjured());
    }

    /**
     * Verifies that failed missions send surviving crew members back to Quarters instead of
     * leaving them stranded on mission.
     */
    @Test
    public void missionControl_failureReturnsHealthyCrewToQuarters() {
        Storage storage = new Storage();
        StatisticsManager statisticsManager = new StatisticsManager();
        MissionControl missionControl = new MissionControl(storage, statisticsManager);

        CrewMember alpha = new DurableCrew(1, "Anchor");
        CrewMember beta = new DurableCrew(2, "Bulwark");
        alpha.setLocation(CrewMember.LOCATION_MISSION_READY);
        beta.setLocation(CrewMember.LOCATION_MISSION_READY);
        storage.addCrewMember(alpha);
        storage.addCrewMember(beta);

        missionControl.setCurrentThreat(new Threat("Fortress Bloom", Threat.CATEGORY_ENVIRONMENTAL,
                "Siege", 4, 4, 120, 60, 100, 1, 5));
        missionControl.launchMission(alpha, beta, 1);
        MissionResolution resolution = missionControl.resolveMission();

        assertTrue(resolution.isResolved());
        assertFalse(resolution.isSuccess());
        assertEquals(CrewMember.LOCATION_QUARTERS, alpha.getLocation());
        assertEquals(CrewMember.LOCATION_QUARTERS, beta.getLocation());
        assertFalse(alpha.isInjured());
        assertFalse(beta.isInjured());
    }

    /**
     * Verifies that mission penalties expire after the next resolved mission.
     */
    @Test
    public void missionPenaltyTicksDownAfterNextResolvedMission() {
        Storage storage = new Storage();
        StatisticsManager statisticsManager = new StatisticsManager();
        MissionControl missionControl = new MissionControl(storage, statisticsManager);

        CrewMember penalized = new Pilot(1, "Nova");
        penalized.assignMissionPenalty(1);
        penalized.setLocation(CrewMember.LOCATION_QUARTERS);
        storage.addCrewMember(penalized);

        CrewMember alpha = new OverpoweredCrew(2, "Atlas");
        CrewMember beta = new OverpoweredCrew(3, "Vera");
        alpha.setLocation(CrewMember.LOCATION_MISSION_READY);
        beta.setLocation(CrewMember.LOCATION_MISSION_READY);
        storage.addCrewMember(alpha);
        storage.addCrewMember(beta);

        missionControl.setCurrentThreat(new Threat("Scout", Threat.CATEGORY_FLYING,
                "Interception", 4, 1, 10, 40, 100, 1, 3));
        missionControl.launchMission(alpha, beta, 1);
        missionControl.resolveMission();

        assertEquals(0, penalized.getMissionPenaltyRemaining());
    }

    /**
     * Verifies that later days create stronger variants of the same template threat.
     */
    @Test
    public void threatScalingByDayIncreasesThreatPower() {
        Storage storage = new Storage();
        StatisticsManager statisticsManager = new StatisticsManager();
        MissionControl missionControl = new MissionControl(storage, statisticsManager);

        missionControl.setRandomSeed(7L);
        Threat dayOneThreat = missionControl.generateThreat(1);
        missionControl.setRandomSeed(7L);
        Threat dayFiveThreat = missionControl.generateThreat(5);

        assertEquals(dayOneThreat.getName(), dayFiveThreat.getName());
        assertTrue(dayFiveThreat.getSkill() > dayOneThreat.getSkill());
        assertTrue(dayFiveThreat.getMaxEnergy() > dayOneThreat.getMaxEnergy());
        assertEquals(5, dayOneThreat.getDeadlineDay());
        assertEquals(9, dayFiveThreat.getDeadlineDay());
        assertTrue(dayFiveThreat.isOverdue(10));
    }

    /**
     * Verifies that pair bonuses are emitted into the replay log.
     */
    @Test
    public void missionResolution_logsCompositionBonuses() {
        Storage storage = new Storage();
        StatisticsManager statisticsManager = new StatisticsManager();
        MissionControl missionControl = new MissionControl(storage, statisticsManager);

        CrewMember pilot = new Pilot(1, "Nova");
        CrewMember engineer = new Engineer(2, "Bolt");
        pilot.setLocation(CrewMember.LOCATION_MISSION_READY);
        engineer.setLocation(CrewMember.LOCATION_MISSION_READY);
        storage.addCrewMember(pilot);
        storage.addCrewMember(engineer);

        missionControl.setCurrentThreat(new Threat("Skyrender", Threat.CATEGORY_FLYING,
                "Interception", 5, 1, 14, 50, 100, 1, 3));
        missionControl.launchMission(pilot, engineer, 1);
        MissionResolution resolution = missionControl.resolveMission();

        boolean foundBonus = false;
        for (MissionEvent event : resolution.getEvents()) {
            if ("Composition Bonus".equals(event.getTitle())) {
                foundBonus = true;
                break;
            }
        }
        assertTrue(foundBonus);
    }

    /**
     * Verifies that defend tactics now emit role-specific support skills into the replay log.
     */
    @Test
    public void missionResolution_logsRoleSupportAbilities() {
        Storage storage = new Storage();
        StatisticsManager statisticsManager = new StatisticsManager();
        MissionControl missionControl = new MissionControl(storage, statisticsManager);

        CrewMember medic = new Medic(1, "Pulse");
        CrewMember soldier = new Soldier(2, "Aegis");
        medic.setLocation(CrewMember.LOCATION_MISSION_READY);
        soldier.setLocation(CrewMember.LOCATION_MISSION_READY);
        storage.addCrewMember(medic);
        storage.addCrewMember(soldier);

        missionControl.setCurrentThreat(new Threat("Boarders", Threat.CATEGORY_COMBAT,
                "Defense", 6, 1, 18, 50, 100, 1, 5));
        missionControl.launchMission(medic, soldier, 1);
        missionControl.setTactics(MissionControl.TACTIC_DEFEND, MissionControl.TACTIC_ATTACK);
        MissionResolution resolution = missionControl.resolveMission();

        boolean foundFieldSupport = false;
        boolean foundSupportBonus = false;
        for (MissionEvent event : resolution.getEvents()) {
            if ("Field Support".equals(event.getTitle())) {
                foundFieldSupport = true;
            }
            if ("Support Bonus".equals(event.getTitle())) {
                foundSupportBonus = true;
            }
        }

        assertTrue(foundFieldSupport);
        assertTrue(foundSupportBonus);
    }

    /**
     * Verifies that preview generation fails safely when the squad is incomplete.
     */
    @Test
    public void missionPreview_returnsPlaceholderForMissingSelection() {
        MissionControl missionControl = new MissionControl(new Storage(), new StatisticsManager());
        Threat threat = new Threat("Scout", Threat.CATEGORY_FLYING, "Interception",
                4, 1, 10, 40, 100, 1, 5);

        MissionTacticPreview preview = missionControl.buildTacticPreview(null, null, threat);

        assertTrue(preview.isPlaceholder());
        assertTrue(preview.getAttackOption().getSummaryText().contains("Select mission-ready crew"));
    }

    /**
     * Verifies that every role exposes a distinct tactic identity in the preview model.
     */
    @Test
    public void missionPreview_allRolesExposeDistinctCoreTitles() {
        MissionControl missionControl = new MissionControl(new Storage(), new StatisticsManager());
        Threat threat = new Threat("Rift Echo", Threat.CATEGORY_ANOMALY, "Investigation",
                6, 2, 20, 50, 100, 1, 5);
        CrewMember ally = new Soldier(99, "Anchor");

        MissionTacticPreview pilotPreview = missionControl.buildTacticPreview(new Pilot(1, "Nova"),
                ally, threat);
        MissionTacticPreview engineerPreview = missionControl.buildTacticPreview(
                new Engineer(2, "Bolt"), ally, threat);
        MissionTacticPreview medicPreview = missionControl.buildTacticPreview(new Medic(3, "Pulse"),
                ally, threat);
        MissionTacticPreview scientistPreview = missionControl.buildTacticPreview(
                new Scientist(4, "Quill"), ally, threat);
        MissionTacticPreview soldierPreview = missionControl.buildTacticPreview(
                new Soldier(5, "Aegis"), ally, threat);

        assertEquals("Flyby Strike", pilotPreview.getAttackOption().getTitle());
        assertEquals("Sabotage Burst", engineerPreview.getAttackOption().getTitle());
        assertEquals("Siphon Strike", medicPreview.getAttackOption().getTitle());
        assertEquals("Weak Point Scan", scientistPreview.getAttackOption().getTitle());
        assertEquals("Heavy Impact", soldierPreview.getAttackOption().getTitle());

        assertEquals("Evasive Cover", pilotPreview.getDefendOption().getTitle());
        assertEquals("Barrier Rig", engineerPreview.getDefendOption().getTitle());
        assertEquals("Field Support", medicPreview.getDefendOption().getTitle());
        assertEquals("Threat Scan", scientistPreview.getDefendOption().getTitle());
        assertEquals("Frontline Guard", soldierPreview.getDefendOption().getTitle());
    }

    /**
     * Verifies that preview text surfaces role support, threat bonuses, and pair bonuses.
     */
    @Test
    public void missionPreview_surfacesRoleThreatAndPairBonuses() {
        MissionControl missionControl = new MissionControl(new Storage(), new StatisticsManager());

        CrewMember medic = new Medic(1, "Pulse");
        CrewMember scientist = new Scientist(2, "Quill");
        Threat anomalyThreat = new Threat("Rift Echo", Threat.CATEGORY_ANOMALY, "Investigation",
                6, 2, 20, 50, 100, 1, 5);

        MissionTacticPreview medicPreview = missionControl.buildTacticPreview(medic, scientist,
                anomalyThreat);
        MissionTacticPreview scientistPreview = missionControl.buildTacticPreview(scientist, medic,
                anomalyThreat);

        assertTrue(medicPreview.getDefendOption().getSummaryText().contains("Heal the ally"));
        assertTrue(scientistPreview.getAttackOption().getSummaryText().contains("Anomaly target"));
        assertTrue(scientistPreview.getAttackOption().getSummaryText().contains("Medic + Scientist"));

        CrewMember pilot = new Pilot(3, "Nova");
        CrewMember engineer = new Engineer(4, "Bolt");
        Threat flyingThreat = new Threat("Skyrender", Threat.CATEGORY_FLYING, "Interception",
                5, 1, 14, 50, 100, 1, 5);

        MissionTacticPreview pilotPreview = missionControl.buildTacticPreview(pilot, engineer,
                flyingThreat);

        assertTrue(pilotPreview.getAttackOption().getSummaryText().contains("Flying target"));
        assertTrue(pilotPreview.getAttackOption().getSummaryText().contains("Pilot + Engineer"));

        CrewMember soldier = new Soldier(5, "Aegis");
        Threat combatThreat = new Threat("Boarders", Threat.CATEGORY_COMBAT, "Defense",
                6, 1, 18, 50, 100, 1, 5);
        MissionTacticPreview soldierPreview = missionControl.buildTacticPreview(soldier, medic,
                combatThreat);

        assertTrue(soldierPreview.getDefendOption().getSummaryText().contains("counter"));
        assertTrue(soldierPreview.getDefendOption().getSummaryText().contains("Soldier + Medic"));
    }

    /**
     * Verifies that storage can return only crew members assigned to a specific location.
     */
    @Test
    public void storage_filtersCrewByLocation() {
        Storage storage = new Storage();
        CrewMember quartersCrew = new Pilot(1, "A");
        CrewMember simulatorCrew = new Engineer(2, "B");
        simulatorCrew.setLocation(CrewMember.LOCATION_SIMULATOR);

        storage.addCrewMember(quartersCrew);
        storage.addCrewMember(simulatorCrew);

        List<CrewMember> quarters = storage.getCrewByLocation(CrewMember.LOCATION_QUARTERS);
        List<CrewMember> simulator = storage.getCrewByLocation(CrewMember.LOCATION_SIMULATOR);

        assertEquals(1, quarters.size());
        assertEquals("A", quarters.get(0).getName());
        assertEquals(1, simulator.size());
        assertEquals("B", simulator.get(0).getName());
    }

    /**
     * Verifies that storage returns crew members in a stable deterministic order.
     */
    @Test
    public void storage_returnsCrewInAscendingIdOrder() {
        Storage storage = new Storage();
        storage.addCrewMember(new Pilot(5, "Echo"));
        storage.addCrewMember(new Engineer(2, "Bolt"));
        storage.addCrewMember(new Medic(4, "Pulse"));
        storage.addCrewMember(new Scientist(1, "Quill"));

        List<CrewMember> crew = storage.getAllCrew();

        assertEquals(4, crew.size());
        assertEquals(1, crew.get(0).getId());
        assertEquals(2, crew.get(1).getId());
        assertEquals(4, crew.get(2).getId());
        assertEquals(5, crew.get(3).getId());
    }

    /**
     * Verifies that a full-colony rest day is only available when somebody is actually in Quarters.
     */
    @Test
    public void quarters_hasCrewToRestOnlyWhenQuartersIsOccupied() {
        Storage storage = new Storage();
        Quarters quarters = new Quarters();

        assertFalse(quarters.hasCrewToRest(storage));

        CrewMember simulatorCrew = new Engineer(1, "Bolt");
        simulatorCrew.setLocation(CrewMember.LOCATION_SIMULATOR);
        storage.addCrewMember(simulatorCrew);
        assertFalse(quarters.hasCrewToRest(storage));

        CrewMember quartersCrew = new Pilot(2, "Nova");
        quartersCrew.setLocation(CrewMember.LOCATION_QUARTERS);
        storage.addCrewMember(quartersCrew);
        assertTrue(quarters.hasCrewToRest(storage));
    }

    /**
     * Strong deterministic test specialist used for mission success cases.
     */
    private static class OverpoweredCrew extends CrewMember {

        OverpoweredCrew(int id, String name) {
            super(id, name, 18, 5, 28, "Pilot");
        }

        @Override
        public int act() {
            return 22;
        }
    }

    /**
     * Weak deterministic test specialist used for mission failure cases.
     */
    private static class WeakCrew extends CrewMember {

        WeakCrew(int id, String name) {
            super(id, name, 1, 0, 6, "Scientist");
        }

        @Override
        public int act() {
            return 1;
        }
    }

    /**
     * Durable deterministic test specialist used for mission failure retreat cases.
     */
    private static class DurableCrew extends CrewMember {

        DurableCrew(int id, String name) {
            super(id, name, 2, 6, 50, "Engineer");
        }

        @Override
        public int act() {
            return 2;
        }
    }
}
