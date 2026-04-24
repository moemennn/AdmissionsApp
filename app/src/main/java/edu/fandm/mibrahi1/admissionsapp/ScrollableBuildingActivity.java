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

// This Activity displays detailed information about a building
// including images, description, directions, and a YouTube video
public class ScrollableBuildingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout (UI) for this screen
        setContentView(R.layout.activity_scrollable_building);

        // ================================
        // 1. GET DATA PASSED FROM PREVIOUS SCREEN
        // ================================

        // These values were passed using an Intent
        int[] imageIds = getIntent().getIntArrayExtra("imageIds");
        String buildingDescription = getIntent().getStringExtra("buildingDescription");
        String videoId = getIntent().getStringExtra("videoId");
        double lat = getIntent().getDoubleExtra("lat", 0);
        double lng = getIntent().getDoubleExtra("lng", 0);

        // ================================
        // 2. IMAGE GALLERY (SWIPEABLE IMAGES)
        // ================================

        // Convert array into a List (RecyclerView works better with Lists)
        List<Integer> images = new ArrayList<>();
        if (imageIds != null) {
            for (int id : imageIds) {
                images.add(id);
            }
        }

        // ViewPager2 allows users to swipe between images
        ViewPager2 viewPager = findViewById(R.id.viewPagerImages);
        viewPager.setAdapter(new ImageGalleryAdapter(images));

        // ================================
        // 3. DOT INDICATOR (SHOWS CURRENT IMAGE POSITION)
        // ================================

        RecyclerView dotsIndicator = findViewById(R.id.dotsIndicator);

        // Horizontal layout for dots (left to right)
        dotsIndicator.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Create adapter with number of dots equal to number of images
        DotsAdapter dotsAdapter = new DotsAdapter(images.size());
        dotsIndicator.setAdapter(dotsAdapter);

        // Update dots when user swipes images
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                dotsAdapter.setSelectedDot(position);
            }
        });

        // ================================
        // 4. BUILDING DESCRIPTION TEXT
        // ================================

        TextView tvBuildingDescription = findViewById(R.id.tvBuildingDescription);

        // Only set text if it exists (avoid null errors)
        if (buildingDescription != null) {
            tvBuildingDescription.setText(buildingDescription);
        }

        // ================================
        // 5. "GET DIRECTIONS" BUTTON
        // ================================

        Button btnDirections = findViewById(R.id.btnDirections);

        // Only show button if valid coordinates exist
        if (lat != 0 && lng != 0) {
            btnDirections.setVisibility(View.VISIBLE);

            // When clicked, open Google Maps navigation
            btnDirections.setOnClickListener(v -> {
                Uri uri = Uri.parse("google.navigation:q=" + lat + "," + lng + "&mode=w");

                // Create intent to open Google Maps
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);

                // Force it to use Google Maps app (if installed)
                mapIntent.setPackage("com.google.android.apps.maps");

                startActivity(mapIntent);
            });
        }

        // ================================
        // 6. YOUTUBE VIDEO PLAYER
        // ================================

        YouTubePlayerView youTubePlayerView = findViewById(R.id.youtubePlayerView);

        // Only show video if a video ID was provided
        if (videoId != null && !videoId.isEmpty()) {
            youTubePlayerView.setVisibility(View.VISIBLE);

            // Connect player lifecycle to activity lifecycle
            getLifecycle().addObserver(youTubePlayerView);

            // Load the video when player is ready
            youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
                @Override
                public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                    // cueVideo loads the video but does NOT autoplay
                    youTubePlayer.cueVideo(videoId, 0);
                }
            });
        } else {
            // Hide the player if no video exists
            youTubePlayerView.setVisibility(View.GONE);
        }
    }

    // ================================
    // IMAGE GALLERY ADAPTER
    // Handles displaying images inside ViewPager2
    // ================================
    static class ImageGalleryAdapter extends RecyclerView.Adapter<ImageGalleryAdapter.ImageViewHolder> {

        private final List<Integer> images;

        // Constructor receives list of image resource IDs
        ImageGalleryAdapter(List<Integer> images) {
            this.images = images;
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            // Create a new ImageView for each item
            ImageView imageView = new ImageView(parent.getContext());

            // Make image fill the entire screen space
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            // Crop image nicely to fill space
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            return new ImageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {

            // Set the image for this position
            holder.imageView.setImageResource(images.get(position));
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        // ViewHolder holds the ImageView for reuse
        static class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            ImageViewHolder(ImageView view) {
                super(view);
                this.imageView = view;
            }
        }
    }

    // ================================
    // DOTS ADAPTER
    // Controls the small indicator dots under the images
    // ================================
    static class DotsAdapter extends RecyclerView.Adapter<DotsAdapter.DotViewHolder> {

        private final int count; // number of dots
        private int selectedPosition = 0; // currently active dot

        DotsAdapter(int count) {
            this.count = count;
        }

        // Update which dot is selected
        public void setSelectedDot(int position) {
            int previous = selectedPosition;
            selectedPosition = position;

            // Refresh only the changed dots
            notifyItemChanged(previous);
            notifyItemChanged(selectedPosition);
        }

        @NonNull
        @Override
        public DotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            // Inflate dot layout from XML
            View dot = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.dot_indicator, parent, false);

            return new DotViewHolder(dot);
        }

        @Override
        public void onBindViewHolder(@NonNull DotViewHolder holder, int position) {

            // Highlight selected dot, dim others
            holder.dot.setAlpha(position == selectedPosition ? 1f : 0.4f);
        }

        @Override
        public int getItemCount() {
            return count;
        }

        // Holds reference to each dot view
        static class DotViewHolder extends RecyclerView.ViewHolder {
            View dot;

            DotViewHolder(View view) {
                super(view);
                this.dot = view;
            }
        }
    }
}