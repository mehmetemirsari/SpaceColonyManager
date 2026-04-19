package com.example.project;

/**
 * Soldier specialization focused on raw offensive output.
 */
public class Soldier extends CrewMember {

    /**
     * Creates a soldier with the fixed stat values defined for this specialization.
     *
     * @param id unique crew member id
     * @param name display name for the crew member
     */
    public Soldier(int id, String name) {
        super(id, name, 9, 0, 16, "Soldier");
    }

    /**
     * Calculates soldier attack power with an aggressive flat bonus.
     *
     * @return enhanced attack value
     */
    @Override
    public int act() {
        return getEffectiveSkill() + 2 + (int) (Math.random() * 3);
    }
}
