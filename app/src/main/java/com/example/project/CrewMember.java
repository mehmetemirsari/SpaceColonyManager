package com.example.project;

import java.io.Serializable;

/**
 * Base model for every crew member in the colony.
 * <p>
 * This class stores shared combat, training, recovery, and location state used by all
 * specialization subclasses such as {@link Pilot}, {@link Engineer}, and {@link Soldier}.
 */
public abstract class CrewMember implements Serializable {

    private int id;
    private String name;
    private int skill;
    private int resilience;
    private int maxEnergy;
    private int currentEnergy;
    private int experience;
    protected String specialization;
    private String location;
    private boolean isInjured;

    /**
     * Creates a crew member with a fixed specialization stat profile.
     *
     * @param id unique identifier used by storage and persistence
     * @param name player-visible crew member name
     * @param skill base offensive skill value
     * @param resilience base defensive resilience value
     * @param maxEnergy maximum and starting energy
     * @param specialization role label shown in the UI
     */
    public CrewMember(int id, String name, int skill, int resilience, int maxEnergy, String specialization) {
        this.id = id;
        this.name = name;
        this.skill = skill;
        this.resilience = resilience;
        this.specialization = specialization;
        this.maxEnergy = maxEnergy;
        this.currentEnergy = maxEnergy;
        this.experience = 0;
        this.location = "Quarters";
        this.isInjured = false;
    }

    /**
     * Calculates the crew member's attack value for a turn.
     *
     * @return raw attack amount before the target applies defense
     */
    public int act() {
        return this.skill + (this.experience / 100) + (int) (Math.random() * 3);
    }

    /**
     * Applies incoming damage after resilience reduction.
     * <p>
     * If the resulting energy drops to zero or below, the crew member becomes injured and is
     * returned to Quarters.
     *
     * @param damage raw incoming damage
     */
    public void defend(int damage) {
        if (isInjured) return;

        int actualDamage = damage - (this.resilience / 5);
        this.currentEnergy -= Math.max(actualDamage, 0);

        if (this.currentEnergy <= 0) {
            this.currentEnergy = 0;
            setInjured();
        }
    }

    /**
     * Trains the crew member in the Simulator.
     * <p>
     * Successful training grants 50 experience and costs 10 energy.
     */
    public void train() {
        if (!isInjured && location.equals("Simulator")) {
            this.experience += 50;
            this.currentEnergy -= 10;
            if (this.currentEnergy < 0) this.currentEnergy = 0;
        }
    }

    /**
     * Adds experience directly, typically after a successful mission.
     *
     * @param amount amount of experience to add
     */
    public void addExperience(int amount) {
        this.experience += amount;
    }

    /**
     * Fully restores energy and clears the injured flag.
     */
    public void restoreEnergy() {
        this.currentEnergy = this.maxEnergy;
        this.isInjured = false;
    }

    /**
     * Marks the crew member as injured and returns them to Quarters.
     */
    public void setInjured() {
        this.isInjured = true;
        this.location = "Quarters";
    }

    /**
     * Applies gradual recovery while the crew member is in Quarters.
     * <p>
     * Recovery restores 25 energy per use and fully heals the member once maximum energy is reached.
     */
    public void recover() {
        if (location.equals("Quarters") && isInjured) {
            this.currentEnergy += 25;
            if (this.currentEnergy >= maxEnergy) {
                restoreEnergy();
            }
        }
    }

    /**
     * Converts total experience into a simple displayed level value.
     *
     * @return level number starting at 1
     */
    public int getLevel() {
        return (experience / 100) + 1;
    }

    /**
     * @return unique crew member id
     */
    public int getId() { return id; }

    /**
     * @return crew member display name
     */
    public String getName() { return name; }

    /**
     * @return specialization label shown to the player
     */
    public String getSpecialization() { return specialization; }

    /**
     * @return base skill stat
     */
    public int getSkill() { return skill; }

    /**
     * @return base resilience stat
     */
    public int getResilience() { return resilience; }

    /**
     * @return maximum energy capacity
     */
    public int getMaxEnergy() { return maxEnergy; }

    /**
     * @return current energy value
     */
    public int getCurrentEnergy() { return currentEnergy; }

    /**
     * @return total accumulated experience
     */
    public int getExperience() { return experience; }

    /**
     * @return {@code true} if the crew member is injured
     */
    public boolean isInjured() { return isInjured; }

    /**
     * @return current location label
     */
    public String getLocation() { return location; }

    /**
     * Updates current energy directly.
     *
     * @param currentEnergy new current energy value
     */
    public void setCurrentEnergy(int currentEnergy) {
        this.currentEnergy = currentEnergy;
    }

    /**
     * Updates maximum energy directly.
     *
     * @param maxEnergy new maximum energy value
     */
    public void setMaxEnergy(int maxEnergy) {
        this.maxEnergy = maxEnergy;
    }

    /**
     * Updates total experience directly.
     *
     * @param experience new experience amount
     */
    public void setExperience(int experience) {
        this.experience = experience;
    }

    /**
     * Updates the crew member location directly.
     *
     * @param location new location label
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Restores the saved injured flag during persistence loading.
     *
     * @param injuredStatus saved injured state
     */
    public void setInjuredStatus(boolean injuredStatus) {
        this.isInjured = injuredStatus;
    }
}
