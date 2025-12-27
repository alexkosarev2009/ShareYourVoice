package com.example.shareyourvoice;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.shareyourvoice.databinding.ActivityMapBinding;
import com.example.shareyourvoice.domain.Place;
import com.example.shareyourvoice.services.MapService;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Objects;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private ActivityMapBinding binding;
    private Toast toast;

    private GoogleMap map;

    private MapService mapService;

    ActivityResultLauncher<PickVisualMediaRequest> pickVisualMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
        if (uri != null) {
            mapService.setUriForImageView(uri);
            mapService.setUri(uri);
        } else {
            Toast.makeText(this, "Выберите изображение", Toast.LENGTH_SHORT).show();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mapService = new MapService(this, pickVisualMedia);

        SupportMapFragment fragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.googleMap);

        Objects.requireNonNull(fragment).getMapAsync(this);

        setUpButtons();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void setUpButtons() {
        binding.addMarkerButton.setOnClickListener(v -> {
            binding.addMarkerButton.setVisibility(GONE);
            binding.cancelButton.setVisibility(VISIBLE);
            binding.confirmButton.setVisibility(VISIBLE);
            binding.abstractMarker.setVisibility(VISIBLE);

        });

        binding.cancelButton.setOnClickListener(v -> {
            binding.cancelButton.setVisibility(GONE);
            binding.confirmButton.setVisibility(GONE);
            binding.abstractMarker.setVisibility(GONE);
            binding.addMarkerButton.setVisibility(VISIBLE);
        });

        binding.confirmButton.setOnClickListener(v -> {
            LatLng position = map.getCameraPosition().target;
            float minDistance = 20f;

            Place draftPlace = new Place(
                    "Новая метка",
                    position
            );

            if (canPlaceMarker(position, minDistance)) {
                Marker tempMarker = map.addMarker(
                        new MarkerOptions()
                                .position(position)
                                .title("метка😎")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_gray))
                );
                Objects.requireNonNull(tempMarker).setTag(draftPlace);
                mapService.addMarker(tempMarker);

                binding.confirmButton.setVisibility(GONE);
                binding.cancelButton.setVisibility(GONE);
                binding.abstractMarker.setVisibility(GONE);
                binding.addMarkerButton.setVisibility(VISIBLE);
            }
            else {
                showToast(R.string.too_close);
            }
        });
        binding.btnZoomIn.setOnClickListener(view -> mapService.mapZoomIn(1));
        binding.btnZoomOut.setOnClickListener(view -> mapService.mapZoomOut(1));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;

        mapService.attachMap(map);

        mapService.setup();

    }

    private boolean canPlaceMarker(LatLng newPosition, float minDistanceMeters) {
        for (Marker marker : mapService.getMarkers()) {
            LatLng existing = marker.getPosition();
            float[] results = new float[1];
            android.location.Location.distanceBetween(
                    newPosition.latitude, newPosition.longitude,
                    existing.latitude, existing.longitude,
                    results
            );
            if (results[0] < minDistanceMeters) {
                return false;
            }
        }
        return true;
    }

    private void showToast(int resId) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(this, resId, Toast.LENGTH_SHORT);
        toast.show();
    }
}