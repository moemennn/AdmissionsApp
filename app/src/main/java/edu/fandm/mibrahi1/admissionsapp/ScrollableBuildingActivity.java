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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.ArrayList;
import java.util.List;

public class ScrollableBuildingActivity extends AppCompatActivity {

    private static final String STORAGE_BUILDINGS_FOLDER = "buildings/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrollable_building);

        String imageFileNames = getIntent().getStringExtra("imageFileNames");
        String buildingDescription = getIntent().getStringExtra("buildingDescription");
        String videoId = getIntent().getStringExtra("videoId");
        double lat = getIntent().getDoubleExtra("lat", 0);
        double lng = getIntent().getDoubleExtra("lng", 0);

        List<String> imagePaths = new ArrayList<>();

        if (imageFileNames != null && !imageFileNames.trim().isEmpty()) {
            String[] files = imageFileNames.split(",");

            for (String file : files) {
                String fileName = file.trim();

                if (!fileName.isEmpty()) {
                    imagePaths.add(STORAGE_BUILDINGS_FOLDER + fileName);
                }
            }
        }

        ViewPager2 viewPager = findViewById(R.id.viewPagerImages);
        viewPager.setAdapter(new ImageGalleryAdapter(imagePaths));

        RecyclerView dotsIndicator = findViewById(R.id.dotsIndicator);
        dotsIndicator.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        DotsAdapter dotsAdapter = new DotsAdapter(imagePaths.size());
        dotsIndicator.setAdapter(dotsAdapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                dotsAdapter.setSelectedDot(position);
            }
        });

        TextView tvBuildingDescription = findViewById(R.id.tvBuildingDescription);
        if (buildingDescription != null) {
            tvBuildingDescription.setText(buildingDescription);
        }

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

        YouTubePlayerView youTubePlayerView = findViewById(R.id.youtubePlayerView);
        if (videoId != null && !videoId.trim().isEmpty()) {
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

    static class ImageGalleryAdapter extends RecyclerView.Adapter<ImageGalleryAdapter.ImageViewHolder> {

        private final List<String> imagePaths;

        ImageGalleryAdapter(List<String> imagePaths) {
            this.imagePaths = imagePaths;
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new ImageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            String path = imagePaths.get(position);

            StorageReference ref = FirebaseStorage.getInstance()
                    .getReference()
                    .child(path);

            ref.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        Glide.with(holder.imageView.getContext())
                                .load(uri)
                                .into(holder.imageView);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ScrollableBuilding", "Failed to load image: " + path, e);
                    });
        }

        @Override
        public int getItemCount() {
            return imagePaths.size();
        }

        static class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            ImageViewHolder(ImageView view) {
                super(view);
                this.imageView = view;
            }
        }
    }

    static class DotsAdapter extends RecyclerView.Adapter<DotsAdapter.DotViewHolder> {
        private final int count;
        private int selectedPosition = 0;

        DotsAdapter(int count) {
            this.count = count;
        }

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
        public int getItemCount() {
            return count;
        }

        static class DotViewHolder extends RecyclerView.ViewHolder {
            View dot;

            DotViewHolder(View view) {
                super(view);
                this.dot = view;
            }
        }
    }
}