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
 * <p>
 * The adapter can operate in a read-only mode or an interactive mode depending on whether a
 * {@link CrewActionListener} is supplied.
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
         * @param action string action key such as {@code train}, {@code rest}, or {@code simulator}
         */
        void onAction(CrewMember member, String action);
    }

    private List<CrewMember> crewList;
    private CrewActionListener listener;

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

    /**
     * Inflates a crew card view holder.
     *
     * @param parent parent view group
     * @param viewType adapter view type
     * @return new crew card view holder
     */
    @NonNull
    @Override
    public CrewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_crew, parent, false);
        return new CrewViewHolder(view);
    }

    /**
     * Binds a crew member to a card view and configures context-sensitive action buttons.
     *
     * @param holder destination view holder
     * @param position adapter position being bound
     */
    @Override
    public void onBindViewHolder(@NonNull CrewViewHolder holder, int position) {
        CrewMember member = crewList.get(position);

        holder.tvName.setText(member.getName());
        holder.tvSpec.setText(member.getSpecialization() + " — Lv." + member.getLevel() + " — EXP: " + member.getExperience());
        holder.tvLocation.setText(member.getLocation() + (member.isInjured() ? " [INJURED]" : ""));
        holder.pbEnergy.setMax(member.getMaxEnergy());
        holder.pbEnergy.setProgress(member.getCurrentEnergy());
        holder.tvEnergy.setText("Energy: " + member.getCurrentEnergy() + "/" + member.getMaxEnergy());

        holder.ivAvatar.setImageResource(getAvatarResource(member.getSpecialization()));

        if (listener == null) {
            holder.btnAction1.setVisibility(View.GONE);
            holder.btnAction2.setVisibility(View.GONE);
            holder.btnAction3.setVisibility(View.GONE);
            return;
        }

        String location = member.getLocation();
        switch (location) {
            case "Quarters":
                holder.btnAction1.setVisibility(View.VISIBLE);
                holder.btnAction1.setText("→ Simulator");
                holder.btnAction1.setOnClickListener(v -> listener.onAction(member, "simulator"));

                holder.btnAction2.setVisibility(View.VISIBLE);
                holder.btnAction2.setText("→ Mission");
                holder.btnAction2.setOnClickListener(v -> listener.onAction(member, "mission"));

                holder.btnAction3.setVisibility(View.VISIBLE);
                holder.btnAction3.setText(member.isInjured() ? "Recover" : "Rest");
                holder.btnAction3.setOnClickListener(v -> listener.onAction(member, "rest"));
                break;

            case "Simulator":
                holder.btnAction1.setVisibility(View.VISIBLE);
                holder.btnAction1.setText("Train");
                holder.btnAction1.setOnClickListener(v -> listener.onAction(member, "train"));

                holder.btnAction2.setVisibility(View.VISIBLE);
                holder.btnAction2.setText("→ Quarters");
                holder.btnAction2.setOnClickListener(v -> listener.onAction(member, "quarters"));

                holder.btnAction3.setVisibility(View.GONE);
                break;

            case "MissionControl":
                holder.btnAction1.setVisibility(View.VISIBLE);
                holder.btnAction1.setText("→ Quarters");
                holder.btnAction1.setOnClickListener(v -> listener.onAction(member, "quarters"));

                holder.btnAction2.setVisibility(View.GONE);
                holder.btnAction3.setVisibility(View.GONE);
                break;

            default:
                holder.btnAction1.setVisibility(View.GONE);
                holder.btnAction2.setVisibility(View.GONE);
                holder.btnAction3.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * @return number of crew cards to display
     */
    @Override
    public int getItemCount() {
        return crewList.size();
    }

    /**
     * Maps a specialization label to its avatar drawable resource.
     *
     * @param specialization specialization label
     * @return drawable resource id for the avatar icon
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
        ImageView ivAvatar;
        TextView tvName, tvSpec, tvLocation, tvEnergy;
        ProgressBar pbEnergy;
        Button btnAction1, btnAction2, btnAction3;

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
            tvEnergy = itemView.findViewById(R.id.tv_item_energy);
            pbEnergy = itemView.findViewById(R.id.pb_energy);
            btnAction1 = itemView.findViewById(R.id.btn_action1);
            btnAction2 = itemView.findViewById(R.id.btn_action2);
            btnAction3 = itemView.findViewById(R.id.btn_action3);
        }
    }
}
