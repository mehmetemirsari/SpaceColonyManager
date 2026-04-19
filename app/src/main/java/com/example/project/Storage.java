package com.example.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * In-memory storage for all crew members in the colony.
 * <p>
 * This class acts as the central collection used by fragments, persistence, and mission logic.
 */
public class Storage {
    private static final int MAX_CREW = 5;
    private HashMap<Integer, CrewMember> crewMap;

    /**
     * Creates empty storage for colony crew members.
     */
    public Storage() {
        this.crewMap = new HashMap<>();
    }

    /**
     * Adds a crew member if the id is unique and the crew cap has not been reached.
     *
     * @param c crew member to add
     * @return {@code true} if the crew member was added successfully
     */
    public boolean addCrewMember(CrewMember c) {
        if (c == null) return false;
        if (crewMap.containsKey(c.getId())) return false;
        if (crewMap.size() >= MAX_CREW) return false;

        crewMap.put(c.getId(), c);
        return true;
    }

    /**
     * Removes a crew member from storage.
     *
     * @param id id of the crew member to remove
     */
    public void removeCrewMember(int id) {
        crewMap.remove(id);
    }

    /**
     * Looks up a crew member by id.
     *
     * @param id crew member id
     * @return matching crew member, or {@code null} if not found
     */
    public CrewMember getCrewMember(int id) {
        return crewMap.get(id);
    }

    /**
     * Returns all crew members as a list copy.
     *
     * @return list of all current crew members
     */
    public List<CrewMember> getAllCrew() {
        return new ArrayList<>(crewMap.values());
    }

    /**
     * Returns the current crew count.
     *
     * @return number of crew members stored
     */
    public int getCrewCount() {
        return crewMap.size();
    }

    /**
     * Returns the maximum crew capacity.
     *
     * @return crew cap for the colony
     */
    public int getMaxCrew() {
        return MAX_CREW;
    }

    /**
     * Returns all crew members currently assigned to the given location.
     *
     * @param location location label to filter by
     * @return list of matching crew members
     */
    public List<CrewMember> getCrewByLocation(String location) {
        List<CrewMember> filtered = new ArrayList<>();
        for (CrewMember member : crewMap.values()) {
            if (location.equals(member.getLocation())) {
                filtered.add(member);
            }
        }
        return filtered;
    }

    /**
     * Moves a healthy crew member to a new location.
     *
     * @param id id of the crew member to move
     * @param location target location label
     */
    public void moveCrewMember(int id, String location) {
        CrewMember member = crewMap.get(id);
        if (member != null && !member.isInjured()) {
            member.setLocation(location);
        }
    }

    /**
     * Applies one day of Quarters recovery to the whole colony.
     */
    public void advanceRecoveryDay() {
        for (CrewMember member : crewMap.values()) {
            member.advanceRecoveryDay();
        }
    }

    /**
     * Decrements mission penalty counters after a mission resolves, skipping newly penalized crew.
     *
     * @param excludedIds crew ids whose penalties should not tick down yet
     */
    public void decrementMissionPenaltiesExcluding(Collection<Integer> excludedIds) {
        for (CrewMember member : crewMap.values()) {
            if (excludedIds != null && excludedIds.contains(member.getId())) {
                continue;
            }
            member.consumeMissionPenalty();
        }
    }
}
