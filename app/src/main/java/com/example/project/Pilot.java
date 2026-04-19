package com.example.project;

/**
 * Pilot specialization with strong survivability and slightly wider attack variance.
 */
public class Pilot extends CrewMember {

    /**
     * Creates a pilot with the fixed stat values defined for this specialization.
     *
     * @param id unique crew member id
     * @param name display name for the crew member
     */
    public Pilot(int id, String name) {
        super(id, name, 5, 4, 20, "Pilot");
    }

    /**
     * Calculates pilot attack power.
     *
     * @return attack value with a slightly larger random range than the base crew implementation
     */
    @Override
    public int act() {
        return getSkill() + (getExperience() / 100) + (int) (Math.random() * 4);
    }
}
