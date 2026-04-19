package com.example.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Fragment used to hire new crew members into the colony.
 */
public class RecruitFragment extends Fragment {

    private EditText etCrewName;
    private Spinner spinnerSpec;
    private TextView tvStatPreview;
    private View btnRecruit;
    private MainActivity mainActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recruit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();

        etCrewName = view.findViewById(R.id.et_crew_name);
        spinnerSpec = view.findViewById(R.id.spinner_specialization);
        btnRecruit = view.findViewById(R.id.btn_recruit_confirm);
        tvStatPreview = view.findViewById(R.id.tv_stat_preview);

        String[] specializations = {"Pilot", "Engineer", "Medic", "Scientist", "Soldier"};
        ThemedSpinnerAdapter adapter = new ThemedSpinnerAdapter(requireContext(), specializations);
        spinnerSpec.setAdapter(adapter);

        spinnerSpec.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position,
                    long id) {
                updateStatPreview(specializations[position]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // No-op.
            }
        });

        updateStatPreview("Pilot");
        btnRecruit.setOnClickListener(v -> hireNewMember());
    }

    /**
     * Updates the specialization information panel.
     */
    private void updateStatPreview(String specialization) {
        switch (specialization) {
            case "Pilot":
                tvStatPreview.setText("Role: Evasive Scout\nSkill: 5 | Resilience: 4 | Max HP: 20");
                break;
            case "Engineer":
                tvStatPreview.setText("Role: Technical Specialist\nSkill: 6 | Resilience: 3 | Max HP: 19\nBonus: thrives against technical threats");
                break;
            case "Medic":
                tvStatPreview.setText("Role: Support Specialist\nSkill: 7 | Resilience: 2 | Max HP: 18\nBonus: improves survival against biological threats");
                break;
            case "Scientist":
                tvStatPreview.setText("Role: Anomaly Analyst\nSkill: 8 | Resilience: 1 | Max HP: 17\nBonus: stronger against anomaly targets");
                break;
            case "Soldier":
                tvStatPreview.setText("Role: Frontline Attacker\nSkill: 9 | Resilience: 0 | Max HP: 16\nBonus: stronger against combat threats");
                break;
            default:
                break;
        }
    }

    /**
     * Attempts to create and recruit a new crew member.
     */
    private void hireNewMember() {
        if (mainActivity.isGameOver()) {
            Toast.makeText(getContext(), mainActivity.getGameOverReason(), Toast.LENGTH_LONG).show();
            return;
        }

        String name = etCrewName.getText().toString().trim();
        String selectedSpec = spinnerSpec.getSelectedItem().toString();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Enter a crew member name first.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mainActivity.getStorage().getCrewCount() >= mainActivity.getStorage().getMaxCrew()) {
            Toast.makeText(getContext(), "Crew capacity reached.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mainActivity.getColonyResources() < 50) {
            Toast.makeText(getContext(), "Not enough resources. Recruitment costs 50.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int id = generateUniqueCrewId();
        CrewMember newMember = createSpecialist(id, name, selectedSpec);
        if (newMember != null && mainActivity.getStorage().addCrewMember(newMember)) {
            mainActivity.spendResources(50);
            Toast.makeText(getContext(), name + " joined the colony as a " + selectedSpec + ".",
                    Toast.LENGTH_LONG).show();
            etCrewName.setText("");
        } else {
            Toast.makeText(getContext(), "Could not recruit crew member.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Generates a stable unique id for a newly recruited crew member.
     */
    private int generateUniqueCrewId() {
        int id = (int) (System.currentTimeMillis() % 100000);
        while (mainActivity.getStorage().getCrewMember(id) != null) {
            id++;
        }
        return id;
    }

    /**
     * Creates the chosen specialization object.
     */
    private CrewMember createSpecialist(int id, String name, String selectedSpec) {
        switch (selectedSpec) {
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
}
