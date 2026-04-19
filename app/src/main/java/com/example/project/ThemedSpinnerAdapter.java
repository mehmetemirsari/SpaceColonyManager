package com.example.project;

import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Shared spinner adapter that keeps selected and dropdown text readable on the colony's dark UI.
 */
public class ThemedSpinnerAdapter extends ArrayAdapter<String> {

    /**
     * Creates a spinner adapter from a string array.
     *
     * @param context current UI context
     * @param items spinner labels to display
     */
    public ThemedSpinnerAdapter(@NonNull Context context, @NonNull String[] items) {
        this(context, Arrays.asList(items));
    }

    /**
     * Creates a spinner adapter from a string list.
     *
     * @param context current UI context
     * @param items spinner labels to display
     */
    public ThemedSpinnerAdapter(@NonNull Context context, @NonNull List<String> items) {
        super(context, R.layout.item_spinner_selected, new ArrayList<>(items));
        setDropDownViewResource(R.layout.item_spinner_dropdown);
    }
}
