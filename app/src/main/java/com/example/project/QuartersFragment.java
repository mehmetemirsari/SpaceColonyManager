package com.example.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays the Quarters roster and crew management actions.
 */
public class QuartersFragment extends Fragment {

    private MainActivity mainActivity;
    private CrewAdapter adapter;
    private View btnRestAll;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quarters, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();

        RecyclerView recyclerView = view.findViewById(R.id.rv_quarters_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CrewAdapter(getDisplayedCrew(), (member, action) -> {
            if (mainActivity.isGameOver()) {
                Toast.makeText(getContext(), mainActivity.getGameOverReason(), Toast.LENGTH_LONG).show();
                return;
            }

            switch (action) {
                case "simulator":
                    mainActivity.getStorage().moveCrewMember(member.getId(), CrewMember.LOCATION_SIMULATOR);
                    Toast.makeText(getContext(), member.getName() + " moved to the Simulator.",
                            Toast.LENGTH_SHORT).show();
                    break;
                case "ready":
                    mainActivity.getStorage().moveCrewMember(member.getId(), CrewMember.LOCATION_MISSION_READY);
                    Toast.makeText(getContext(), member.getName() + " moved to Mission Ready.",
                            Toast.LENGTH_SHORT).show();
                    break;
                case "quarters":
                    mainActivity.getStorage().moveCrewMember(member.getId(), CrewMember.LOCATION_QUARTERS);
                    Toast.makeText(getContext(), member.getName() + " returned to Quarters.",
                            Toast.LENGTH_SHORT).show();
                    break;
                case "rest":
                    mainActivity.getQuarters().restCrew(member);
                    Toast.makeText(getContext(), member.getName() + " recovered in Quarters.",
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
            refreshList();
        });
        recyclerView.setAdapter(adapter);

        btnRestAll = view.findViewById(R.id.btn_rest_all);
        btnRestAll.setOnClickListener(v -> {
            Toast.makeText(getContext(), mainActivity.restAllCrew(), Toast.LENGTH_LONG).show();
            refreshList();
            mainActivity.showGameOverScreenIfNeeded();
        });
        updateRestAllButtonState();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    /**
     * Reloads the crew list from Quarters and Mission Ready staging.
     */
    private void refreshList() {
        if (adapter != null) {
            adapter.updateList(getDisplayedCrew());
        }
        updateRestAllButtonState();
    }

    /**
     * Builds the roster shown in the Quarters UI.
     */
    private List<CrewMember> getDisplayedCrew() {
        List<CrewMember> displayedCrew = new ArrayList<>();
        for (CrewMember member : mainActivity.getStorage().getAllCrew()) {
            if (CrewMember.LOCATION_QUARTERS.equals(member.getLocation())
                    || CrewMember.LOCATION_MISSION_READY.equals(member.getLocation())) {
                displayedCrew.add(member);
            }
        }
        return displayedCrew;
    }

    /**
     * Disables the full-colony rest action when the colony has no crew members yet.
     */
    private void updateRestAllButtonState() {
        if (btnRestAll == null || mainActivity == null) {
            return;
        }

        boolean hasCrewToRest = mainActivity.getQuarters().hasCrewToRest(mainActivity.getStorage());
        btnRestAll.setEnabled(hasCrewToRest);
        btnRestAll.setAlpha(hasCrewToRest ? 1f : 0.45f);
    }
}
