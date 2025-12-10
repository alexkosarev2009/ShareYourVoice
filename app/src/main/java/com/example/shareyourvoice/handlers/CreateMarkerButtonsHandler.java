package com.example.shareyourvoice.handlers;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import com.example.shareyourvoice.MapActivity;
import com.example.shareyourvoice.databinding.ActivityMapBinding;

import java.util.Map;

public class CreateMarkerButtonsHandler {

    private ActivityMapBinding binding;

    public CreateMarkerButtonsHandler(ActivityMapBinding binding) {
        this.binding = binding;
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
    }
}
