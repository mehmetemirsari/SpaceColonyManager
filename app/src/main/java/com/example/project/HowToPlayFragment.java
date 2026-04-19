package com.example.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Help screen that explains the colony loop and expanded mission rules through a smooth
 * RecyclerView-based guide.
 */
public class HowToPlayFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_how_to_play, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
            @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.rv_how_to_play);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new HowToPlayAdapter(buildGuideSections()));
    }

    /**
     * Builds the ordered help content for the guide.
     *
     * @return guide sections shown in the RecyclerView
     */
    private List<HowToPlayAdapter.GuideSection> buildGuideSections() {
        List<HowToPlayAdapter.GuideSection> sections = new ArrayList<>();
        sections.add(new HowToPlayAdapter.GuideSection(
                "Recruit Specialists",
                "Hire crew members from the Recruit screen. Every specialist starts with a unique base role, attack power, resilience, and HP profile."));
        sections.add(new HowToPlayAdapter.GuideSection(
                "Move Them Through The Colony",
                "Use Quarters to recover, the Simulator to train, and Mission Ready staging to prepare a squad before launching a mission."));
        sections.add(new HowToPlayAdapter.GuideSection(
                "Level Up Your Crew",
                "Every 100 XP raises a crew member's level. Each new level increases attack and max HP. Training gives 25 XP, successful missions give 100 XP, and failed missions still give 50 XP."));
        sections.add(new HowToPlayAdapter.GuideSection(
                "Watch The Active Threat",
                "Only one threat is active at a time. Each threat has a 5-day deadline, and if it stays active past that deadline the colony loses immediately."));
        sections.add(new HowToPlayAdapter.GuideSection(
                "Use Tactics And Compositions",
                "Attack and Defend tactics change combat flow. Specializations and crew pairings perform better on certain threat categories, so squad composition matters."));
        sections.add(new HowToPlayAdapter.GuideSection(
                "How A Day Passes",
                "A day only advances when you do a time-consuming action: training a crew member, using Rest All in Quarters, calling a supply drop, or resolving a mission. Recruiting, moving crew members, and opening screens do not advance the day."));
        sections.add(new HowToPlayAdapter.GuideSection(
                "Manage Time And Resources",
                "Recruiting costs resources, missions reward resources, and supply drops trade one day for extra income. Plan your timing so you do not let the active threat sit too long."));
        sections.add(new HowToPlayAdapter.GuideSection(
                "Handle Knockouts Carefully",
                "If a crew member drops to 0 HP, they move back to Quarters, become injured, and must skip the next mission while recovering."));
        return sections;
    }
}
