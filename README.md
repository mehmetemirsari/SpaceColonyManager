# !!IMPORTANT!!

Don't forget to remove unrelated files from the project such as uml, uml2, uml3!!

# Space Colony Manager

Space Colony Manager is an Android game project. The player manages a small space colony by recruiting specialists, training crew members, preparing missions, and responding to growing threats before the colony collapses.

## Project Overview

The game combines colony management and turn-based mission mechanics. Players must balance resources, crew progression, recovery, and mission timing while facing one active threat at a time. If a threat is ignored for too many in-game days, the colony is lost.

## Main Features

- Recruit up to 5 crew members
- Five different roles:
  - Pilot
  - Engineer
  - Medic
  - Scientist
  - Soldier
- Crew movement between stations:
  - Quarters
  - Simulator
  - Mission Ready
  - On Mission
- XP and level system
- Tactical Combat feature has been modified as Role-based attack and defend abilities with pair/composition bonuses between certain crew combinations
- Day-based threat scaling
- Mission rewards and supply drops
- Knockout, injury, and mission penalty system (no death)
- Automatic mission resolution with step-by-step mission replay log
- (custom feature) Settings page with menu music selection, save/load and reset buttons
- How To Play page
- Manual save, manual load, and colony reset
- Full-screen game over screen when the colony collapses
- Multiple fragment usage for how to play, game over and all the places in colony
- Statistics fragment with visualization
- Mission log

## Gameplay Summary

- Recruiting a new crew member costs 50 resources.
- Training in the Simulator gives XP and advances the day.
- Resting in Quarters helps crew recover and can also advance the day.
- Supply drops provide extra resources but cost 1 day.
- Missions require 2 mission-ready crew members.
- Each crew member can be assigned either Attack or Defend.
- Threats have categories, HP, rewards, and deadlines.
- If the active threat is not resolved before its deadline, the player loses.

## Technologies Used

- Java
- Android Studio
- Android SDK
- RecyclerView
- Fragments
- SharedPreferences
- Object-Oriented Programming

## UML's

You can find the related UML diagrams as uml.png, uml2.png and uml3.png.
## Contributors
Emir: gameplay systems, technical structure, mission mechanics
Gülse: interface, player flow, accessibility, summary and polish
