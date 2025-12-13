package com.example.shareyourvoice.services;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.example.shareyourvoice.R;
import com.example.shareyourvoice.domain.Place;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class MapService implements GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener {

    private final Context context;
    private BottomSheetDialog dialog = null;

    private GoogleMap googleMap;

    private MediaRecorder recorder;

    private boolean isRecording = false;

    private final ArrayList<Marker> markers = new ArrayList<>();

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

        Object tag = marker.getTag();
        if (!(tag instanceof Place)) return false;
        Place place = (Place) tag;

        if (place.getObjectId() == null) {
            dialog = new BottomSheetDialog(context);
            dialog.setContentView(R.layout.create_marker_dialog);
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0f);
            dialog.show();

            ImageButton btnRecord = dialog.findViewById(R.id.recordButton);
            ImageButton btnDelete = dialog.findViewById(R.id.deleteMarker);
            ImageButton btnPlace = dialog.findViewById(R.id.placeMarker);
            EditText etName = dialog.findViewById(R.id.markerNameInput);
            TextView tvError = dialog.findViewById(R.id.tvError);

            Objects.requireNonNull(btnRecord).setOnClickListener(view -> {

                if (!isRecording) {
//                    recorder.start();
                    btnRecord.setImageResource(R.drawable.stop_recording);
                }
                if (isRecording) {
//                    recorder.stop();
                    btnRecord.setImageResource(R.drawable.record);

                }
                isRecording = !isRecording;

            });

            Objects.requireNonNull(btnDelete).setOnClickListener(view -> {
                Objects.requireNonNull(marker).remove();
                markers.remove(marker);
                dialog.dismiss();

            });

            Objects.requireNonNull(btnPlace).setOnClickListener(view -> {
                String name = Objects.requireNonNull(etName).getText().toString();

                switch (EtCorrect(name)) {
                    case 0:
                        if (Objects.requireNonNull(tvError).getVisibility() == VISIBLE) tvError.setVisibility(GONE);
                        if (place.getAudioFile() != null) {
                            place.saveToParse(e -> {
                                if (e == null) {
                                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_orange));
                                    Toast.makeText(context, R.string.marker_placed, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, R.string.parse_error + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        else {
                            Toast.makeText(context, R.string.audio_error, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 1:
                        Objects.requireNonNull(tvError).setText(R.string.prohibited_symbols);
                        tvError.setVisibility(VISIBLE);
                        break;
                    case 2:
                        Objects.requireNonNull(tvError).setText(R.string.name_too_long);
                        tvError.setVisibility(VISIBLE);
                        break;
                    case 3:
                        Objects.requireNonNull(tvError).setText(R.string.name_too_short);
                        tvError.setVisibility(VISIBLE);
                }
            });
            return false;
        }

        dialog = new BottomSheetDialog(context);
        dialog.setContentView(R.layout.marker_dialog);
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setDimAmount(0f);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();



        ImageView imageView = dialog.findViewById(R.id.placeImage);
        TextView tvName = dialog.findViewById(R.id.placeName);
        TextView timeLabel = dialog.findViewById(R.id.timeLabel);
        ImageButton btnPlay = dialog.findViewById(R.id.btnPlay);
        SeekBar seekBar = dialog.findViewById(R.id.audioSeekBar);

        Objects.requireNonNull(tvName).setText(place.getName());

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

    public void attachMap(GoogleMap map) {
        this.googleMap = map;
    }

    public void setup() {
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Place");
        query.findInBackground((objects, e) -> {
            if (e == null) {
                for (ParseObject obj : objects) {
                    Place place = Place.fromParseObject(obj);

                    Marker marker = googleMap.addMarker(
                            new MarkerOptions()
                                    .position(place.getLatLng())
                                    .title(place.getName())
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_orange))
                    );
                    Objects.requireNonNull(marker).setTag(place);
                    markers.add(marker);
                }
            }
        });

        googleMap.setOnMapClickListener(this);
        googleMap.setOnMapLongClickListener(this);
        googleMap.setOnMarkerClickListener(this);
    }

    public void addMarker(Marker marker) {
        markers.add(marker);
    }
    public void removeMarker(Marker marker) {
        markers.remove(marker);
    }
    public ArrayList<Marker> getMarkers() {
        return markers;
    }

    public void mapZoomIn(int zoom) {
        if (googleMap != null) {
            float currentZoom = googleMap.getCameraPosition().zoom;
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(currentZoom + zoom));
        }
    }
    public void mapZoomOut(int zoom) {
        if (googleMap != null) {
            float currentZoom = googleMap.getCameraPosition().zoom;
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(currentZoom - zoom));
        }
    }

    private int EtCorrect(String s) {
        for (char c: s.strip().toCharArray()) {
            if (!(Character.isLetter(c) || c == ' ')) return 1;
        }
        if (s.length() > 15) return 2;
        if (s.length() < 5) return 3;
        return 0;
    }
}