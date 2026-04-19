package com.example.project;

/**
 * Mission opponent model used by {@link MissionControl}.
 * <p>
 * Threats carry category and archetype metadata, scale with the current day, and reward the
 * colony when neutralized.
 */
public class Threat {

    /** Threat category used for mission bonuses. */
    public static final String CATEGORY_FLYING = "Flying";
    /** Threat category used for mission bonuses. */
    public static final String CATEGORY_TECHNICAL = "Technical";
    /** Threat category used for mission bonuses. */
    public static final String CATEGORY_BIOLOGICAL = "Biological";
    /** Threat category used for mission bonuses. */
    public static final String CATEGORY_COMBAT = "Combat";
    /** Threat category used for mission bonuses. */
    public static final String CATEGORY_ENVIRONMENTAL = "Environmental";
    /** Threat category used for mission bonuses. */
    public static final String CATEGORY_ANOMALY = "Anomaly";

    private final String name;
    private final String category;
    private final String archetype;
    private final int skill;
    private final int resilience;
    private int currentEnergy;
    private final int maxEnergy;
    private final int resourceReward;
    private final int experienceReward;
    private final int spawnDay;
    private final int deadlineDay;

    /**
     * Creates a fresh threat with full energy.
     *
     * @param name display name of the threat
     * @param category broad threat family used by specialization bonuses
     * @param archetype mission flavor or objective descriptor
     * @param skill offensive power used when retaliating
     * @param resilience defensive stat used to reduce incoming damage
     * @param maxEnergy starting and maximum energy value
     * @param resourceReward resource reward granted on success
     * @param experienceReward XP reward granted on success
     * @param spawnDay day the threat entered the colony state
     * @param deadlineDay last safe day to resolve the threat
     */
    public Threat(String name, String category, String archetype, int skill, int resilience,
            int maxEnergy, int resourceReward, int experienceReward, int spawnDay,
            int deadlineDay) {
        this(name, category, archetype, skill, resilience, maxEnergy, maxEnergy, resourceReward,
                experienceReward, spawnDay, deadlineDay);
    }

    /**
     * Restores a threat from saved state.
     *
     * @param name display name of the threat
     * @param category broad threat family used by specialization bonuses
     * @param archetype mission flavor or objective descriptor
     * @param skill offensive power used when retaliating
     * @param resilience defensive stat used to reduce incoming damage
     * @param currentEnergy current saved energy value
     * @param maxEnergy maximum energy value
     * @param resourceReward resource reward granted on success
     * @param experienceReward XP reward granted on success
     * @param spawnDay day the threat entered the colony state
     * @param deadlineDay last safe day to resolve the threat
     */
    public Threat(String name, String category, String archetype, int skill, int resilience,
            int currentEnergy, int maxEnergy, int resourceReward, int experienceReward,
            int spawnDay, int deadlineDay) {
        this.name = name;
        this.category = category;
        this.archetype = archetype;
        this.skill = skill;
        this.resilience = resilience;
        this.currentEnergy = currentEnergy;
        this.maxEnergy = maxEnergy;
        this.resourceReward = resourceReward;
        this.experienceReward = experienceReward;
        this.spawnDay = spawnDay;
        this.deadlineDay = deadlineDay;
    }

    /**
     * Calculates the damage this threat deals when retaliating.
     *
     * @return raw retaliation damage
     */
    public int act() {
        return skill + 2 + (int) (Math.random() * 4);
    }

    /**
     * Applies incoming damage after resilience reduction.
     *
     * @param damage raw incoming damage
     * @return actual damage dealt after mitigation
     */
    public int takeDamage(int damage) {
        int actualDamage = Math.max(0, damage - resilience);
        currentEnergy = Math.max(0, currentEnergy - actualDamage);
        return actualDamage;
    }

    /**
     * Backwards-compatible alias for {@link #takeDamage(int)}.
     *
     * @param damage raw damage dealt by a crew member
     */
    public void defend(int damage) {
        takeDamage(damage);
    }

    /**
     * @param day current colony day
     * @return {@code true} when the colony has exceeded the deadline for this threat
     */
    public boolean isOverdue(int day) {
        return day > deadlineDay;
    }

    /**
     * @return threat name
     */
    public String getName() {
        return name;
    }

    /**
     * @return threat category used by mission bonuses
     */
    public String getCategory() {
        return category;
    }

    /**
     * @return threat mission archetype
     */
    public String getArchetype() {
        return archetype;
    }

    /**
     * @return threat skill stat
     */
    public int getSkill() {
        return skill;
    }

    /**
     * @return threat resilience stat
     */
    public int getResilience() {
        return resilience;
    }

    /**
     * @return current threat energy
     */
    public int getCurrentEnergy() {
        return currentEnergy;
    }

    /**
     * @return backwards-compatible alias for {@link #getCurrentEnergy()}
     */
    public int getEnergy() {
        return getCurrentEnergy();
    }

    /**
     * @return maximum threat energy
     */
    public int getMaxEnergy() {
        return maxEnergy;
    }

    /**
     * @return resource reward for defeating the threat
     */
    public int getResourceReward() {
        return resourceReward;
    }

    /**
     * @return XP reward for defeating the threat
     */
    public int getExperienceReward() {
        return experienceReward;
    }

    /**
     * @return day the threat spawned
     */
    public int getSpawnDay() {
        return spawnDay;
    }

    /**
     * @return colony day by which this threat must be defeated
     */
    public int getDeadlineDay() {
        return deadlineDay;
    }

    /**
     * @return legacy compatibility alias for {@link #getCategory()}
     */
    public String getThreatType() {
        return category;
    }

    /**
     * Restores saved energy state.
     *
     * @param currentEnergy restored current energy
     */
    public void setCurrentEnergy(int currentEnergy) {
        this.currentEnergy = Math.max(0, Math.min(currentEnergy, maxEnergy));
    }
}
