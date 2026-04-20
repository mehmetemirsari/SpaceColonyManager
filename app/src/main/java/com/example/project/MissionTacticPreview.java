package com.example.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only mission tactic preview model shown before the player deploys a squad.
 */
public final class MissionTacticPreview {

    private final String crewLabel;
    private final TacticOption attackOption;
    private final TacticOption defendOption;
    private final boolean placeholder;

    /**
     * Creates a complete preview for one crew member.
     *
     * @param crewLabel crew label shown above the preview
     * @param attackOption attack tactic description
     * @param defendOption defend tactic description
     * @param placeholder whether this preview is only a placeholder state
     */
    public MissionTacticPreview(String crewLabel, TacticOption attackOption,
            TacticOption defendOption, boolean placeholder) {
        this.crewLabel = crewLabel;
        this.attackOption = attackOption;
        this.defendOption = defendOption;
        this.placeholder = placeholder;
    }

    /**
     * Creates a safe placeholder preview used when the squad is incomplete.
     *
     * @param message player-facing placeholder message
     * @return placeholder preview
     */
    public static MissionTacticPreview placeholder(String message) {
        List<String> lines = Collections.singletonList(message);
        return new MissionTacticPreview("Awaiting Mission Pair",
                new TacticOption("Attack Preview", lines),
                new TacticOption("Defend Preview", lines), true);
    }

    /**
     * @return crew label shown above this preview
     */
    public String getCrewLabel() {
        return crewLabel;
    }

    /**
     * @return attack tactic option details
     */
    public TacticOption getAttackOption() {
        return attackOption;
    }

    /**
     * @return defend tactic option details
     */
    public TacticOption getDefendOption() {
        return defendOption;
    }

    /**
     * @return {@code true} when this preview is only a placeholder message
     */
    public boolean isPlaceholder() {
        return placeholder;
    }

    /**
     * Immutable tactic description payload used by the Mission tab preview UI.
     */
    public static final class TacticOption {
        private final String title;
        private final List<String> effectLines;

        /**
         * Creates one tactic description block.
         *
         * @param title short tactic title
         * @param effectLines player-facing effect lines
         */
        public TacticOption(String title, List<String> effectLines) {
            this.title = title;
            this.effectLines = Collections.unmodifiableList(new ArrayList<>(effectLines));
        }

        /**
         * @return short tactic title
         */
        public String getTitle() {
            return title;
        }

        /**
         * @return read-only list of player-facing effect lines
         */
        public List<String> getEffectLines() {
            return effectLines;
        }

        /**
         * @return multi-line text ready for display in the UI
         */
        public String getSummaryText() {
            StringBuilder builder = new StringBuilder();
            for (String line : effectLines) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append("- ").append(line);
            }
            return builder.toString();
        }
    }
}
