package com.example.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays and manages the Simulator training roster.
 */
public class SimulatorFragment extends Fragment {

    private MainActivity mainActivity;
    private CrewAdapter adapter;
    private List<CrewMember> simulatorCrew;

    /**
     * Inflates the Simulator screen layout.
     *
     * @param inflater layout inflater
     * @param container parent container
     * @param savedInstanceState saved instance state, if any
     * @return inflated fragment view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_simulator, container, false);
    }

    /**
     * Initializes the simulator screen after the view hierarchy is created.
     *
     * @param view created fragment view
     * @param savedInstanceState saved instance state, if any
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        setupUI(view);
    }

    /**
     * Refreshes the training roster when returning to the fragment.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) setupUI(getView());
    }

    /**
     * Rebuilds the Simulator UI based on whether any crew members are currently assigned there.
     *
     * @param view root fragment view
     */
    private void setupUI(View view) {
        RecyclerView rv = view.findViewById(R.id.rv_simulator_list);
        View emptyLayout = view.findViewById(R.id.layout_empty_simulator);
        View trainingContent = view.findViewById(R.id.layout_training_content);

        simulatorCrew = new ArrayList<>();
        for (CrewMember c : mainActivity.getStorage().getAllCrew()) {
            if ("Simulator".equals(c.getLocation())) {
                simulatorCrew.add(c);
            }
        }

        if (simulatorCrew.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            trainingContent.setVisibility(View.GONE);

            Button btnGoToQuarters = view.findViewById(R.id.btn_go_to_quarters);
            btnGoToQuarters.setOnClickListener(v -> {
                mainActivity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new QuartersFragment())
                        .addToBackStack(null)
                        .commit();
                com.google.android.material.bottomnavigation.BottomNavigationView nav =
                        mainActivity.findViewById(R.id.bottom_navigation);
                nav.setSelectedItemId(R.id.nav_quarters);
            });
        } else {
            emptyLayout.setVisibility(View.GONE);
            trainingContent.setVisibility(View.VISIBLE);

            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new CrewAdapter(simulatorCrew, (member, action) -> {
                if (action.equals("train")) {
                    if (member.isInjured()) {
                        Toast.makeText(getContext(), member.getName() + " is injured and cannot train!", Toast.LENGTH_SHORT).show();
                    } else if (member.getCurrentEnergy() < 10) {
                        Toast.makeText(getContext(), member.getName() + " is too tired to train!", Toast.LENGTH_SHORT).show();
                    } else if (mainActivity.getSimulator().trainCrew(member)) {
                        Toast.makeText(getContext(), member.getName() + " trained! +50 EXP", Toast.LENGTH_SHORT).show();
                        adapter.notifyDataSetChanged();
                    }
                } else if (action.equals("quarters")) {
                    mainActivity.getStorage().moveCrewMember(member.getId(), "Quarters");
                    Toast.makeText(getContext(), member.getName() + " returned to Quarters", Toast.LENGTH_SHORT).show();
                    setupUI(view);
                }
            });
            rv.setAdapter(adapter);
        }
    }
}
