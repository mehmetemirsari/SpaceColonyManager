package com.example.project;

import java.util.List;

/**
 * Utility class that encapsulates crew resting and recovery behavior in Quarters.
 * <p>
 * This class keeps Quarters-related rules out of the fragment layer so the implementation
 * better matches the project UML and remains easier to reuse.
 */
public class Quarters {

    /**
     * Fully restores one crew member and ensures they are assigned to Quarters.
     *
     * @param member crew member to rest
     */
    public void restCrew(CrewMember member) {
        if (member != null) {
            if (member.isInjured()) {
                member.advanceRecoveryDay();
            } else {
                member.restoreEnergy();
            }
            member.setLocation("Quarters");
        }
    }

    /**
     * Applies gradual recovery to an injured crew member currently in Quarters.
     *
     * @param member crew member to recover
     */
    public void recoverCrew(CrewMember member) {
        if (member != null && "Quarters".equals(member.getLocation())) {
            member.advanceRecoveryDay();
        }
    }

    /**
     * Fully restores every crew member currently assigned to Quarters.
     *
     * @param storage storage containing the colony crew list
     */
    public void restAll(Storage storage) {
        List<CrewMember> quartersCrew = storage.getCrewByLocation("Quarters");
        for (CrewMember member : quartersCrew) {
            if (member.isInjured()) {
                member.advanceRecoveryDay();
                member.advanceRecoveryDay();
            } else {
                member.restoreEnergy();
            }
            member.setLocation(CrewMember.LOCATION_QUARTERS);
        }
    }
}
