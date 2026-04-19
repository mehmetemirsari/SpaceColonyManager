package com.example.project;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles serialization and deserialization of crew and threat state for persistence.
 * <p>
 * The save format intentionally uses plain Java string encoding so it works in both the Android
 * runtime and host-side unit tests without depending on Android's JSON stubs.
 */
public class SaveLoadManager {

    private static final String RECORD_SEPARATOR = "\n";
    private static final String FIELD_SEPARATOR = "\t";

    /**
     * Serializes all crew members in storage into a save string.
     *
     * @param storage current colony storage
     * @return encoded string representing every saved crew member
     */
    public String createJsonFromStorage(Storage storage) {
        StringBuilder builder = new StringBuilder();
        for (CrewMember crewMember : storage.getAllCrew()) {
            if (builder.length() > 0) {
                builder.append(RECORD_SEPARATOR);
            }
            builder.append(crewMember.getId()).append(FIELD_SEPARATOR)
                    .append(encode(crewMember.getName())).append(FIELD_SEPARATOR)
                    .append(encode(crewMember.getSpecialization())).append(FIELD_SEPARATOR)
                    .append(crewMember.getCurrentEnergy()).append(FIELD_SEPARATOR)
                    .append(crewMember.getExperience()).append(FIELD_SEPARATOR)
                    .append(crewMember.isInjured()).append(FIELD_SEPARATOR)
                    .append(encode(crewMember.getLocation())).append(FIELD_SEPARATOR)
                    .append(crewMember.getMissionPenaltyRemaining());
        }
        return builder.toString();
    }

    /**
     * Deserializes a saved crew string into crew member objects.
     *
     * @param saveString serialized crew data
     * @return reconstructed list of crew members
     */
    public List<CrewMember> loadStorageFromJson(String saveString) {
        List<CrewMember> loadedCrew = new ArrayList<>();
        if (saveString == null || saveString.isEmpty()) {
            return loadedCrew;
        }

        String[] records = saveString.split(RECORD_SEPARATOR);
        for (String record : records) {
            if (record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split(FIELD_SEPARATOR, -1);
            if (fields.length < 8) {
                continue;
            }

            CrewMember member = createSpecialist(
                    Integer.parseInt(fields[0]),
                    decode(fields[1]),
                    decode(fields[2]));

            if (member == null) {
                continue;
            }

            member.setExperience(Integer.parseInt(fields[4]));
            member.setCurrentEnergy(Integer.parseInt(fields[3]));
            member.setInjuredStatus(Boolean.parseBoolean(fields[5]));
            member.setLocation(normalizeLocation(decode(fields[6])));
            member.setMissionPenaltyRemaining(Integer.parseInt(fields[7]));
            loadedCrew.add(member);
        }
        return loadedCrew;
    }

    /**
     * Serializes one active threat to a save string.
     *
     * @param threat active colony threat
     * @return encoded threat string, or {@code null} when no threat is active
     */
    public String createJsonFromThreat(Threat threat) {
        if (threat == null) {
            return null;
        }

        return encode(threat.getName()) + FIELD_SEPARATOR
                + encode(threat.getCategory()) + FIELD_SEPARATOR
                + encode(threat.getArchetype()) + FIELD_SEPARATOR
                + threat.getSkill() + FIELD_SEPARATOR
                + threat.getResilience() + FIELD_SEPARATOR
                + threat.getCurrentEnergy() + FIELD_SEPARATOR
                + threat.getMaxEnergy() + FIELD_SEPARATOR
                + threat.getResourceReward() + FIELD_SEPARATOR
                + threat.getExperienceReward() + FIELD_SEPARATOR
                + threat.getSpawnDay() + FIELD_SEPARATOR
                + threat.getDeadlineDay();
    }

    /**
     * Restores a threat from a saved string.
     *
     * @param saveString encoded threat string
     * @return reconstructed threat, or {@code null} when nothing was saved
     */
    public Threat loadThreatFromJson(String saveString) {
        if (saveString == null || saveString.isEmpty()) {
            return null;
        }

        String[] fields = saveString.split(FIELD_SEPARATOR, -1);
        if (fields.length < 11) {
            return null;
        }

        return new Threat(
                decode(fields[0]),
                decode(fields[1]),
                decode(fields[2]),
                Integer.parseInt(fields[3]),
                Integer.parseInt(fields[4]),
                Integer.parseInt(fields[5]),
                Integer.parseInt(fields[6]),
                Integer.parseInt(fields[7]),
                Integer.parseInt(fields[8]),
                Integer.parseInt(fields[9]),
                Integer.parseInt(fields[10]));
    }

    /**
     * Creates a crew member from its specialization label.
     */
    private CrewMember createSpecialist(int id, String name, String specialization) {
        switch (specialization) {
            case "Pilot":
                return new Pilot(id, name);
            case "Medic":
                return new Medic(id, name);
            case "Engineer":
                return new Engineer(id, name);
            case "Scientist":
                return new Scientist(id, name);
            case "Soldier":
                return new Soldier(id, name);
            default:
                return null;
        }
    }

    /**
     * Maps legacy saved locations into the expanded location model.
     */
    private String normalizeLocation(String location) {
        if ("MissionControl".equals(location)) {
            return CrewMember.LOCATION_MISSION_READY;
        }
        return location;
    }

    /**
     * Escapes a field for safe persistence.
     */
    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /**
     * Restores an escaped save field.
     */
    private String decode(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
