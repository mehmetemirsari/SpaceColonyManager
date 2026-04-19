package com.example.project;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;

/**
 * Handles looping menu music selection and volume control for the application.
 */
public class MusicManager {

    private static final String KEY_SELECTED_TRACK = "music_selected_track";
    private static final String KEY_VOLUME = "music_volume";

    private static final int[] TRACK_RESOURCES = {
            R.raw.colony_mainframe,
            R.raw.bonus_poset_protocol,
            R.raw.floodline_drive,
            R.raw.starhaven_echo,
            R.raw.hazard_bloom
    };

    private static final String[] TRACK_NAMES = {
            "Colony Mainframe",
            "Bonus: Poset Protocol",
            "Floodline Drive",
            "Starhaven Echo",
            "Hazard Bloom"
    };

    private final Context appContext;
    private MediaPlayer mediaPlayer;
    private int selectedTrackIndex;
    private float volume;

    /**
     * Creates the menu music manager.
     *
     * @param context any application context
     */
    public MusicManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.selectedTrackIndex = 0;
        this.volume = 0.6f;
    }

    /**
     * Restores audio preferences from SharedPreferences and starts playback.
     *
     * @param sharedPreferences saved preference source
     */
    public void restorePreferences(SharedPreferences sharedPreferences) {
        restorePreferences(sharedPreferences, "");
    }

    /**
     * Restores audio preferences from a keyed preference slot and starts playback.
     *
     * @param sharedPreferences saved preference source
     * @param prefix key prefix for the target save slot
     */
    public void restorePreferences(SharedPreferences sharedPreferences, String prefix) {
        selectedTrackIndex = sharedPreferences.getInt(prefix + KEY_SELECTED_TRACK, 0);
        if (selectedTrackIndex < 0 || selectedTrackIndex >= TRACK_RESOURCES.length) {
            selectedTrackIndex = 0;
        }
        volume = sharedPreferences.getFloat(prefix + KEY_VOLUME, 0.6f);
        start();
    }

    /**
     * Persists audio preferences through the caller's editor.
     *
     * @param editor target preference editor
     */
    public void writePreferences(SharedPreferences.Editor editor) {
        writePreferences(editor, "");
    }

    /**
     * Persists audio preferences into a keyed preference slot.
     *
     * @param editor target preference editor
     * @param prefix key prefix for the target save slot
     */
    public void writePreferences(SharedPreferences.Editor editor, String prefix) {
        editor.putInt(prefix + KEY_SELECTED_TRACK, selectedTrackIndex);
        editor.putFloat(prefix + KEY_VOLUME, volume);
    }

    /**
     * Starts or restarts menu music using the selected track.
     */
    public void start() {
        releasePlayer();
        mediaPlayer = MediaPlayer.create(appContext, TRACK_RESOURCES[selectedTrackIndex]);
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.setLooping(true);
        mediaPlayer.setVolume(volume, volume);
        mediaPlayer.start();
    }

    /**
     * Resumes playback if the current track is paused.
     */
    public void resume() {
        if (mediaPlayer == null) {
            start();
            return;
        }
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    /**
     * Pauses playback without releasing the current player.
     */
    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    /**
     * Releases all player resources.
     */
    public void release() {
        releasePlayer();
    }

    /**
     * Updates the selected track and restarts playback immediately.
     *
     * @param index selected track index
     */
    public void setSelectedTrackIndex(int index) {
        if (index < 0 || index >= TRACK_RESOURCES.length) {
            return;
        }
        selectedTrackIndex = index;
        start();
    }

    /**
     * Updates the current volume and applies it to the active player.
     *
     * @param volume normalized volume between 0.0 and 1.0
     */
    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(this.volume, this.volume);
        }
    }

    /**
     * @return display names for the bundled menu tracks
     */
    public String[] getTrackNames() {
        return TRACK_NAMES.clone();
    }

    /**
     * @return selected track index
     */
    public int getSelectedTrackIndex() {
        return selectedTrackIndex;
    }

    /**
     * @return current normalized volume between 0.0 and 1.0
     */
    public float getVolume() {
        return volume;
    }

    /**
     * @return currently selected track name
     */
    public String getCurrentTrackName() {
        return TRACK_NAMES[selectedTrackIndex];
    }

    /**
     * Releases the current player if present.
     */
    private void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
