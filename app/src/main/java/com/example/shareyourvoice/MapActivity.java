package com.example.shareyourvoice;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;

import com.example.shareyourvoice.databinding.ActivityMainBinding;
import com.example.shareyourvoice.databinding.ActivityMapBinding;
import com.example.shareyourvoice.dialogs.MarkerCreationFragment;
import com.example.shareyourvoice.handlers.CreateMarkerButtonsHandler;
import com.example.shareyourvoice.services.MapService;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MapActivity extends AppCompatActivity {

    private ActivityMapBinding binding;

    private DialogFragment dialog;

    private GoogleMap map;

    private CreateMarkerButtonsHandler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        handler = new CreateMarkerButtonsHandler(binding);

        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.googleMap);
        Objects.requireNonNull(fragment).getMapAsync(new MapService(this));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Логика добавления маркера

        handler.setUpButtons();

//        binding.addMarkerButton.setOnClickListener(v -> {
//            binding.addMarkerButton.setVisibility(GONE);
//            binding.cancelButton.setVisibility(VISIBLE);
//            binding.confirmButton.setVisibility(VISIBLE);
//            binding.abstractMarker.setVisibility(VISIBLE);
//
//        });
//
//        binding.cancelButton.setOnClickListener(v -> {
//            binding.cancelButton.setVisibility(GONE);
//            binding.confirmButton.setVisibility(GONE);
//            binding.abstractMarker.setVisibility(GONE);
//            binding.addMarkerButton.setVisibility(VISIBLE);
//        });

//        binding.confirmButton.setOnClickListener(v -> {
//            LatLng position = map.getCameraPosition().target;
//
//            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
//            List<Address> result = geocoder.getFromLocation(position.latitude, position.longitude, 1);
//
//            String address = result.get(0).getAddressLine(0);
//
//            MarkerCreationFragment dialog = MarkerCreationFragment.newInstance(position.latitude, position.longitude, address);
//            dialog.show(getSupportFragmentManager(), "marker_dialog");
//
//        });



    }
}