package com.example.shareyourvoice.services;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.shareyourvoice.dialogs.CreateMarkerDialog;
import com.example.shareyourvoice.R;
import com.example.shareyourvoice.domain.Place;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.io.File;

public class MapService implements GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener {

    private final Context context;
    private BottomSheetDialog dialog = null;

    private GoogleMap googleMap;

    private MediaRecorder recorder;
    private Handler recordHandler;
    private Runnable recordRunnable;
    private int recordSeconds;
    private File audioTemplate;
    private boolean isRecording = false;

    private Uri uri;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickVisualMedia;
    private final ArrayList<Marker> markers = new ArrayList<>();


    public MapService(Context context, ActivityResultLauncher<PickVisualMediaRequest> pickVisualMedia) {
        this.context = context;
        this.pickVisualMedia = pickVisualMedia;
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
            dialog = new CreateMarkerDialog(context);
            dialog.setContentView(R.layout.create_marker_dialog);
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0f);
            dialog.show();

            ImageButton btnRecord = dialog.findViewById(R.id.recordButton);
            ImageButton btnDelete = dialog.findViewById(R.id.deleteMarker);
            ImageButton btnPlace = dialog.findViewById(R.id.placeMarker);
            ImageButton btnAttachPhoto = dialog.findViewById(R.id.attachPhotoButton);
            EditText etName = dialog.findViewById(R.id.markerNameInput);
            TextView tvError = dialog.findViewById(R.id.tvError);
            TextView recordTimer = dialog.findViewById(R.id.recordTimer);
            ImageView userPhoto = dialog.findViewById(R.id.userImage);
            ImageButton btnDeletePhoto = dialog.findViewById(R.id.btnDeletePhoto);
            ImageButton confirmPhoto = dialog.findViewById(R.id.confirmPhoto);

            Objects.requireNonNull(confirmPhoto).setOnClickListener(v -> {
                try {
                    uriToBase64(getUri(), place);
                    Toast.makeText(context, "Изображение Загружено!", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            Objects.requireNonNull(btnAttachPhoto).setOnClickListener(view ->
                    pickVisualMedia.launch(new PickVisualMediaRequest.Builder().
                    setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).
                    build()));

            Objects.requireNonNull(btnDeletePhoto).setOnClickListener(v -> {
                Objects.requireNonNull(userPhoto).setImageURI(null);
                place.setImageBase64(null);
                userPhoto.setImageResource(R.drawable.image);
                btnAttachPhoto.setVisibility(VISIBLE);
                btnDeletePhoto.setVisibility(GONE);
                Objects.requireNonNull(confirmPhoto).setVisibility(GONE);

            });

            Objects.requireNonNull(btnRecord).setOnClickListener(view -> {

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions((Activity) context,
                            new String[]{Manifest.permission.RECORD_AUDIO}, 123);
                    return;
                }

                if (!isRecording) {
                    startRecording(recordTimer);
                    btnRecord.setImageResource(R.drawable.stop_recording);
                } else {
                    stopRecording(place);
                    btnRecord.setImageResource(R.drawable.record);
                }
                isRecording = !isRecording;


            });

            Objects.requireNonNull(btnDelete).setOnClickListener(view ->
                    ((CreateMarkerDialog)dialog).deleteDismiss(marker, markers));

            Objects.requireNonNull(btnPlace).setOnClickListener(view -> {
                String name = Objects.requireNonNull(etName).getText().toString();

                switch (etCorrect(name)) {
                    case 0:
                        place.setName(name);
                        if (Objects.requireNonNull(tvError).getVisibility() == VISIBLE)
                            tvError.setVisibility(GONE);

                        // Проверяем аудио
                        if (place.getAudioBase64() == null || place.getAudioBase64().isEmpty()) {
                            Toast.makeText(context,
                                    R.string.audio_error,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (isRecording) {
                            Toast.makeText(context,
                                    R.string.stopRecordingWarning,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Проверяем изображение
                        if (uri != null && !(place.getImageBase64() == null || place.getImageBase64().isEmpty())) {
                            try {
                                uriToBase64(uri, place);
                            } catch (IOException e) {
                                Toast.makeText(context,
                                        "Ошибка загрузки изображения",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } else {
                            Toast.makeText(context, "Добавьте изображение", Toast.LENGTH_LONG).show();
                            return;
                        }
                        place.saveToParse(e -> {
                            if (e == null) {
                                marker.setIcon(BitmapDescriptorFactory
                                        .fromResource(R.drawable.marker_orange));
                                marker.setTitle(place.getName());
                                Toast.makeText(context,
                                        R.string.marker_placed,
                                        Toast.LENGTH_SHORT).show();
                                ((CreateMarkerDialog)dialog).superDismiss();
                            } else {
                                Log.e("SAVE_ERROR", "Ошибка сохранения:", e);
                                Toast.makeText(context,
                                        "Ошибка сохранения: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
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
                        break;
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

        final MediaPlayer mediaPlayer = new MediaPlayer();
        final Handler handler = new Handler();
        final boolean[] isPlaying = {false};

        // Отображение изображения из Base64
        if (place.getImageBase64() != null && !place.getImageBase64().isEmpty()) {
            try {
                byte[] imageBytes = base64ToBytes(place.getImageBase64());
                Glide.with(context)
                        .load(imageBytes)
                        .into(Objects.requireNonNull(imageView));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Воспроизведение аудио из Base64
        if (place.getAudioBase64() != null && !place.getAudioBase64().isEmpty()) {
            try {
                // Конвертация Base64 обратно в временный файл
                byte[] audioBytes = base64ToBytes(place.getAudioBase64());
                File tempAudio = File.createTempFile("playback_", ".m4a", context.getCacheDir());
                try (FileOutputStream fos = new FileOutputStream(tempAudio)) {
                    fos.write(audioBytes);
                }

                // Установка источника для MediaPlayer
                mediaPlayer.setDataSource(tempAudio.getAbsolutePath());
                mediaPlayer.setOnPreparedListener(mp -> {
                    Objects.requireNonNull(seekBar).setMax(mediaPlayer.getDuration());
                    // Удаление временного файла
                    tempAudio.delete();
                });
                mediaPlayer.prepareAsync();

                // Установка слушателя для удаления файла после выхода
                mediaPlayer.setOnCompletionListener(mp -> {
                    if (tempAudio.exists()) {
                        tempAudio.delete();
                    }
                });

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

            if (isRecording && recorder != null) {
                stopRecording(place);
                isRecording = false;
            }

            //Очистка временных файлов
            File cacheDir = context.getCacheDir();
            if (cacheDir.exists() && cacheDir.isDirectory()) {
                File[] files = cacheDir.listFiles((dir, name) -> name.startsWith("playback_"));
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }
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
                    Place place = new Place(
                            obj.getString("name"),
                            new LatLng(
                                    obj.getDouble("latitude"),
                                    obj.getDouble("longitude")
                            )
                    );

                    place.setAudioBase64(obj.getString("audioBase64"));
                    place.setImageBase64(obj.getString("imageBase64"));
                    place.setObjectId(obj.getObjectId());

                    Marker marker = googleMap.addMarker(
                            new MarkerOptions()
                                    .position(place.getLatLng())
                                    .title(place.getName())
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_orange))
                    );
                    Objects.requireNonNull(marker).setTag(place);
                    markers.add(marker);
                }
            } else {
                Log.e("PARSE", "Ошибка загрузки маркеров", e);
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

    private int etCorrect(String s) {
        for (char c: s.strip().toCharArray()) {
            if (!(Character.isLetter(c) || c == ' ')) return 1;
        }
        if (s.length() > 15) return 2;
        if (s.length() < 5) return 3;
        return 0;
    }
    private void startRecording(TextView recordTimer) {
        try {
            audioTemplate = File.createTempFile(
                    "audio_" + System.currentTimeMillis(),
                    ".m4a",
                    context.getCacheDir()
            );

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(audioTemplate.getAbsolutePath());
            recorder.prepare();
            recorder.start();

            recordSeconds = 0;

            recordHandler = new Handler();
            recordRunnable = new Runnable() {
                @SuppressLint("DefaultLocale")
                @Override
                public void run() {
                    recordSeconds++;
                    recordTimer.setText(String.format("00:%02d", recordSeconds));

                    if (recordSeconds < 5) {
                        recordTimer.setTextColor(Color.RED);
                    } else {
                        recordTimer.setTextColor(Color.BLACK);
                    }

                    if (recordSeconds < 30) {
                        recordHandler.postDelayed(this, 1000);
                    } else {
                        stopRecording(null);
                    }
                }
            };
            recordHandler.postDelayed(recordRunnable, 1000);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void stopRecording(Place place) {
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (Exception ignored) {}

            recorder.release();
            recorder = null;

            if (recordHandler != null && recordRunnable != null) {
                recordHandler.removeCallbacks(recordRunnable);
            }

            if (place == null || audioTemplate == null) return;

            if (recordSeconds < 5) {
                audioTemplate.delete();
                Toast.makeText(context,
                        "Запись слишком короткая",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // Конвертация аудио в Base64
                String audioBase64 = fileToBase64(audioTemplate);
                place.setAudioBase64(audioBase64);
                Toast.makeText(context, "Voice message has been attached", Toast.LENGTH_SHORT).show();
                Log.d("VOICE", "Аудио конвертировано в Base64, длина: " + audioBase64.length());

                // Удаление временного файла
                audioTemplate.delete();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context, "Ошибка конвертации аудио", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void setUriForImageView(Uri uri) {
        ImageView imageView = dialog.findViewById(R.id.userImage);
        ImageButton imageButton = dialog.findViewById(R.id.attachPhotoButton);
        ImageButton deletePhoto = dialog.findViewById(R.id.btnDeletePhoto);
        ImageButton confirmPhoto = dialog.findViewById(R.id.confirmPhoto);
        Objects.requireNonNull(confirmPhoto).setVisibility(VISIBLE);
        Objects.requireNonNull(imageView).setImageURI(uri);
        Objects.requireNonNull(imageButton).setVisibility(GONE);
        Objects.requireNonNull(deletePhoto).setVisibility(VISIBLE);

    }

    public void uriToBase64(Uri uri, Place place) throws IOException {
        // Конвертация uri изображения в Base64-формат
        String imageBase64 = uriToBase64(uri);
        place.setImageBase64(imageBase64);
        Log.d("IMAGE", "Изображение конвертировано в Base64, длина: " + imageBase64.length());
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public Uri getUri() {
        return uri;
    }

    // Конвертация аудио в Base64-формат
    private String fileToBase64(File file) throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        byte[] bytes = new byte[(int) file.length()];
        inputStream.read(bytes);
        inputStream.close();
        return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
    }

    // Конвертация URI изображения в Base64
    private String uriToBase64(Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int len;
        while ((len = Objects.requireNonNull(inputStream).read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        inputStream.close();
        byte[] bytes = byteBuffer.toByteArray();
        return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
    }

    private byte[] base64ToBytes(String base64) {
        return android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
    }
}