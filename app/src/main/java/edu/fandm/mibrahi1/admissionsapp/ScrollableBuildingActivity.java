package edu.fandm.mibrahi1.admissionsapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.ArrayList;
import java.util.List;

public class ScrollableBuildingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrollable_building);

        // Retrieve intent data
        int[] imageIds = getIntent().getIntArrayExtra("imageIds");
        String buildingDescription = getIntent().getStringExtra("buildingDescription");
        String videoId = getIntent().getStringExtra("videoId");
        double lat = getIntent().getDoubleExtra("lat", 0);
        double lng = getIntent().getDoubleExtra("lng", 0);

        // Image Gallery
        List<Integer> images = new ArrayList<>();
        if (imageIds != null) {
            for (int id : imageIds) {
                images.add(id);
            }
        }

        ViewPager2 viewPager = findViewById(R.id.viewPagerImages);
        viewPager.setAdapter(new ImageGalleryAdapter(images));

        RecyclerView dotsIndicator = findViewById(R.id.dotsIndicator);
        dotsIndicator.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        DotsAdapter dotsAdapter = new DotsAdapter(images.size());
        dotsIndicator.setAdapter(dotsAdapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                dotsAdapter.setSelectedDot(position);
            }
        });

        // Building Description
        TextView tvBuildingDescription = findViewById(R.id.tvBuildingDescription);
        if (buildingDescription != null) {
            tvBuildingDescription.setText(buildingDescription);
        }

        // Get Directions Button
        Button btnDirections = findViewById(R.id.btnDirections);
        if (lat != 0 && lng != 0) {
            btnDirections.setVisibility(View.VISIBLE);
            btnDirections.setOnClickListener(v -> {
                Uri uri = Uri.parse("google.navigation:q=" + lat + "," + lng + "&mode=w");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            });
        }

        // YouTube Player — only show if video ID exists
        YouTubePlayerView youTubePlayerView = findViewById(R.id.youtubePlayerView);
        if (videoId != null && !videoId.isEmpty()) {
            youTubePlayerView.setVisibility(View.VISIBLE);
            getLifecycle().addObserver(youTubePlayerView);
            youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
                @Override
                public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                    youTubePlayer.cueVideo(videoId, 0);
                }
            });
        } else {
            youTubePlayerView.setVisibility(View.GONE);
        }
    }

    // Image Gallery Adapter
    static class ImageGalleryAdapter extends RecyclerView.Adapter<ImageGalleryAdapter.ImageViewHolder> {
        private final List<Integer> images;

        ImageGalleryAdapter(List<Integer> images) { this.images = images; }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new ImageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            holder.imageView.setImageResource(images.get(position));
        }

        @Override
        public int getItemCount() { return images.size(); }

        static class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            ImageViewHolder(ImageView view) {
                super(view);
                this.imageView = view;
            }
        }
    }

    // Dots Adapter
    static class DotsAdapter extends RecyclerView.Adapter<DotsAdapter.DotViewHolder> {
        private final int count;
        private int selectedPosition = 0;

        DotsAdapter(int count) { this.count = count; }

        public void setSelectedDot(int position) {
            int previous = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(previous);
            notifyItemChanged(selectedPosition);
        }

        @NonNull
        @Override
        public DotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View dot = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.dot_indicator, parent, false);
            return new DotViewHolder(dot);
        }

        @Override
        public void onBindViewHolder(@NonNull DotViewHolder holder, int position) {
            holder.dot.setAlpha(position == selectedPosition ? 1f : 0.4f);
        }

        @Override
        public int getItemCount() { return count; }

        static class DotViewHolder extends RecyclerView.ViewHolder {
            View dot;
            DotViewHolder(View view) {
                super(view);
                this.dot = view;
            }
        }
    }
}