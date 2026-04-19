package com.example.project;

/**
 * Mission opponent model used by {@link MissionControl}.
 * <p>
 * A threat has its own stats and represents the enemy or obstacle the crew must overcome.
 */
public class Threat {
    private String name;
    private int skill;
    private int resilience;
    private int energy;
    private int maxEnergy;
    private String threatType;

    /**
     * Creates a threat with a fixed stat profile.
     *
     * @param name display name of the threat
     * @param skill offensive power used when retaliating
     * @param resilience defensive stat used to reduce incoming damage
     * @param energy starting and maximum energy value
     * @param threatType category used by specialization bonuses
     */
    public Threat(String name, int skill, int resilience, int energy, String threatType) {
        this.name = name;
        this.skill = skill;
        this.resilience = resilience;
        this.energy = energy;
        this.maxEnergy = energy;
        this.threatType = threatType;
    }

    /**
     * Calculates the damage this threat deals when retaliating.
     *
     * @return raw retaliation damage
     */
    public int act() {
        return this.skill * 2;
    }

    /**
     * Applies incoming damage after resilience reduction.
     *
     * @param damage raw damage dealt by a crew member
     */
    public void defend(int damage) {
        int actualDamage = damage - (this.resilience / 5);
        this.energy -= Math.max(actualDamage, 0);
        if (this.energy < 0) {
            this.energy = 0;
        }
    }

    /**
     * @return threat name
     */
    public String getName() { return name; }

    /**
     * @return threat skill stat
     */
    public int getSkill() { return skill; }

    /**
     * @return threat resilience stat
     */
    public int getResilience() { return resilience; }

    /**
     * @return current threat energy
     */
    public int getEnergy() { return energy; }

    /**
     * @return maximum threat energy
     */
    public int getMaxEnergy() { return maxEnergy; }

    /**
     * @return threat category label
     */
    public String getThreatType() { return threatType; }
}
