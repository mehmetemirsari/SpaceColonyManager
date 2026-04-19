package com.example.project;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Small host-side test that validates the new level progression helper methods.
 */
public class ExampleUnitTest {

    /**
     * Confirms XP progress helpers match the configured 100 XP level band.
     */
    @Test
    public void xpProgressionHelpers_areConsistent() {
        CrewMember pilot = new Pilot(1, "Nova");
        pilot.gainExperience(175);

        assertEquals(2, pilot.getLevel());
        assertEquals(75, pilot.getXpIntoCurrentLevel());
        assertEquals(25, pilot.getXpNeededForNextLevel());
    }
}
