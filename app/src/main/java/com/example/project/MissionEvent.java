package com.example.project;

/**
 * Immutable UI-facing event emitted while resolving a mission.
 */
public class MissionEvent {

    /** Informational event type. */
    public static final String TYPE_INFO = "info";
    /** Bonus event type. */
    public static final String TYPE_BONUS = "bonus";
    /** Attack event type. */
    public static final String TYPE_ATTACK = "attack";
    /** Defense or retaliation event type. */
    public static final String TYPE_DEFENSE = "defense";
    /** Status change event type. */
    public static final String TYPE_STATUS = "status";
    /** Reward event type. */
    public static final String TYPE_REWARD = "reward";
    /** End-state event type. */
    public static final String TYPE_OUTCOME = "outcome";

    private final String type;
    private final String title;
    private final String description;

    /**
     * Creates one mission event for the replay feed.
     *
     * @param type display category used for styling
     * @param title short headline shown in the log
     * @param description longer detail for the event
     */
    public MissionEvent(String type, String title, String description) {
        this.type = type;
        this.title = title;
        this.description = description;
    }

    /**
     * @return display type used by the mission log adapter
     */
    public String getType() {
        return type;
    }

    /**
     * @return short event title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return detailed event description
     */
    public String getDescription() {
        return description;
    }
}
