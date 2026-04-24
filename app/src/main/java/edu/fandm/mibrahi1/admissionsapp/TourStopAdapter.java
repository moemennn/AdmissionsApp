package edu.fandm.mibrahi1.admissionsapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

// Custom adapter for displaying tour stops in a list
public class TourStopAdapter extends ArrayAdapter<String> {

    // Constructor takes context and list of stop names
    public TourStopAdapter(Context context, List<String> stops) {
        super(context, 0, stops);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        // Reuse existing view if available, otherwise inflate a new one
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_tour_stop, parent, false);
        }

        // Get the stop name for this position
        String stopName = getItem(position);

        // Find UI elements in the layout
        TextView tvStopNumber = view.findViewById(R.id.tvStopNumber);
        TextView tvStopName = view.findViewById(R.id.tvStopName);
        TextView tvStopHint = view.findViewById(R.id.tvStopHint);

        // Populate UI with data
        tvStopNumber.setText("Stop " + (position + 1));
        tvStopName.setText(stopName);
        tvStopHint.setText("Tap to view building details");

        return view;
    }
}