package com.example.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Structured mission result returned by {@link MissionControl}.
 */
public class MissionResolution {

    private final List<MissionEvent> events;
    private final boolean resolved;
    private final boolean success;
    private final int resourceReward;
    private final int experienceReward;

    /**
     * Creates a resolved or informational mission result.
     *
     * @param resolved whether a real mission resolution occurred
     * @param success whether the threat was defeated
     * @param resourceReward colony resource reward granted on success
     * @param experienceReward XP awarded to deployed crew
     * @param events ordered event feed for UI replay
     */
    public MissionResolution(boolean resolved, boolean success, int resourceReward,
            int experienceReward, List<MissionEvent> events) {
        this.resolved = resolved;
        this.success = success;
        this.resourceReward = resourceReward;
        this.experienceReward = experienceReward;
        this.events = new ArrayList<>(events);
    }

    /**
     * Appends an additional event after the combat engine finishes.
     *
     * @param event event to append
     */
    public void addEvent(MissionEvent event) {
        events.add(event);
    }

    /**
     * @return ordered replay events
     */
    public List<MissionEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * @return {@code true} if a mission was actively resolved
     */
    public boolean isResolved() {
        return resolved;
    }

    /**
     * @return {@code true} if the mission defeated the active threat
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return resource reward granted to the colony
     */
    public int getResourceReward() {
        return resourceReward;
    }

    /**
     * @return XP reward granted to deployed crew
     */
    public int getExperienceReward() {
        return experienceReward;
    }
}
