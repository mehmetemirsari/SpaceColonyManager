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

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays and manages the Simulator training roster.
 */
public class SimulatorFragment extends Fragment {

    private MainActivity mainActivity;
    private CrewAdapter adapter;
    private List<CrewMember> simulatorCrew;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_simulator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        setupUI(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            setupUI(getView());
        }
    }

    /**
     * Rebuilds the Simulator UI based on whether any crew members are currently assigned there.
     */
    private void setupUI(@NonNull View view) {
        RecyclerView recyclerView = view.findViewById(R.id.rv_simulator_list);
        View emptyLayout = view.findViewById(R.id.layout_empty_simulator);
        View trainingContent = view.findViewById(R.id.layout_training_content);

        simulatorCrew = new ArrayList<>();
        for (CrewMember crewMember : mainActivity.getStorage().getAllCrew()) {
            if (CrewMember.LOCATION_SIMULATOR.equals(crewMember.getLocation())) {
                simulatorCrew.add(crewMember);
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
                BottomNavigationView navigationView = mainActivity.findViewById(R.id.bottom_navigation);
                navigationView.setSelectedItemId(R.id.nav_quarters);
            });
            return;
        }

        emptyLayout.setVisibility(View.GONE);
        trainingContent.setVisibility(View.VISIBLE);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CrewAdapter(simulatorCrew, (member, action) -> {
            if (mainActivity.isGameOver()) {
                Toast.makeText(getContext(), mainActivity.getGameOverReason(), Toast.LENGTH_LONG).show();
                return;
            }

            if ("train".equals(action)) {
                Toast.makeText(getContext(), mainActivity.trainCrewMember(member), Toast.LENGTH_LONG).show();
                setupUI(view);
                mainActivity.showGameOverScreenIfNeeded();
            } else if ("quarters".equals(action)) {
                mainActivity.getStorage().moveCrewMember(member.getId(), CrewMember.LOCATION_QUARTERS);
                Toast.makeText(getContext(), member.getName() + " returned to Quarters.",
                        Toast.LENGTH_SHORT).show();
                setupUI(view);
            }
        });
        recyclerView.setAdapter(adapter);
    }
}
