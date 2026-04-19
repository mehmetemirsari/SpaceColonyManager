package com.example.project;

/**
 * Medic specialization that restores a small amount of energy before each attack.
 */
public class Medic extends CrewMember {

    /**
     * Creates a medic with the fixed stat values defined for this specialization.
     *
     * @param id unique crew member id
     * @param name display name for the crew member
     */
    public Medic(int id, String name) {
        super(id, name, 7, 2, 18, "Medic");
    }

    /**
     * Heals the medic slightly before calculating attack power.
     *
     * @return attack value after applying the self-heal effect
     */
    @Override
    public int act() {
        int healed = Math.min(4, getMaxEnergy() - getCurrentEnergy());
        setCurrentEnergy(getCurrentEnergy() + healed);
        return getEffectiveSkill() + (int) (Math.random() * 3);
    }
}
