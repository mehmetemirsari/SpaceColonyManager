package com.example.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Fragment that lets the user choose menu music and adjust volume.
 */
public class SettingsFragment extends Fragment {

    private MainActivity mainActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();

        MusicManager musicManager = mainActivity.getMusicManager();
        Spinner spinnerTracks = view.findViewById(R.id.spinner_music_track);
        TextView tvCurrentTrack = view.findViewById(R.id.tv_current_track);
        TextView tvVolumeValue = view.findViewById(R.id.tv_volume_value);
        SeekBar seekBar = view.findViewById(R.id.seek_music_volume);
        View btnSave = view.findViewById(R.id.btn_manual_save);
        View btnLoad = view.findViewById(R.id.btn_manual_load);
        View btnReset = view.findViewById(R.id.btn_reset_colony);

        ThemedSpinnerAdapter adapter =
                new ThemedSpinnerAdapter(requireContext(), musicManager.getTrackNames());
        spinnerTracks.setAdapter(adapter);
        spinnerTracks.setSelection(musicManager.getSelectedTrackIndex());
        tvCurrentTrack.setText("Now playing: " + musicManager.getCurrentTrackName());

        seekBar.setProgress(Math.round(musicManager.getVolume() * 100f));
        tvVolumeValue.setText(Math.round(musicManager.getVolume() * 100f) + "%");

        spinnerTracks.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View itemView,
                    int position, long id) {
                if (musicManager.getSelectedTrackIndex() != position) {
                    musicManager.setSelectedTrackIndex(position);
                }
                tvCurrentTrack.setText("Now playing: " + musicManager.getCurrentTrackName());
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // No-op.
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volume = progress / 100f;
                musicManager.setVolume(volume);
                tvVolumeValue.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No-op.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No-op.
            }
        });

        btnSave.setOnClickListener(v -> showToast(mainActivity.createManualSave()));
        btnLoad.setOnClickListener(v -> showToast(mainActivity.loadManualSave()));
        btnReset.setOnClickListener(v -> showToast(mainActivity.resetColony()));
    }

    /**
     * Shows a settings toast using the activity context so it survives fragment replacement.
     *
     * @param message user-facing message
     */
    private void showToast(@NonNull String message) {
        Toast.makeText(mainActivity, message, Toast.LENGTH_LONG).show();
    }
}
