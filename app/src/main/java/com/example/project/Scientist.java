package com.example.project;

/**
 * Scientist specialization with a small flat research/analysis bonus to attacks.
 */
public class Scientist extends CrewMember {

    /**
     * Creates a scientist with the fixed stat values defined for this specialization.
     *
     * @param id unique crew member id
     * @param name display name for the crew member
     */
    public Scientist(int id, String name) {
        super(id, name, 8, 1, 17, "Scientist");
    }

    /**
     * Calculates scientist attack power with a flat specialization bonus.
     *
     * @return enhanced attack value
     */
    @Override
    public int act() {
        return getEffectiveSkill() + 1 + (int) (Math.random() * 3);
    }
}
