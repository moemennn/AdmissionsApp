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

public class TourStopAdapter extends ArrayAdapter<String> {

    public TourStopAdapter(Context context, List<String> stops) {
        super(context, 0, stops);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.item_tour_stop, parent, false);
        }

        String stopName = getItem(position);

        TextView tvStopNumber = view.findViewById(R.id.tvStopNumber);
        TextView tvStopName = view.findViewById(R.id.tvStopName);
        TextView tvStopHint = view.findViewById(R.id.tvStopHint);

        tvStopNumber.setText("Stop " + (position + 1));
        tvStopName.setText(stopName);
        tvStopHint.setText("Tap to view building details");

        return view;
    }
}