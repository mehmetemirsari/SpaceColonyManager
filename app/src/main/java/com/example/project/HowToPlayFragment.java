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
                "Build Your Colony",
                "Your colony starts small. Recruit specialists from the Recruit screen, remember the crew cap is 5, and spend resources carefully because each recruit costs 50."));
        sections.add(new HowToPlayAdapter.GuideSection(
                "Know The Five Roles",
                "Pilots are evasive specialists who shine against Flying threats, Engineers control technical problems, Medics sustain the squad against Biological pressure, Scientists exploit Anomalies, and Soldiers lead the front line against Combat threats."));
        sections.add(new HowToPlayAdapter.GuideSection(
                "Move Crew Between Stations",
                "Use Quarters for healing and recovery, the Simulator for XP training, Mission Ready for staging a squad, and On Mission for active deployment once a launch is confirmed."));
        sections.add(new HowToPlayAdapter.GuideSection(
                "How XP And Leveling Work",
                "Training grants 25 XP, failed missions still grant 50 XP, and successful missions grant 100 XP. Every 100 XP raises a crew member by 1 level, which increases both attack and max HP."));
        sections.add(new HowToPlayAdapter.GuideSection(
                "How A Day Passes",
                "Time only advances when you do a major action: training, using Rest All, calling a supply drop, or resolving a mission. Recruiting, moving crew, and browsing screens do not consume a day."));
        sections.add(new HowToPlayAdapter.GuideSection(
                "Watch The Active Threat",
                "Only one active threat exists at a time. Every threat shows a category, archetype, HP, deadline, and reward, and the current colony deadline is 5 days. If that deadline is missed, the colony collapses immediately."));
        sections.add(new HowToPlayAdapter.GuideSection(
                "Launching A Mission",
                "Move 2 penalty-free crew members to Mission Ready, choose one tactic for each crew member, review the preview cards, and confirm the launch. The mission then starts and resolves automatically, and the log replays the encounter step by step."));
        sections.add(new HowToPlayAdapter.GuideSection(
                "Attack And Defend Tactics",
                "Both crew members choose their own Attack or Defend tactic in Mission Control. The preview cards show what each tactic will do before launch, the selected tactic is highlighted, and the choice changes retaliation, healing, shielding, support, and bonus damage."));
        sections.add(new HowToPlayAdapter.GuideSection(
                "Role Abilities And Pair Bonuses",
                "Each role uses different Attack and Defend abilities. Medics can heal and support, Soldiers can guard and counterattack, Pilots provide evasive cover, Engineers build barriers and sabotage targets, and Scientists scan for weak points. Pair bonuses also matter, especially Pilot + Engineer, Medic + Scientist, and Soldier + Medic."));
        sections.add(new HowToPlayAdapter.GuideSection(
                "Knockouts, Injury, And Penalties",
                "If a crew member reaches 0 HP, they are not deleted. They return to Quarters, become injured, and must skip the next mission before they can be used again. Recovery happens in Quarters over time."));
        sections.add(new HowToPlayAdapter.GuideSection(
                "Resources, Rewards, And Supply Drops",
                "Successful missions reward colony resources, and supply drops trade 1 day for extra resources. Resources matter because recruiting, recovery planning, and surviving long enough to beat deadlines all depend on them."));
        sections.add(new HowToPlayAdapter.GuideSection(
                "Settings, Saves, And Losing",
                "The Settings screen lets you change menu music, adjust volume, create a manual save, load a manual save, or reset the colony. If the colony collapses, a full-screen loss screen appears, Try Again starts a new run, and Load Manual Save restores a saved colony if one exists."));
        return sections;
    }
}
