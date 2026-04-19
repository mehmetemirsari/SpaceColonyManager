package com.example.project;

import java.io.Serializable;

/**
 * Base model for every crew member in the colony.
 * <p>
 * Specializations define base combat values, while mission and training experience determine
 * each crew member's effective damage and maximum energy over time.
 */
public abstract class CrewMember implements Serializable {

    /** Crew members heal and recover in the Quarters. */
    public static final String LOCATION_QUARTERS = "Quarters";
    /** Crew members gain experience in the Simulator. */
    public static final String LOCATION_SIMULATOR = "Simulator";
    /** Crew members staged for launch wait in the Mission Ready area. */
    public static final String LOCATION_MISSION_READY = "MissionReady";
    /** Crew members currently participating in a mission use this location. */
    public static final String LOCATION_ON_MISSION = "OnMission";

    /** Fixed amount of XP required for each level. */
    public static final int XP_PER_LEVEL = 100;

    private final int id;
    private final String name;
    private final int baseSkill;
    private final int resilience;
    private final int baseMaxEnergy;

    private int currentEnergy;
    private int experience;
    private int missionPenaltyRemaining;
    private final String specialization;
    private String location;
    private boolean injured;

    /**
     * Creates a crew member with a fixed specialization stat profile.
     *
     * @param id unique identifier used by storage and persistence
     * @param name player-visible crew member name
     * @param baseSkill base offensive skill value before leveling
     * @param resilience defensive resilience value
     * @param baseMaxEnergy maximum energy at level 1
     * @param specialization role label shown in the UI
     */
    public CrewMember(int id, String name, int baseSkill, int resilience, int baseMaxEnergy,
            String specialization) {
        this.id = id;
        this.name = name;
        this.baseSkill = baseSkill;
        this.resilience = resilience;
        this.baseMaxEnergy = baseMaxEnergy;
        this.specialization = specialization;
        this.currentEnergy = baseMaxEnergy;
        this.experience = 0;
        this.missionPenaltyRemaining = 0;
        this.location = LOCATION_QUARTERS;
        this.injured = false;
    }

    /**
     * Calculates the crew member's attack value for the current turn.
     *
     * @return raw attack amount before target defense and mission modifiers
     */
    public int act() {
        return getEffectiveSkill() + (int) (Math.random() * 3);
    }

    /**
     * Applies incoming damage after resilience reduction.
     *
     * @param damage raw incoming damage
     * @return actual damage taken after mitigation
     */
    public int takeDamage(int damage) {
        if (injured) {
            return 0;
        }

        int actualDamage = Math.max(0, damage - resilience);
        currentEnergy = Math.max(0, currentEnergy - actualDamage);
        if (currentEnergy == 0) {
            knockOut();
        }
        return actualDamage;
    }

    /**
     * Backwards-compatible alias for {@link #takeDamage(int)}.
     *
     * @param damage raw incoming damage
     */
    public void defend(int damage) {
        takeDamage(damage);
    }

    /**
     * Trains the crew member in the Simulator.
     * <p>
     * Successful training costs 10 energy and grants 25 XP.
     */
    public void train() {
        if (!injured && LOCATION_SIMULATOR.equals(location) && currentEnergy >= 10) {
            currentEnergy -= 10;
            gainExperience(25);
        }
    }

    /**
     * Grants mission or training experience and applies level-up HP growth.
     *
     * @param amount amount of experience to add
     */
    public void gainExperience(int amount) {
        if (amount <= 0) {
            return;
        }

        int oldMaxEnergy = getEffectiveMaxEnergy();
        experience += amount;
        int newMaxEnergy = getEffectiveMaxEnergy();
        if (newMaxEnergy > oldMaxEnergy) {
            currentEnergy = Math.min(newMaxEnergy, currentEnergy + (newMaxEnergy - oldMaxEnergy));
        }
    }

    /**
     * Backwards-compatible alias for {@link #gainExperience(int)}.
     *
     * @param amount amount of experience to add
     */
    public void addExperience(int amount) {
        gainExperience(amount);
    }

    /**
     * Fully restores energy and clears the injured flag.
     */
    public void restoreEnergy() {
        currentEnergy = getEffectiveMaxEnergy();
        injured = false;
    }

    /**
     * Knocks the crew member out of combat and returns them to Quarters.
     */
    public void knockOut() {
        currentEnergy = 0;
        injured = true;
        location = LOCATION_QUARTERS;
    }

    /**
     * Backwards-compatible alias for {@link #knockOut()}.
     */
    public void setInjured() {
        knockOut();
    }

    /**
     * Applies one day of passive recovery while the crew member remains in Quarters.
     */
    public void advanceRecoveryDay() {
        if (!LOCATION_QUARTERS.equals(location)) {
            return;
        }

        if (injured) {
            int healAmount = Math.max(8, getEffectiveMaxEnergy() / 2);
            currentEnergy = Math.min(getEffectiveMaxEnergy(), currentEnergy + healAmount);
            if (currentEnergy >= getEffectiveMaxEnergy()) {
                injured = false;
            }
        } else {
            currentEnergy = Math.min(getEffectiveMaxEnergy(), currentEnergy + 8);
        }
    }

    /**
     * Backwards-compatible alias for {@link #advanceRecoveryDay()}.
     */
    public void recover() {
        advanceRecoveryDay();
    }

    /**
     * Adds mission cooldown after a knockout.
     *
     * @param missions number of upcoming missions the crew member must skip
     */
    public void assignMissionPenalty(int missions) {
        missionPenaltyRemaining = Math.max(missionPenaltyRemaining, missions);
    }

    /**
     * Reduces the remaining mission penalty by one resolved mission.
     */
    public void consumeMissionPenalty() {
        if (missionPenaltyRemaining > 0) {
            missionPenaltyRemaining--;
        }
    }

    /**
     * @return crew member level, starting at 1
     */
    public int getLevel() {
        return (experience / XP_PER_LEVEL) + 1;
    }

    /**
     * @return effective attack stat after level-based growth
     */
    public int getEffectiveSkill() {
        return baseSkill + Math.max(0, getLevel() - 1);
    }

    /**
     * @return effective maximum energy after level-based growth
     */
    public int getEffectiveMaxEnergy() {
        return baseMaxEnergy + (Math.max(0, getLevel() - 1) * 2);
    }

    /**
     * @return progress within the current level band
     */
    public int getXpIntoCurrentLevel() {
        return experience % XP_PER_LEVEL;
    }

    /**
     * @return XP needed to reach the next level threshold
     */
    public int getXpNeededForNextLevel() {
        return XP_PER_LEVEL - getXpIntoCurrentLevel();
    }

    /**
     * @return {@code true} if this crew member can be assigned to a mission right now
     */
    public boolean isAvailableForMission() {
        return !injured && missionPenaltyRemaining == 0;
    }

    /**
     * @return unique crew member id
     */
    public int getId() {
        return id;
    }

    /**
     * @return crew member display name
     */
    public String getName() {
        return name;
    }

    /**
     * @return specialization label shown to the player
     */
    public String getSpecialization() {
        return specialization;
    }

    /**
     * @return level-scaled skill value for display and combat
     */
    public int getSkill() {
        return getEffectiveSkill();
    }

    /**
     * @return base skill before level-based growth
     */
    public int getBaseSkill() {
        return baseSkill;
    }

    /**
     * @return resilience value used to reduce incoming damage
     */
    public int getResilience() {
        return resilience;
    }

    /**
     * @return effective maximum energy value
     */
    public int getMaxEnergy() {
        return getEffectiveMaxEnergy();
    }

    /**
     * @return maximum energy value at level 1
     */
    public int getBaseMaxEnergy() {
        return baseMaxEnergy;
    }

    /**
     * @return current energy value
     */
    public int getCurrentEnergy() {
        return currentEnergy;
    }

    /**
     * @return total accumulated experience
     */
    public int getExperience() {
        return experience;
    }

    /**
     * @return {@code true} if the crew member is recovering from a knockout
     */
    public boolean isInjured() {
        return injured;
    }

    /**
     * @return current location label
     */
    public String getLocation() {
        return location;
    }

    /**
     * @return remaining missions this crew member must skip
     */
    public int getMissionPenaltyRemaining() {
        return missionPenaltyRemaining;
    }

    /**
     * Updates current energy directly.
     *
     * @param currentEnergy new current energy value
     */
    public void setCurrentEnergy(int currentEnergy) {
        this.currentEnergy = Math.max(0, Math.min(currentEnergy, getEffectiveMaxEnergy()));
    }

    /**
     * Maintains source compatibility with older save logic. Level scaling now derives effective
     * energy from base stats and XP, so this method intentionally performs no mutation.
     *
     * @param maxEnergy ignored compatibility parameter
     */
    public void setMaxEnergy(int maxEnergy) {
        // Compatibility no-op. Effective max energy is derived from base stats and level.
    }

    /**
     * Restores total experience directly from persistence.
     *
     * @param experience new experience amount
     */
    public void setExperience(int experience) {
        this.experience = Math.max(0, experience);
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
        this.injured = injuredStatus;
    }

    /**
     * Restores mission penalty data during persistence loading.
     *
     * @param missionPenaltyRemaining saved penalty count
     */
    public void setMissionPenaltyRemaining(int missionPenaltyRemaining) {
        this.missionPenaltyRemaining = Math.max(0, missionPenaltyRemaining);
    }
}
