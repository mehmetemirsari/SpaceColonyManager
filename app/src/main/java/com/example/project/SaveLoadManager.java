package com.example.project;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles serialization and deserialization of crew data for persistence.
 * <p>
 * This class converts between in-memory {@link Storage} contents and a JSON string stored
 * by {@link MainActivity} in SharedPreferences.
 */
public class SaveLoadManager {

    /**
     * Creates a save/load manager instance.
     */
    public SaveLoadManager() {
    }

    /**
     * Serializes all crew members in storage into a JSON array string.
     *
     * @param storage current colony storage
     * @return JSON string representing every saved crew member
     */
    public String createJsonFromStorage(Storage storage) {
        List<CrewMember> allCrew = storage.getAllCrew();
        JSONArray jsonArray = new JSONArray();

        try {
            for (CrewMember c : allCrew) {
                JSONObject obj = new JSONObject();
                obj.put("id", c.getId());
                obj.put("name", c.getName());
                obj.put("specialization", c.getSpecialization());
                obj.put("skill", c.getSkill());
                obj.put("resilience", c.getResilience());
                obj.put("maxEnergy", c.getMaxEnergy());
                obj.put("currentEnergy", c.getCurrentEnergy());
                obj.put("experience", c.getExperience());
                obj.put("isInjured", c.isInjured());
                obj.put("location", c.getLocation());
                jsonArray.put(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonArray.toString();
    }

    /**
     * Deserializes a saved JSON string into crew member objects.
     *
     * @param jsonString serialized crew data
     * @return reconstructed list of crew members
     */
    public List<CrewMember> loadStorageFromJson(String jsonString) {
        List<CrewMember> loadedCrew = new ArrayList<>();

        if (jsonString == null || jsonString.isEmpty()) {
            return loadedCrew;
        }

        try {
            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);

                int id = obj.getInt("id");
                String name = obj.getString("name");
                String spec = obj.getString("specialization");

                CrewMember member = null;

                switch (spec) {
                    case "Pilot":
                        member = new Pilot(id, name);
                        break;
                    case "Medic":
                        member = new Medic(id, name);
                        break;
                    case "Engineer":
                        member = new Engineer(id, name);
                        break;
                    case "Scientist":
                        member = new Scientist(id, name);
                        break;
                    case "Soldier":
                        member = new Soldier(id, name);
                        break;
                }

                if (member != null) {
                    member.setMaxEnergy(obj.getInt("maxEnergy"));
                    member.setCurrentEnergy(obj.getInt("currentEnergy"));
                    member.setExperience(obj.getInt("experience"));
                    member.setLocation(obj.getString("location"));
                    member.setInjuredStatus(obj.getBoolean("isInjured"));
                    loadedCrew.add(member);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return loadedCrew;
    }
}
