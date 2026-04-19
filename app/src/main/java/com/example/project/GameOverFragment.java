package com.example.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Full-screen loss state shown when an active threat outlasts the colony.
 */
public class GameOverFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game_over, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) {
            return;
        }

        TextView tvReason = view.findViewById(R.id.tv_game_over_reason);
        View btnTryAgain = view.findViewById(R.id.btn_try_again);
        View btnLoadSave = view.findViewById(R.id.btn_load_save);

        tvReason.setText(mainActivity.getGameOverReason());
        btnLoadSave.setVisibility(mainActivity.hasManualSave() ? View.VISIBLE : View.GONE);

        btnTryAgain.setOnClickListener(v -> mainActivity.resetColony());
        btnLoadSave.setOnClickListener(v -> Toast.makeText(mainActivity,
                mainActivity.loadManualSave(), Toast.LENGTH_LONG).show());
    }
}
