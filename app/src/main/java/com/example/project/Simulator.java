package com.example.project;

/**
 * Utility class that encapsulates training behavior performed in the Simulator.
 * <p>
 * It validates whether a crew member is eligible to train and then delegates the actual
 * state change to {@link CrewMember#train()}.
 */
public class Simulator {

    /**
     * Trains a crew member if they are healthy, assigned to the Simulator, and have enough energy.
     *
     * @param member crew member to train
     * @return {@code true} if training was performed successfully
     */
    public boolean trainCrew(CrewMember member) {
        if (member == null) return false;
        if (member.isInjured()) return false;
        if (!CrewMember.LOCATION_SIMULATOR.equals(member.getLocation())) return false;
        if (member.getCurrentEnergy() < 10) return false;

        member.train();
        return true;
    }
}
