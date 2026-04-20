package com.example.project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * RecyclerView adapter used to render crew member cards across multiple screens.
 */
public class CrewAdapter extends RecyclerView.Adapter<CrewAdapter.CrewViewHolder> {

    /**
     * Callback interface for crew card action buttons.
     */
    public interface CrewActionListener {
        /**
         * Called when an action button is pressed for a crew member.
         *
         * @param member target crew member
         * @param action string action key such as {@code train}, {@code rest}, or {@code ready}
         */
        void onAction(CrewMember member, String action);
    }

    private List<CrewMember> crewList;
    private final CrewActionListener listener;

    /**
     * Creates a new adapter for a crew list.
     *
     * @param crewList crew members to display
     * @param listener optional action listener, or {@code null} for display-only mode
     */
    public CrewAdapter(List<CrewMember> crewList, CrewActionListener listener) {
        this.crewList = crewList;
        this.listener = listener;
    }

    /**
     * Replaces the current list and refreshes the RecyclerView.
     *
     * @param newList new crew list to display
     */
    public void updateList(List<CrewMember> newList) {
        this.crewList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CrewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_crew, parent, false);
        return new CrewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CrewViewHolder holder, int position) {
        CrewMember member = crewList.get(position);

        holder.tvName.setText(member.getName());
        holder.tvSpec.setText(member.getSpecialization() + " | Lv." + member.getLevel()
                + " | XP " + member.getExperience());
        holder.tvLocation.setText(getLocationLabel(member));
        holder.tvProgress.setText("ATK " + member.getSkill() + " | Next level in "
                + member.getXpNeededForNextLevel() + " XP");
        holder.pbEnergy.setMax(member.getMaxEnergy());
        holder.pbEnergy.setProgress(member.getCurrentEnergy());
        holder.tvEnergy.setText("HP " + member.getCurrentEnergy() + "/" + member.getMaxEnergy());
        holder.ivAvatar.setImageResource(getAvatarResource(member.getSpecialization()));

        if (listener == null) {
            holder.btnAction1.setVisibility(View.GONE);
            holder.btnAction2.setVisibility(View.GONE);
            holder.btnAction3.setVisibility(View.GONE);
            return;
        }

        switch (member.getLocation()) {
            case CrewMember.LOCATION_QUARTERS:
                configureButton(holder.btnAction1, "Simulator",
                        v -> listener.onAction(member, "simulator"));
                configureButton(holder.btnAction2, "Mission Prep",
                        v -> listener.onAction(member, "ready"));
                configureButton(holder.btnAction3, member.isInjured() ? "Recover" : "Rest",
                        v -> listener.onAction(member, "rest"));
                break;

            case CrewMember.LOCATION_MISSION_READY:
                configureButton(holder.btnAction1, "To Quarters",
                        v -> listener.onAction(member, "quarters"));
                configureButton(holder.btnAction2, "Simulator",
                        v -> listener.onAction(member, "simulator"));
                holder.btnAction3.setVisibility(View.GONE);
                break;

            case CrewMember.LOCATION_SIMULATOR:
                configureButton(holder.btnAction1, "Train",
                        v -> listener.onAction(member, "train"));
                configureButton(holder.btnAction2, "To Quarters",
                        v -> listener.onAction(member, "quarters"));
                holder.btnAction3.setVisibility(View.GONE);
                break;

            default:
                holder.btnAction1.setVisibility(View.GONE);
                holder.btnAction2.setVisibility(View.GONE);
                holder.btnAction3.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return crewList.size();
    }

    /**
     * Configures one visible action button.
     */
    private void configureButton(Button button, String label, View.OnClickListener listener) {
        button.setVisibility(View.VISIBLE);
        button.setText(label);
        button.setOnClickListener(listener);
    }

    /**
     * Builds a readable location and status label for the UI.
     */
    private String getLocationLabel(CrewMember member) {
        StringBuilder label = new StringBuilder();
        if (CrewMember.LOCATION_MISSION_READY.equals(member.getLocation())) {
            label.append("Mission Ready");
        } else if (CrewMember.LOCATION_ON_MISSION.equals(member.getLocation())) {
            label.append("On Mission");
        } else {
            label.append(member.getLocation());
        }

        if (member.isInjured()) {
            label.append(" | Injured");
        }
        if (member.getMissionPenaltyRemaining() > 0) {
            label.append(" | Penalty ").append(member.getMissionPenaltyRemaining());
        }
        return label.toString();
    }

    /**
     * Maps a specialization label to its avatar drawable resource.
     */
    private int getAvatarResource(String specialization) {
        switch (specialization) {
            case "Pilot":
                return R.drawable.ic_pilot;
            case "Engineer":
                return R.drawable.ic_engineer;
            case "Medic":
                return R.drawable.ic_medic;
            case "Scientist":
                return R.drawable.ic_scientist;
            case "Soldier":
                return R.drawable.ic_soldier;
            default:
                return R.drawable.ic_pilot;
        }
    }

    /**
     * View holder for one crew card.
     */
    public static class CrewViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivAvatar;
        final TextView tvName;
        final TextView tvSpec;
        final TextView tvLocation;
        final TextView tvProgress;
        final TextView tvEnergy;
        final ProgressBar pbEnergy;
        final Button btnAction1;
        final Button btnAction2;
        final Button btnAction3;

        /**
         * Finds and caches all card subviews.
         *
         * @param itemView inflated card view
         */
        public CrewViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvName = itemView.findViewById(R.id.tv_item_name);
            tvSpec = itemView.findViewById(R.id.tv_item_spec);
            tvLocation = itemView.findViewById(R.id.tv_item_location);
            tvProgress = itemView.findViewById(R.id.tv_item_progress);
            tvEnergy = itemView.findViewById(R.id.tv_item_energy);
            pbEnergy = itemView.findViewById(R.id.pb_energy);
            btnAction1 = itemView.findViewById(R.id.btn_action1);
            btnAction2 = itemView.findViewById(R.id.btn_action2);
            btnAction3 = itemView.findViewById(R.id.btn_action3);
        }
    }
}
