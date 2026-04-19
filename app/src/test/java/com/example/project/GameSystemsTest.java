package com.example.project;

import org.junit.Test;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests covering the most important game system rules.
 * <p>
 * These tests focus on storage limits, training behavior, Quarters recovery scope, mission-level
 * statistics, and location filtering.
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
     * Verifies that valid simulator training grants experience and consumes energy.
     */
    @Test
    public void simulator_training_addsExperienceAndCostsEnergy() {
        CrewMember pilot = new Pilot(1, "Nova");
        pilot.setLocation("Simulator");
        int startEnergy = pilot.getCurrentEnergy();

        Simulator simulator = new Simulator();
        boolean trained = simulator.trainCrew(pilot);

        assertTrue(trained);
        assertEquals(50, pilot.getExperience());
        assertEquals(startEnergy - 10, pilot.getCurrentEnergy());
    }

    /**
     * Verifies that Quarters bulk rest only affects crew members currently in Quarters.
     */
    @Test
    public void quarters_restAll_onlyAffectsQuartersCrew() {
        Storage storage = new Storage();
        CrewMember quartersCrew = new Pilot(1, "A");
        CrewMember simulatorCrew = new Engineer(2, "B");

        quartersCrew.setCurrentEnergy(5);
        simulatorCrew.setCurrentEnergy(5);
        simulatorCrew.setLocation("Simulator");

        storage.addCrewMember(quartersCrew);
        storage.addCrewMember(simulatorCrew);

        Quarters quarters = new Quarters();
        quarters.restAll(storage);

        assertEquals(quartersCrew.getMaxEnergy(), quartersCrew.getCurrentEnergy());
        assertEquals(5, simulatorCrew.getCurrentEnergy());
    }

    /**
     * Verifies that mission statistics are counted once per mission and that successful missions
     * reward crew experience.
     */
    @Test
    public void missionControl_recordsMissionLevelStatsAndRewardsCrew() {
        Storage storage = new Storage();
        StatisticsManager statisticsManager = new StatisticsManager();
        MissionControl missionControl = new MissionControl(storage, statisticsManager);

        CrewMember a = new Soldier(1, "Atlas");
        CrewMember b = new Scientist(2, "Vera");
        storage.addCrewMember(a);
        storage.addCrewMember(b);

        String launch = missionControl.launchMission(a, b);
        assertTrue(launch.contains("Mission started!"));

        String result = missionControl.resolveMission();

        assertEquals(1, statisticsManager.getTotalMissions());
        assertTrue(result.contains("MISSION COMPLETE") || result.contains("MISSION FAILED"));

        if (result.contains("MISSION COMPLETE")) {
            assertEquals(100, a.getExperience());
            assertEquals(100, b.getExperience());
            assertEquals(1, statisticsManager.getTotalWins());
        }
    }

    /**
     * Verifies that storage can return only crew members assigned to a specific location.
     */
    @Test
    public void storage_filtersCrewByLocation() {
        Storage storage = new Storage();
        CrewMember a = new Pilot(1, "A");
        CrewMember b = new Engineer(2, "B");
        b.setLocation("Simulator");

        storage.addCrewMember(a);
        storage.addCrewMember(b);

        List<CrewMember> quartersCrew = storage.getCrewByLocation("Quarters");
        List<CrewMember> simulatorCrew = storage.getCrewByLocation("Simulator");

        assertEquals(1, quartersCrew.size());
        assertEquals("A", quartersCrew.get(0).getName());
        assertEquals(1, simulatorCrew.size());
        assertEquals("B", simulatorCrew.get(0).getName());
    }
}
