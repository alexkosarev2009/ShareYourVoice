package com.example.shareyourvoice.dialogs;

import android.app.AlertDialog;
import android.content.Context;

import androidx.annotation.NonNull;

import com.example.shareyourvoice.R;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class CreateMarkerDialog extends BottomSheetDialog {
    public CreateMarkerDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    public void dismiss() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(R.string.don_t_save_this_marker);
        builder.setPositiveButton(R.string.yes, (dialog, which) -> super.dismiss());
        builder.setNegativeButton(R.string.no, (dialog, which) -> {

        });
        builder.show();
    }

    public void superDismiss() {
        super.dismiss();
    }

    public void deleteDismiss(Marker marker, ArrayList<Marker> markers) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(R.string.delete_this_marker);
        builder.setPositiveButton(R.string.yes, (dialog, which) ->  {
            super.dismiss();
            markers.remove(marker);
            marker.remove();
        });
        builder.setNegativeButton(R.string.no, (dialog, which) -> {

        });
        builder.show();
    }
}
