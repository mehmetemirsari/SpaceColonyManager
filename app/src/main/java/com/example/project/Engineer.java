package com.example.project;

/**
 * Engineer specialization with a bonus against Technical threats.
 */
public class Engineer extends CrewMember {

    /**
     * Creates an engineer with the fixed stat values defined for this specialization.
     *
     * @param id unique crew member id
     * @param name display name for the crew member
     */
    public Engineer(int id, String name) {
        super(id, name, 6, 3, 19, "Engineer");
    }

    /**
     * Calculates engineer attack power without threat-specific context.
     *
     * @return base engineer attack value
     */
    @Override
    public int act() {
        return getSkill() + (getExperience() / 100) + (int) (Math.random() * 3);
    }

    /**
     * Calculates engineer attack power using the current threat type.
     *
     * @param threat active mission threat
     * @return attack value including a bonus against Technical threats
     */
    public int act(Threat threat) {
        int bonus = threat.getThreatType().equals("Technical") ? 2 : 0;
        return getSkill() + bonus + (getExperience() / 100) + (int) (Math.random() * 3);
    }
}
