package com.example.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Fragment used to hire new crew members into the colony.
 * <p>
 * It presents specialization choices, shows a stat preview, and enforces resource and crew-cap rules.
 */
public class RecruitFragment extends Fragment {

    private EditText etCrewName;
    private Spinner spinnerSpec;
    private TextView tvStatPreview;
    private View btnRecruit;
    private MainActivity mainActivity;

    /**
     * Inflates the recruitment screen layout.
     *
     * @param inflater layout inflater
     * @param container parent container
     * @param savedInstanceState saved instance state, if any
     * @return inflated recruitment fragment view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recruit, container, false);
    }

    /**
     * Binds the recruitment form widgets and wires specialization selection and hiring actions.
     *
     * @param view created fragment view
     * @param savedInstanceState saved instance state, if any
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();

        etCrewName = view.findViewById(R.id.et_crew_name);
        spinnerSpec = view.findViewById(R.id.spinner_specialization);
        btnRecruit = view.findViewById(R.id.btn_recruit_confirm);
        tvStatPreview = view.findViewById(R.id.tv_stat_preview);

        String[] specs = {"Pilot", "Engineer", "Medic", "Scientist", "Soldier"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, specs);
        spinnerSpec.setAdapter(adapter);

        spinnerSpec.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            /**
             * Updates the stat preview when a specialization is selected.
             */
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateStatPreview(specs[position]);
            }

            /**
             * No-op callback required by the interface.
             */
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        updateStatPreview("Pilot");
        btnRecruit.setOnClickListener(v -> hireNewMember());
    }

    /**
     * Updates the specialization information panel.
     *
     * @param spec selected specialization label
     */
    private void updateStatPreview(String spec) {
        if (tvStatPreview == null) return;
        switch (spec) {
            case "Pilot":
                tvStatPreview.setText("Role: evasive scout\nSkill: 5 | Resilience: 4 | Max Energy: 20");
                break;
            case "Engineer":
                tvStatPreview.setText("Role: technical specialist\nSkill: 6 | Resilience: 3 | Max Energy: 19\nBonus: +2 against Technical threats");
                break;
            case "Medic":
                tvStatPreview.setText("Role: support healer\nSkill: 7 | Resilience: 2 | Max Energy: 18\nBonus: heals before attacking");
                break;
            case "Scientist":
                tvStatPreview.setText("Role: research expert\nSkill: 8 | Resilience: 1 | Max Energy: 17\nBonus: +1 all-purpose attack advantage");
                break;
            case "Soldier":
                tvStatPreview.setText("Role: frontline attacker\nSkill: 9 | Resilience: 0 | Max Energy: 16\nBonus: +2 flat attack power");
                break;
        }
    }

    /**
     * Attempts to create and recruit a new crew member.
     */
    private void hireNewMember() {
        String name = etCrewName.getText().toString().trim();
        String selectedSpec = spinnerSpec.getSelectedItem().toString();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Enter a crew member name first.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mainActivity.getStorage().getCrewCount() >= mainActivity.getStorage().getMaxCrew()) {
            Toast.makeText(getContext(), "Crew capacity reached. Max " + mainActivity.getStorage().getMaxCrew() + " specialists allowed.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mainActivity.getColonyResources() < 50) {
            Toast.makeText(getContext(), "Not enough resources. Recruitment costs 50.", Toast.LENGTH_SHORT).show();
            return;
        }

        int id = (int) (System.currentTimeMillis() % 100000);
        CrewMember newMember = null;

        switch (selectedSpec) {
            case "Pilot":
                newMember = new Pilot(id, name);
                break;
            case "Medic":
                newMember = new Medic(id, name);
                break;
            case "Engineer":
                newMember = new Engineer(id, name);
                break;
            case "Scientist":
                newMember = new Scientist(id, name);
                break;
            case "Soldier":
                newMember = new Soldier(id, name);
                break;
        }

        if (newMember != null && mainActivity.getStorage().addCrewMember(newMember)) {
            mainActivity.spendResources(50);
            Toast.makeText(getContext(), name + " joined the colony as a " + selectedSpec + ".", Toast.LENGTH_LONG).show();
            etCrewName.setText("");
        } else {
            Toast.makeText(getContext(), "Could not recruit crew member.", Toast.LENGTH_SHORT).show();
        }
    }
}
