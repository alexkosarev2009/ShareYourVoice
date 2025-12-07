package com.example.shareyourvoice.services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.example.shareyourvoice.R;
import com.example.shareyourvoice.domain.Place;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.IOException;
import java.util.Objects;

public class MapService implements OnMapReadyCallback, GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener {

    private final Context context;
    private BottomSheetDialog dialog = null;

    public MapService(Context context) {
        this.context = context;
    }
    @Override
    public void onMapClick(@NonNull LatLng latLng) {

    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {

    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        if (dialog != null && dialog.isShowing()) return true;
        Place place = (Place) marker.getTag();
        if (place == null) return false;

        dialog = new BottomSheetDialog(context);
        dialog.setContentView(R.layout.marker_dialog);
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setDimAmount(0f);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();

        ImageView imageView = dialog.findViewById(R.id.placeImage);
        TextView tvName = dialog.findViewById(R.id.placeName);
        TextView tvAdress = dialog.findViewById(R.id.placeAdress);
        TextView timeLabel = dialog.findViewById(R.id.timeLabel);
        ImageButton btnPlay = dialog.findViewById(R.id.btnPlay);
        SeekBar seekBar = dialog.findViewById(R.id.audioSeekBar);

        Objects.requireNonNull(tvName).setText(place.getName());
        Objects.requireNonNull(tvAdress).setText(place.getAddress());

        if (place.getImageFile() != null) {
            String url = place.getImageFile().getUrl();
            Glide.with(context).load(url).into(Objects.requireNonNull(imageView));
        }

        final MediaPlayer mediaPlayer = new MediaPlayer();
        final Handler handler = new Handler();
        final boolean[] isPlaying = {false};

        if (place.getAudioFile() != null) {
            try {
                mediaPlayer.setDataSource(place.getAudioFile().getUrl());
                mediaPlayer.setOnPreparedListener(mp ->
                        Objects.requireNonNull(seekBar).setMax(mediaPlayer.getDuration()));
                mediaPlayer.prepareAsync();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        Runnable updateSeekBar = new Runnable() {
            @SuppressLint("DefaultLocale")
            @Override
            public void run() {
                if (mediaPlayer.isPlaying()) {
                    int progress = mediaPlayer.getCurrentPosition();
                    Objects.requireNonNull(seekBar).setProgress(progress);
                    handler.postDelayed(this, 500);
                    int sec = progress / 1000;
                    Objects.requireNonNull(timeLabel).setText(sec >= 10 ? String.format("0:%d", sec) : String.format("0:0%d", sec));
                }
            }
        };
        dialog.setOnDismissListener(d -> {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            handler.removeCallbacksAndMessages(null);
        });


        Objects.requireNonNull(btnPlay).setOnClickListener(v -> {
            if (!isPlaying[0]) {
                mediaPlayer.start();
                btnPlay.setImageResource(R.drawable.pause);
                isPlaying[0] = true;
                handler.post(updateSeekBar);

            } else {
                mediaPlayer.pause();
                btnPlay.setImageResource(R.drawable.play);
                isPlaying[0] = false;
                handler.removeCallbacks(updateSeekBar);

            }
        });
        mediaPlayer.setOnCompletionListener(mp -> {
            btnPlay.setImageResource(R.drawable.play);
            isPlaying[0] = false;
        });
        Objects.requireNonNull(seekBar).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        return false;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Place");
        query.findInBackground((objects, e) -> {
            if (e == null) {
                for (ParseObject obj : objects) {
                    Place place = Place.fromParseObject(obj);
                    Marker marker = googleMap.addMarker(new MarkerOptions()
                            .position(place.getLatLng())
                            .title(place.getName())
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_orange)));
                    Objects.requireNonNull(marker).setTag(place);
                }
            } else {
                e.printStackTrace();
            }
        });
        googleMap.setOnMapClickListener(this);
        googleMap.setOnMapLongClickListener(this);
        googleMap.setOnMarkerClickListener(this);

    }
}
