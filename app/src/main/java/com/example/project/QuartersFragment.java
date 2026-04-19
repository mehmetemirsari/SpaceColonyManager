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
import java.util.List;

/**
 * Fragment that displays the Quarters crew roster and crew management actions.
 * <p>
 * Crew members in Quarters can rest, move to the Simulator, or move to Mission Control.
 */
public class QuartersFragment extends Fragment {
    private MainActivity mainActivity;
    private CrewAdapter adapter;
    private List<CrewMember> allCrew;

    /**
     * Inflates the Quarters screen layout.
     *
     * @param inflater layout inflater
     * @param container parent container
     * @param savedInstanceState saved instance state, if any
     * @return inflated fragment view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quarters, container, false);
    }

    /**
     * Sets up the RecyclerView and wires crew management actions.
     *
     * @param view created fragment view
     * @param savedInstanceState saved instance state, if any
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();

        RecyclerView rv = view.findViewById(R.id.rv_quarters_list);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        allCrew = mainActivity.getStorage().getCrewByLocation("Quarters");
        adapter = new CrewAdapter(allCrew, (member, action) -> {
            switch (action) {
                case "simulator":
                    mainActivity.getStorage().moveCrewMember(member.getId(), "Simulator");
                    Toast.makeText(getContext(), member.getName() + " moved to Simulator", Toast.LENGTH_SHORT).show();
                    break;
                case "mission":
                    mainActivity.getStorage().moveCrewMember(member.getId(), "MissionControl");
                    Toast.makeText(getContext(), member.getName() + " moved to Mission Control", Toast.LENGTH_SHORT).show();
                    break;
                case "rest":
                    mainActivity.getQuarters().restCrew(member);
                    Toast.makeText(getContext(), member.getName() + " is fully rested!", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
            refreshList();
        });
        rv.setAdapter(adapter);

        view.findViewById(R.id.btn_rest_all).setOnClickListener(v -> {
            mainActivity.getQuarters().restAll(mainActivity.getStorage());
            refreshList();
            Toast.makeText(getContext(), "All crew in Quarters fully rested!", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Refreshes the displayed Quarters roster when returning to the fragment.
     */
    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    /**
     * Reloads the Quarters crew list from storage and updates the adapter.
     */
    private void refreshList() {
        allCrew = mainActivity.getStorage().getCrewByLocation("Quarters");
        adapter.updateList(allCrew);
    }
}
