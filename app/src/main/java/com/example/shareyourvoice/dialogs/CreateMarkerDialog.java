package com.example.shareyourvoice.dialogs;

import android.app.AlertDialog;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;

public class CreateMarkerDialog extends BottomSheetDialog {
    public CreateMarkerDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    public void dismiss() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("Вы уверены что не хотите сохранить маркер?");
        builder.setPositiveButton("Да", (dialog, which) ->  {
            super.dismiss();
        });
        builder.setNegativeButton("Нет", (dialog, which) -> {

        });
        builder.show();
    }

    public void superDismiss() {
        super.dismiss();
    }
}
